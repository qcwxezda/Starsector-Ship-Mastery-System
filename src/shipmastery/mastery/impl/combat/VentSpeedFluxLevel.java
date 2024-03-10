package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class VentSpeedFluxLevel extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.VentSpeedFluxLevel)
                                 .params(Utils.asPercent(2f/3f * getStrength((PersonAPI) null)), Utils.asPercent(getStrength(selectedModule)))
                                 .colors(Settings.NEGATIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(VentSpeedFluxLevelScript.class)) {
            ship.addListener(new VentSpeedFluxLevelScript(ship, -2f/3f*getStrength((PersonAPI) null), getStrength(ship), id));
        }
    }

    static class VentSpeedFluxLevelScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxPenalty, maxBoost;
        final String id;
        float modifier = 0f;

        VentSpeedFluxLevelScript(ShipAPI ship, float maxPenalty, float maxBoost, String id) {
            this.ship = ship;
            this.maxPenalty = maxPenalty;
            this.maxBoost = maxBoost;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (!ship.getFluxTracker().isVenting()) {
                modifier = MathUtils.lerp(maxPenalty, maxBoost, ship.getFluxLevel());
                ship.getMutableStats().getVentRateMult().modifyPercent(id, 100f * modifier);
            }

            if (modifier != 0f) {
                String desc = modifier > 0f ? Strings.Descriptions.VentSpeedFluxLevelDesc2 : Strings.Descriptions.VentSpeedFluxLevelDesc1;
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/tactical/venting_flux2.png",
                        Strings.Descriptions.VentSpeedFluxLevelTitle,
                        String.format(desc, Utils.asPercentNoDecimal(Math.abs(modifier))),
                        modifier < 0f);
            }
        }
    }
}
