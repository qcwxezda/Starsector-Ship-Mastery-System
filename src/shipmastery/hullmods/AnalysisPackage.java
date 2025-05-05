package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.campaign.RefitHandler;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.util.HashSet;
import java.util.Set;

public class AnalysisPackage extends BaseHullMod implements HullModFleetEffect {

    public static final float[] BASE_INCREASE_VALUES = new float[] {0.05f, 0.1f, 0.15f, 0.2f};
    public static final float MAX_INCREASE_VALUE = 1f;
    public static final String STAT_MOD_ID = "sms_analysis_package";

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return Utils.asPercent(BASE_INCREASE_VALUES[index % 4]);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null) return;
        String typeString = ship.getHullSpec().isCivilianNonCarrier() ? Strings.Hullmods.analysisPackageCivilian : Strings.Hullmods.analysisPackageCombat;
        var increase = getCurrentIncrease();
        String amountString = Utils.asPercent(ship.getHullSpec().isCivilianNonCarrier() ? increase.civIncrease : increase.combatIncrease);
        tooltip.addPara(
                Strings.Hullmods.analysisPackageDesc,
                10f,
                Misc.getHighlightColor(),
                typeString,
                Global.getSettings().getHullModSpec(Strings.Hullmods.ANALYSIS_PACKAGE).getDisplayName(),
                typeString,
                typeString,
                amountString);
    }

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (!fleet.isPlayerFleet()) return;
        var cur = getCurrentIncrease();
        Global.getSector().getPlayerStats().getDynamic()
                .getStat(PlayerMPHandler.CIVILIAN_MP_GAIN_STAT_MULT_KEY)
                .modifyPercent(STAT_MOD_ID, 100f*cur.civIncrease);
        Global.getSector().getPlayerStats().getDynamic()
                .getStat(PlayerMPHandler.COMBAT_MP_GAIN_STAT_MULT_KEY)
                .modifyPercent(STAT_MOD_ID, 100f*cur.combatIncrease);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        var lookup = VariantLookup.getVariantInfo(ship.getVariant());
        return lookup == null || lookup.variant == lookup.root;
    }




    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        // Also possible: check for MODULE hint
        // But do all modules require the MODULE hint?
        var lookup = VariantLookup.getVariantInfo(ship.getVariant());
        if (lookup != null && lookup.variant != lookup.root) {
            return Strings.Hullmods.moduleCantInstall;
        }
        return null;
    }

    private record IncreaseRecord(float civIncrease, float combatIncrease) {}

    private IncreaseRecord getCurrentIncrease() {
        float curCiv = 0f, curCombat = 0f;
        var fleet = Global.getSector().getPlayerFleet();

        var refitShip = (ShipAPI) fleet.getMemoryWithoutUpdate().get(RefitHandler.CURRENT_REFIT_SHIP_KEY);
        FleetMemberAPI refitMember = null;
        if (refitShip != null) {
            refitMember = refitShip.getFleetMember();
        }


        Set<FleetMemberAPI> withSMod = new HashSet<>();
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
            if (fm == refitMember) continue;
            if (fm.getVariant().getSMods().contains(Strings.Hullmods.ANALYSIS_PACKAGE)) {
                withSMod.add(fm);
                continue;
            }
            if (fm.getVariant().hasHullMod(Strings.Hullmods.ANALYSIS_PACKAGE)) {
                boolean civ = fm.getHullSpec().isCivilianNonCarrier();
                var increase = BASE_INCREASE_VALUES[Utils.hullSizeToInt(fm.getVariant().getHullSize())]
                        * (MAX_INCREASE_VALUE - (civ ? curCiv : curCombat));
                if (civ) {
                    curCiv += increase;
                } else {
                    curCombat += increase;
                }
            }
        }

        if (refitShip != null) {
            var refitVariant = refitShip.getVariant();
            float increase;
            boolean civ = refitVariant.getHullSpec().isCivilianNonCarrier();
            if (!refitVariant.getSMods().contains(Strings.Hullmods.ANALYSIS_PACKAGE)
                    &&  refitVariant.hasHullMod(Strings.Hullmods.ANALYSIS_PACKAGE)) {
                increase = BASE_INCREASE_VALUES[Utils.hullSizeToInt(refitVariant.getHullSize())]
                        * (MAX_INCREASE_VALUE - (civ ? curCiv : curCombat));
                if (civ) {
                    curCiv += increase;
                } else {
                    curCombat += increase;
                }
            }
            else if (refitVariant.getSMods().contains(Strings.Hullmods.ANALYSIS_PACKAGE)) {
                increase = BASE_INCREASE_VALUES[Utils.hullSizeToInt(refitVariant.getHullSize())];
                curCombat += increase;
                curCiv += increase;

            }
        }

        for (FleetMemberAPI fm : withSMod) {
            var increase = BASE_INCREASE_VALUES[Utils.hullSizeToInt(fm.getVariant().getHullSize())];
            curCiv += increase;
            curCombat += increase;
        }

        return new IncreaseRecord(curCiv, curCombat);
    }
}
