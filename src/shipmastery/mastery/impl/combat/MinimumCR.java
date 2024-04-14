package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MinimumCR extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MinimumCR)
                                 .params(Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.MinimumCRPost, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getCriticalMalfunctionChance().modifyMult(id, 0f);
        stats.getWeaponMalfunctionChance().modifyMult(id, 0f);
        stats.getEngineMalfunctionChance().modifyMult(id, 0f);
        stats.getShieldMalfunctionChance().modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(MinimumCRScript.class)) {
            ship.addListener(new MinimumCRScript(ship, getStrength(ship), id));
        }
    }

    static class MinimumCRScript implements AdvanceableListener {
        final ShipAPI ship;
        final float minimum;
        final String id;

        MinimumCRScript(final ShipAPI ship, float minimum, String id) {
            this.ship = ship;
            this.minimum = minimum;
            this.id  = id;
        }

        @Override
        public void advance(float amount) {
            ship.setCurrentCR(Math.min(ship.getCRAtDeployment(), Math.max(ship.getCurrentCR(), minimum)));
            if (ship.getCurrentCR() <= 0f) {
                ship.getMutableStats().getCriticalMalfunctionChance().unmodify(id);
                ship.getMutableStats().getWeaponMalfunctionChance().unmodify(id);
                ship.getMutableStats().getEngineMalfunctionChance().unmodify(id);
                ship.getMutableStats().getShieldMalfunctionChance().unmodify(id);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 3.5f - Utils.hullSizeToInt(spec.getHullSize());
    }
}
