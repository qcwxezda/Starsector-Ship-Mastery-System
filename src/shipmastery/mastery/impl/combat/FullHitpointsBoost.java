package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class FullHitpointsBoost extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FullHitpointsBoost)
                                 .params(Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FullHitpointsBoostPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new FullHitpointsBoostScript(ship, id, getStrength(ship)));
    }

    public static class FullHitpointsBoostScript implements AdvanceableListener {
        final ShipAPI ship;
        final String id;

        public FullHitpointsBoostScript(ShipAPI ship, String id, float strength) {
            this.ship = ship;
            this.id = id;
            float amount = 100f * strength;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getMaxSpeed().modifyPercent(id, amount);
            stats.getFluxDissipation().modifyPercent(id, amount);
            stats.getMissileRoFMult().modifyPercent(id, amount);
            stats.getEnergyRoFMult().modifyPercent(id, amount);
            stats.getBallisticRoFMult().modifyPercent(id, amount);
        }

        @Override
        public void advance(float amount) {
            if (ship.getHitpoints() < ship.getMaxHitpoints()) {
                MutableShipStatsAPI stats = ship.getMutableStats();
                stats.getMaxSpeed().unmodify(id);
                stats.getFluxDissipation().unmodify(id);
                stats.getMissileRoFMult().unmodify(id);
                stats.getEnergyRoFMult().unmodify(id);
                stats.getBallisticRoFMult().unmodify(id);
                ship.removeListener(this);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 1.4f;
    }
}
