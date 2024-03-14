package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ArmorRepair extends BaseMasteryEffect {

    public static final float MIN_ARMOR_FRAC = 0.75f;

    public float getFlatMax(ShipAPI ship) {
        return getStrength(ship) * 150f * (Utils.hullSizeToInt(ship.getHullSize()) + 1);
    }


    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.ArmorRepair);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ArmorRepairPost, 0f, new Color[] {
                Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                        Utils.asPercent(getStrength(selectedModule)),
                        Utils.asFloatOneDecimal(getFlatMax(selectedModule)),
                        Utils.asPercent(1f - MIN_ARMOR_FRAC));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getMinArmorFraction().modifyMult(id, MIN_ARMOR_FRAC);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ArmorRepairScript.class)) {
            ship.addListener(new ArmorRepairScript(ship, getStrength(ship), getFlatMax(ship)));
        }
    }

    static class ArmorRepairScript implements AdvanceableListener {

        final ShipAPI ship;
        final float fracPerSecond;
        final float maxAmountPerSecond;
        final float[][] maxArmor;
        final IntervalUtil repairInterval = new IntervalUtil(0.5f, 1f);

        ArmorRepairScript(ShipAPI ship, float fracPerSecond, float maxAmountPerSecond) {
            this.ship = ship;
            this.fracPerSecond = fracPerSecond;
            // Each grid cell only has 1/15 the armor rating
            this.maxAmountPerSecond = maxAmountPerSecond / 15f;
            maxArmor = Utils.clone2DArray(ship.getArmorGrid().getGrid());
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive()) return;
            repairInterval.advance(amount);

            if (repairInterval.intervalElapsed()) {
                float dur = repairInterval.getIntervalDuration();
                float repairFrac = fracPerSecond * dur;

                float[][] grid = ship.getArmorGrid().getGrid();
                for (int i = 0; i < grid.length; i++) {
                    for (int j = 0; j < grid[0].length; j++) {
                        float amountMissing = maxArmor[i][j] - grid[i][j];
                        if (amountMissing > 0) {
                            float repairAmount = Math.min(repairFrac * amountMissing, maxAmountPerSecond * dur);
                            grid[i][j] += repairAmount;
                        }
                    }
                }
                ship.syncWithArmorGridState();
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getArmorRating(), 800f, false);
    }
}
