package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BackArmorBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BackArmorBoost).params(
                Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BackArmorBoostPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        float strength = getStrength(ship);
        float[][] grid = ship.getArmorGrid().getGrid();
        for (int i = 0; i < grid[0].length; i++) {
            float effectLevel = Math.max(0f, 2f * ((float) grid[0].length / 2f - i) / grid[0].length);
            if (effectLevel  <= 0f) continue;
            for (int j = 0; j < grid.length; j++) {
                grid[j][i] += grid[j][i] * effectLevel * strength;
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getArmorRating() < 400f) return null;
        return Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
