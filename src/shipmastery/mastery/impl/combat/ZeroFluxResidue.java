package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ZeroFluxResidue extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ZeroFluxResidue).params(Utils.asFloatOneDecimal(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ZeroFluxResidueScript.class)) {
            ship.addListener(new ZeroFluxResidueScript(ship, getStrength(ship), id));
        }
    }

    static class ZeroFluxResidueScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxDuration;
        final String id;
        boolean engineBoostActiveLastFrame;
        float duration;

        ZeroFluxResidueScript(ShipAPI ship, float maxDuration, String id) {
            this.ship = ship;
            this.maxDuration = maxDuration;
            this.id = id;
            duration = maxDuration;
        }

        @Override
        public void advance(float amount) {
            boolean isEngineBoostActive = ship.getFluxTracker().isEngineBoostActive();
            if (engineBoostActiveLastFrame && !isEngineBoostActive) {
                duration = 0f;
            }
            if (isEngineBoostActive) {
                duration = maxDuration;
            }

            if (duration < maxDuration) {
                // 0 flux speed boost also gives +10 flat max turn rate
                float zeroFluxSpeed = ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue();
                float boost = zeroFluxSpeed * (1f - duration / maxDuration);
                ship.getMutableStats().getMaxSpeed().modifyFlat(id, boost);
                ship.getMutableStats().getMaxTurnRate().modifyFlat(id, 10f * (1f - duration / maxDuration));
                Utils.maintainStatusForPlayerShip(ship,
                        id,
                        "graphics/icons/tactical/engine_boost2.png",
                        Strings.Descriptions.ZeroFluxResidueTitle,
                        String.format(Strings.Descriptions.ZeroFluxResidueDesc1, (int) boost),
                        false);
            }
            else {
                ship.getMutableStats().getMaxSpeed().unmodify(id);
                ship.getMutableStats().getMaxTurnRate().unmodify(id);
            }

            engineBoostActiveLastFrame = isEngineBoostActive;
            duration += amount;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return spec.isCivilianNonCarrier() ? null : 1f;
    }
}
