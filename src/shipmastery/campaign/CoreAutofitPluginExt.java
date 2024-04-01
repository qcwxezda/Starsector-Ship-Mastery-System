package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog;
import com.fs.starfarer.api.impl.campaign.plog.SModRecord;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import shipmastery.ShipMastery;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.lang.reflect.Field;
import java.util.*;

public class CoreAutofitPluginExt extends CoreAutofitPlugin {
    public static String COPY_S_MODS = "sms_copy_s_mods";
    protected Map<String, Integer> sModCostMap = new HashMap<>();
    private final RefitHandler refitHandler;

    public CoreAutofitPluginExt(PersonAPI commander, RefitHandler refitHandler) {
        super(commander);
        this.refitHandler = refitHandler;
        options.add(new AutofitOption(
                COPY_S_MODS,
                Strings.RefitScreen.sModAutofitName,
                false,
                Strings.RefitScreen.sModAutofitDesc));
    }

    @Override
    public int getCreditCost() {
        int cost = super.getCreditCost();
        if (isChecked(COPY_S_MODS)) {
            for (int c : sModCostMap.values()) {
                cost += c;
            }
        }
        return cost;
    }

    protected void sortByCost(List<String> hullmods, final ShipAPI ship) {
        Collections.sort(hullmods, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                float cost1 = SModUtils.getCreditsCost(Global.getSettings().getHullModSpec(s1), ship);
                float cost2 = SModUtils.getCreditsCost(Global.getSettings().getHullModSpec(s2), ship);
                return (int) (cost2 - cost1);
            }
        });
    }

    private boolean addSModIfPossible(String hullmod, ShipVariantAPI variant, AutofitPluginDelegate delegate) {
        ShipAPI ship = delegate.getShip();
        ShipHullSpecAPI spec = Utils.getRestoredHullSpec(ship.getHullSpec());
        int sModLimit = Misc.getMaxPermanentMods(ship);
        HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(hullmod);
        float mpCost = SModUtils.getMPCost(modSpec, ship);
        float creditsCost = SModUtils.getCreditsCost(modSpec, ship);
        float playerMP = ShipMastery.getPlayerMasteryPoints(spec);
        MutableValue playerCredits = Utils.getPlayerCredits();
        boolean isBuiltIn = variant.getHullSpec().isBuiltInMod(hullmod);
        if (playerMP >= mpCost &&
                playerCredits.get() >= creditsCost &&
                (variant.getSMods().size() < sModLimit || isBuiltIn) &&
                (delegate.canAddRemoveHullmodInPlayerCampaignRefit(hullmod) || variant.hasHullMod(hullmod))) {
            ShipMastery.spendPlayerMasteryPoints(spec, mpCost);
            playerCredits.subtract(creditsCost);
            if (isBuiltIn) {
                variant.getSModdedBuiltIns().add(hullmod);
            }
            else {
                variant.addPermaMod(hullmod, true);
            }
            return true;
        } else {
            return false;
        }
    }

    private String isNotConfirmFieldName;

    @Override
    public void doFit(ShipVariantAPI current, ShipVariantAPI target, int maxSMods, AutofitPluginDelegate delegate) {
        if (delegate.isPlayerCampaignRefit() && isChecked(COPY_S_MODS)) {

            if (isNotConfirmFieldName == null) {
                for (Field field : delegate.getClass().getDeclaredFields()) {
                    if (field.getType().equals(boolean.class)) {
                        isNotConfirmFieldName = field.getName();
                    }
                }
            }
            if (isNotConfirmFieldName != null) {
                final ShipAPI ship = delegate.getShip();
                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(ship.getHullSpec());

                boolean isConfirm = !((boolean) ReflectionUtils.getField(delegate, isNotConfirmFieldName));

                sModCostMap.clear();
                float savedCredits = Utils.getPlayerCredits().get();
                float savedMP = ShipMastery.getPlayerMasteryPoints(spec);

                // Enhanceable built-ins
                boolean modified = false;
                List<String> enhanceable = new ArrayList<>(target.getSModdedBuiltIns());
                sortByCost(enhanceable, ship);
                for (String sMod : enhanceable) {
                    if (!current.getSModdedBuiltIns().contains(sMod)) {
                        if (addSModIfPossible(sMod, current, delegate)) {
                            sModCostMap.put(
                                    sMod, SModUtils.getCreditsCost(Global.getSettings().getHullModSpec(sMod), ship));
                            modified = true;
                        } else break;
                    }
                }
                // Modular s-mods
                List<String> sMods = new ArrayList<>(target.getSMods());
                sortByCost(sMods, ship);

                for (String sMod : sMods) {
                    if (!current.getSMods().contains(sMod)) {
                        if (addSModIfPossible(sMod, current, delegate)) {
                            sModCostMap.put(
                                    sMod, SModUtils.getCreditsCost(Global.getSettings().getHullModSpec(sMod), ship));
                            modified = true;
                        } else break;
                    }
                }

                if (!isConfirm) {
                    Utils.getPlayerCredits().set(savedCredits);
                    ShipMastery.setPlayerMasteryPoints(spec, savedMP);
                }

                if (modified && isConfirm) {
                    if (ship.getFleetMember() != null) {
                        SModRecord record = new SModRecord(ship.getFleetMember());
                        record.setSPSpent(0);
                        record.setSmods(new ArrayList<>(sModCostMap.keySet()));
                        PlaythroughLog.getInstance().addSModsInstalled(record);
                    }

                    DeferredActionPlugin.performLater(new Action() {
                        @Override
                        public void perform() {
                            refitHandler.injectRefitScreen(true, true);
                        }
                    }, 0f);
                }
            }
        }

        super.doFit(current, target, maxSMods, delegate);
    }
}
