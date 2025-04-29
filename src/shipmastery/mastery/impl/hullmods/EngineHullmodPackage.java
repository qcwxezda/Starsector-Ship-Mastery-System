package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EngineHullmodPackage extends HullmodPackage {

    public static final float[] TOP_SPEED_BONUS = {10f, 8f, 6f, 5f};

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.EngineHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.UNSTABLE_INJECTOR),
                Utils.getHullmodName(HullMods.AUXILIARY_THRUSTERS),
                Utils.getHullmodName(HullMods.INSULATEDENGINE),
                Utils.getHullmodName(HullMods.UNSTABLE_INJECTOR)
        };
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.EngineHullmodPackagePost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asFloatOneDecimal(getStrength(selectedModule)*TOP_SPEED_BONUS[Utils.hullSizeToInt(selectedModule.getHullSize())]));
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.UNSTABLE_INJECTOR, false),
                new HullmodData(HullMods.AUXILIARY_THRUSTERS, false),
                new HullmodData(HullMods.INSULATEDENGINE, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 3;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getBallisticWeaponRangeBonus().unmodify(HullMods.UNSTABLE_INJECTOR);
        stats.getEnergyWeaponRangeBonus().unmodify(HullMods.UNSTABLE_INJECTOR);
        stats.getFighterRefitTimeMult().unmodify(HullMods.UNSTABLE_INJECTOR);
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getMaxSpeed().modifyFlat(id, getStrength(stats)*TOP_SPEED_BONUS[Utils.hullSizeToInt(hullSize)]);
    }
}
