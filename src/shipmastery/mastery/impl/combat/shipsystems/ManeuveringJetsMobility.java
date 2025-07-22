package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ManeuveringJetsMobility extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ManeuveringJetsMobility).params(getSystemName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        tooltip.addPara(Strings.Descriptions.ManeuveringJetsMobilityPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asFloatOneDecimal(strength), Utils.asFloatOneDecimal(2f*strength));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        ship.addListener(new ManeuveringJetsMobilityScript(ship, getStrength(ship), id));
    }

    @Override
    public String getSystemSpecId() {
        return "maneuveringjets";
    }

    public static class ManeuveringJetsMobilityScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float mult;
        final String id;
        static final Color color = new Color(185, 255, 75);

        public ManeuveringJetsMobilityScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getDeceleration().unmodify(id);
            ship.getMutableStats().getAcceleration().unmodify(id);
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel();
            float effectAmount = Math.max(1f, effectLevel * mult);
            ship.getMutableStats().getDeceleration().modifyMult(id, 2f*effectAmount);
            ship.getMutableStats().getAcceleration().modifyMult(id, effectAmount);
            ship.getMutableStats().getTurnAcceleration().modifyMult(id, effectAmount);
            ship.getEngineController().fadeToOtherColor(id, color, null, effectLevel, 1f);

            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    "graphics/icons/hullsys/maneuvering_jets.png",
                    Strings.Descriptions.ManeuveringJetsMobilityTitle,
                    String.format(Strings.Descriptions.ManeuveringJetsMobilityDesc1, Utils.asFloatOneDecimal(effectAmount)),
                    false);
        }
    }
}
