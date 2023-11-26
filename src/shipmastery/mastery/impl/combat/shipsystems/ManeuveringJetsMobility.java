package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ManeuveringJetsMobility extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ManeuveringJetsMobility).params(Strings.Descriptions.ManeuveringJetsName);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        tooltip.addPara(Strings.Descriptions.ManeuveringJetsMobilityPost, 0f, Misc.getHighlightColor(), Utils.oneDecimalPlaceFormat.format(strength), Utils.oneDecimalPlaceFormat.format(2f*strength));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"maneuveringjets".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(ManeuveringJetsMobilityScript.class)) {
            ship.addListener(new ManeuveringJetsMobilityScript(ship, getStrength(ship), id));
        }
    }

    public static class ManeuveringJetsMobilityScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float mult;
        final String id;
        final Map<ShipEngineControllerAPI.ShipEngineAPI, Color> originalColors = new HashMap<>();

        public ManeuveringJetsMobilityScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
        }

        @Override
        public void onActivate() {
            if (originalColors.size() < ship.getEngineController().getShipEngines().size()) {
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                    originalColors.put(engine, engine.getEngineColor());
                }
            }
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getDeceleration().unmodify(id);
            ship.getMutableStats().getAcceleration().unmodify(id);
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                engine.getEngineSlot().setColor(originalColors.get(engine));
            }
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel();
            float effectAmount = Math.max(1f, effectLevel * mult);
            ship.getMutableStats().getDeceleration().modifyMult(id, 2f*effectAmount);
            ship.getMutableStats().getAcceleration().modifyMult(id, effectAmount);
            ship.getMutableStats().getTurnAcceleration().modifyMult(id, effectAmount);

            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                engine.getEngineSlot().setColor(Utils.mixColor(originalColors.get(engine), new Color(185, 255, 75), effectLevel));
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(
                    id,
                    "graphics/icons/hullsys/maneuvering_jets.png",
                    Strings.Descriptions.ManeuveringJetsMobilityTitle,
                    String.format(Strings.Descriptions.ManeuveringJetsMobilityDesc1, Utils.oneDecimalPlaceFormat.format(effectAmount)),
                    false);
        }
    }
}
