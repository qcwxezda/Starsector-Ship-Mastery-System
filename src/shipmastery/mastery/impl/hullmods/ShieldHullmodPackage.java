package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ShieldHullmodPackage extends HullmodPackage {

    public static final float REQ_NOT_MET_MULT = 1f/3f;
    public static final float SHIELD_UPKEEP_MULT = 0.5f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .init(getDescriptionString())
                .params((Object[]) getDescriptionParams(selectedModule))
                .colors(Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Misc.getTextColor());
    }

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.ShieldHullmodPackage;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.ShieldHullmodPackagePost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(getStrength(selectedModule)*REQ_NOT_MET_MULT));
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.ACCELERATED_SHIELDS),
                Utils.getHullmodName(HullMods.EXTENDED_SHIELDS),
                Utils.getHullmodName(HullMods.HARDENED_SHIELDS),
                Utils.getHullmodName(HullMods.STABILIZEDSHIELDEMITTER),
                Utils.asPercent(getStrength(selectedModule)),
                Utils.asPercent(1f - SHIELD_UPKEEP_MULT)
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.ACCELERATED_SHIELDS, false),
                new HullmodData(HullMods.EXTENDED_SHIELDS, false),
                new HullmodData(HullMods.HARDENED_SHIELDS, false),
                new HullmodData(HullMods.STABILIZEDSHIELDEMITTER, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 3;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getShieldUpkeepMult().modifyMult(id, SHIELD_UPKEEP_MULT);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - getStrength(stats));
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - getStrength(stats)*REQ_NOT_MET_MULT);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!Utils.hasShield(spec)) return null;
        // Prefer stronger shields
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueDecreasing(spec.getBaseShieldFluxPerDamageAbsorbed(), 0.4f, 0.7f, 1.5f);
    }
}
