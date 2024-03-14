package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class PhasedArmorRepair extends ArmorRepair {

    public static final float UPKEEP_COST_INCREASE = 0.25f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.PhasedArmorRepair);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.PhasedArmorRepairPost, 0f, new Color[] {
                                Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                        Utils.asPercent(getStrength(selectedModule)),
                        Utils.asFloatOneDecimal(getFlatMax(selectedModule)),
                        Utils.asPercent(UPKEEP_COST_INCREASE));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getPhaseCloakUpkeepCostBonus().modifyPercent(id, 100f * UPKEEP_COST_INCREASE);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(PhasedArmorRepairScript.class)) {
            ship.addListener(new PhasedArmorRepairScript(ship, getStrength(ship), getFlatMax(ship)));
        }
    }

    static class PhasedArmorRepairScript extends ArmorRepair.ArmorRepairScript {
        PhasedArmorRepairScript(ShipAPI ship, float fracPerSecond, float maxAmountPerSecond) {
            super(ship, fracPerSecond, maxAmountPerSecond);
        }

        @Override
        public void advance(float amount) {
            if (!ship.isPhased()) return;
            super.advance(amount);
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isPhase()) return null;
        return 0.5f + Utils.hullSizeToInt(spec.getHullSize());
    }
}
