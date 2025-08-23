package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class VentingRepairs extends BaseMasteryEffect {

    public static final float BASE_VENT_RATE = 0.08f;
    public static final float BASE_FLUX_NEEDED = 0.7f;
    public static final float COOLDOWN_SECONDS = 20f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.VentingRepairs)
                .params(Utils.asPercent(BASE_VENT_RATE * getStrength(selectedVariant)),
                        Utils.asPercentNoDecimal(BASE_FLUX_NEEDED / getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.VentingRepairsPost, 0f, Misc.getTextColor(), Utils.asInt(COOLDOWN_SECONDS));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getVentRateMult().modifyPercent(id, 100f * BASE_VENT_RATE * getStrength(stats));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new VentingRepairsScript(ship, BASE_FLUX_NEEDED / getStrength(ship)));
    }

    private static class VentingRepairsScript implements AdvanceableListener {
        private final ShipAPI ship;
        private final float fluxNeeded;
        private float cooldownRemaining = 0f;
        private float fluxLevelVenting = 0f;

        private VentingRepairsScript(ShipAPI ship, float fluxNeeded) {
            this.ship = ship;
            this.fluxNeeded = fluxNeeded;
        }

        @Override
        public void advance(float amount) {
            if (ship.getFluxTracker().isVenting()) {
                fluxLevelVenting = Math.max(fluxLevelVenting, ship.getFluxLevel());
            }

            cooldownRemaining -= amount;
            if (cooldownRemaining > 0f) return;

            if (!ship.getFluxTracker().isVenting() && fluxNeeded <= fluxLevelVenting) {
                boolean repairedSomething = false;
                for (WeaponAPI weapon : ship.getUsableWeapons()) {
                    if (weapon.isDisabled() && !weapon.isPermanentlyDisabled()) {
                        weapon.repair();
                        repairedSomething = true;
                    }
                }
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                    if (engine.isDisabled() && !engine.isPermanentlyDisabled()) {
                        engine.repair();
                        repairedSomething = true;
                    }
                }
                if (repairedSomething) {
                    cooldownRemaining = COOLDOWN_SECONDS;
                }
            }
            if (!ship.getFluxTracker().isVenting()) {
                fluxLevelVenting = 0f;
            }
        }
    }
}
