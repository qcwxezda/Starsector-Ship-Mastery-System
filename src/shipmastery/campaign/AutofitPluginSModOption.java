package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog;
import com.fs.starfarer.api.impl.campaign.plog.SModRecord;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import shipmastery.ShipMastery;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.ShipMasterySModRecord;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutofitPluginSModOption extends CoreAutofitPlugin {
    public static final String COPY_S_MODS = "sms_copy_s_mods";
    protected final Map<String, Integer> sModCreditsCostMap = new HashMap<>();
    protected final Map<String, Integer> sModMPCostMap = new HashMap<>();
    protected ShipHullSpecAPI rootSpec;
    private final RefitHandler refitHandler;
    private final boolean useSP;

    public AutofitPluginSModOption(RefitHandler refitHandler, boolean useSP) {
        super(Global.getSector().getPlayerPerson());
        this.refitHandler = refitHandler;
        this.useSP = useSP;
        options.add(new AutofitOption(
                COPY_S_MODS,
                Strings.RefitScreen.sModAutofitName,
                false,
                Strings.RefitScreen.sModAutofitDesc));
    }

    @Override
    public int getCreditCost() {
        int cost = super.getCreditCost();
        if (isChecked(COPY_S_MODS) && !useSP) {
            for (int c : sModCreditsCostMap.values()) {
                cost += c;
            }
        }
        return cost;
    }

    protected void sortByCost(List<String> hullmods, final ShipAPI ship) {
        hullmods.sort((s1, s2) -> {
            HullModSpecAPI hm1 = Global.getSettings().getHullModSpec(s1);
            HullModSpecAPI hm2 = Global.getSettings().getHullModSpec(s2);
            float cost1 = SModUtils.getCreditsCost(hm1, ship);
            float cost2 = SModUtils.getCreditsCost(hm2, ship);
            return (int) (cost2 - cost1);
        });
    }

    private boolean addSModIfPossible(String hullmod, ShipVariantAPI variant, AutofitPluginDelegate delegate, int sModLimit) {
        return useSP ? addSModIfPossibleUseSP(hullmod, variant, delegate, sModLimit)
                : addSModIfPossibleUseMP(hullmod, variant, delegate, sModLimit);
    }

    private boolean addSModIfPossibleUseSP(String hullmod, ShipVariantAPI variant, AutofitPluginDelegate delegate, int sModLimit) {
        MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
        int playerSP = playerStats.getStoryPoints();
        boolean isBuiltIn = variant.getHullSpec().isBuiltInMod(hullmod);
        if (playerSP >= 1 &&
                (variant.getSMods().size() < sModLimit || isBuiltIn) &&
                (delegate.canAddRemoveHullmodInPlayerCampaignRefit(hullmod) || variant.hasHullMod(hullmod))) {
            playerStats.setStoryPoints(playerSP - 1);
            if (isBuiltIn) {
                variant.getSModdedBuiltIns().add(hullmod);
            }
            else {
                variant.addPermaMod(hullmod, true);
            }
            return true;
        }
        return false;
    }

    private boolean addSModIfPossibleUseMP(String hullmod, ShipVariantAPI variant, AutofitPluginDelegate delegate, int sModLimit) {
        ShipAPI ship = delegate.getShip();
        HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(hullmod);
        float mpCost = SModUtils.getMPCost(modSpec, ship);
        float creditsCost = SModUtils.getCreditsCost(modSpec, ship);
        float playerMP = ShipMastery.getPlayerMasteryPoints(rootSpec);
        MutableValue playerCredits = Utils.getPlayerCredits();
        boolean isBuiltIn = variant.getHullSpec().isBuiltInMod(hullmod);
        if (((playerMP >= mpCost &&
                playerCredits.get() >= creditsCost) || Global.getSettings().isDevMode()) &&
                (variant.getSMods().size() < sModLimit || isBuiltIn) &&
                (delegate.canAddRemoveHullmodInPlayerCampaignRefit(hullmod) || variant.hasHullMod(hullmod))) {
            ShipMastery.spendPlayerMasteryPoints(rootSpec, mpCost);
            playerCredits.subtract(creditsCost);
            if (isBuiltIn) {
                variant.getSModdedBuiltIns().add(hullmod);
            }
            else {
                variant.addPermaMod(hullmod, true);
            }
            return true;
        }
        return false;
    }

    private String isNotConfirmFieldName;

    protected boolean tryAddSMods(ShipVariantAPI current, ShipVariantAPI target, AutofitPluginDelegate delegate) {
        boolean modified = false;
        ShipAPI ship = delegate.getShip();
        int limit = Misc.getMaxPermanentMods(ship);
        if (current.hasHullMod("swp_extrememods")) {
            limit--;
        }

        // Only allow adding S-mods with engineering override if already permanent on target
        if (current.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE) && !target.getPermaMods().contains(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
            return false;
        }

        // Enhanceable built-ins
        List<String> enhanceable = new ArrayList<>(target.getSModdedBuiltIns());
        sortByCost(enhanceable, ship);
        for (String sMod : enhanceable) {
            if (!current.getSModdedBuiltIns().contains(sMod)) {
                if (addSModIfPossible(sMod, current, delegate, limit)) {
                    HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(sMod);
                    sModCreditsCostMap.put(
                            sMod, SModUtils.getCreditsCost(hullModSpec, ship));
                    sModMPCostMap.put(sMod, SModUtils.getMPCost(hullModSpec, ship));
                    modified = true;
                } else break;
            }
        }

        // Modular s-mods
        List<String> sMods = new ArrayList<>(target.getSMods());
        sortByCost(sMods, ship);
        boolean hasLogisticsSMod = SModUtils.hasLogisticSMod(current);
        for (String sMod : sMods) {
            HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(sMod);
            if (!current.getSMods().contains(sMod)) {
                boolean isFirstLogistic = SModUtils.hasBonusLogisticSlot(current) && !hasLogisticsSMod && hullModSpec.hasUITag(HullMods.TAG_UI_LOGISTICS);
                if (addSModIfPossible(sMod, current, delegate, limit + (isFirstLogistic ? 1 : 0))) {
                    sModCreditsCostMap.put(
                            sMod, SModUtils.getCreditsCost(hullModSpec, ship));
                    sModMPCostMap.put(sMod, SModUtils.getMPCost(hullModSpec, ship));
                    modified = true;
                    if (isFirstLogistic) limit++;
                    if (hullModSpec.hasUITag(HullMods.TAG_UI_LOGISTICS)) {
                        hasLogisticsSMod = true;
                    }
                }
            }
        }
        return modified;
    }

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
                VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(ship.getVariant());
                if (info != null) {
                    rootSpec = Utils.getRestoredHullSpec(info.root.getHullSpec());
                }
                else {
                    rootSpec = Utils.getRestoredHullSpec(ship.getHullSpec());
                }

                boolean isConfirm = !((boolean) ReflectionUtils.getField(delegate, isNotConfirmFieldName));

                sModCreditsCostMap.clear();
                sModMPCostMap.clear();
                float savedCredits = Utils.getPlayerCredits().get();
                float savedMP = ShipMastery.getPlayerMasteryPoints(rootSpec);
                int savedSP = Global.getSector().getPlayerStats().getStoryPoints();

                boolean modified = tryAddSMods(current, target, delegate);

                if (!isConfirm) {
                    Utils.getPlayerCredits().set(savedCredits);
                    ShipMastery.setPlayerMasteryPoints(rootSpec, savedMP);
                }
                Global.getSector().getPlayerStats().setStoryPoints(savedSP);

                if (modified && isConfirm) {
                    if (ship.getFleetMember() != null) {
                        float averageBonusXPFraction = 0f;
                        if (useSP) {
                            StringBuilder sb = new StringBuilder();
                            int i = 0;
                            for (String mod : sModCreditsCostMap.keySet()) {
                                HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(mod);
                                averageBonusXPFraction += ship.getHullSpec().isBuiltInMod(mod)
                                        ? 1f
                                        : Misc.getBuildInBonusXP(modSpec, ship.getHullSize());
                                sb.append(modSpec.getDisplayName());
                                if (i < sModCreditsCostMap.size() - 1) {
                                    sb.append(", ");
                                }
                                i++;
                            }
                            averageBonusXPFraction /= sModCreditsCostMap.size();

                            Global.getSector().getPlayerStats().spendStoryPoints(
                                    sModCreditsCostMap.size(),
                                    true,
                                    null,
                                    true,
                                    averageBonusXPFraction,
                                    String.format(
                                            Strings.RefitScreen.sModAutofitSPText,
                                            ship.getName(),
                                            ship.getHullSpec().getNameWithDesignationWithDashClass(),
                                            sb));
                            Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);
                        }

                        for (String mod : sModCreditsCostMap.keySet()) {
                            SModRecord record;
                            if (useSP) {
                                record = new SModRecord(ship.getFleetMember());
                                record.setSPSpent(1);
                                record.setBonusXPFractionGained(
                                        ship.getHullSpec().isBuiltInMod(mod)
                                                ? 1f
                                                : Misc.getBuildInBonusXP(Global.getSettings().getHullModSpec(mod), ship.getHullSize()));
                            } else {
                                record = new ShipMasterySModRecord(ship.getFleetMember());
                                record.setSPSpent(0);
                                ((ShipMasterySModRecord) record).setMPSpent(sModMPCostMap.get(mod));
                                ((ShipMasterySModRecord) record).setCreditsSpent(sModCreditsCostMap.get(mod));
                            }
                            record.setSmods(new ArrayList<>(Collections.singleton(mod))); // Make sure it's mutable
                            PlaythroughLog.getInstance().addSModsInstalled(record);
                        }
                    }

                    // Engineering override becomes permanent
                    if (!useSP && !sModCreditsCostMap.isEmpty()
                            && current.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE)
                            && !current.getPermaMods().contains(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
                        current.addPermaMod(Strings.Hullmods.ENGINEERING_OVERRIDE, false);
                    }

                    DeferredActionPlugin.performLater(() -> {
                        if (refitHandler != null) {
                            refitHandler.injectRefitScreen(true, !sModCreditsCostMap.isEmpty());
                        }
                        else {
                            RefitHandler.syncRefitScreenWithVariant(!sModCreditsCostMap.isEmpty());
                        }
                    }, 0f);
                }
            }
        }

        super.doFit(current, target, maxSMods, delegate);
    }
}
