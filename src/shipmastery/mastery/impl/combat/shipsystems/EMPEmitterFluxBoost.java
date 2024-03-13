package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.EMPEmitterDamageListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class EMPEmitterFluxBoost extends ShipSystemEffect {

    public static final int MAX_STACKS = 20;
    public static final float DECAY_RATE = 0.01f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrength(selectedModule));
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EMPEmitterFluxBoost)
                                 .params(systemName, str, str);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EMPEmitterFluxBoostPost, 0f,
                        new Color[]{Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR,
                                Settings.NEGATIVE_HIGHLIGHT_COLOR}, Utils.asInt(MAX_STACKS),
                        Utils.asPercent(MAX_STACKS * getStrength(selectedModule)), Utils.asPercent(DECAY_RATE));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"emp".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(EMPEmitterFluxBoostScript.class)) {
            ship.addListener(new EMPEmitterFluxBoostScript(ship, getStrength(ship), id));
        }
    }

    static class EMPEmitterFluxBoostScript implements EMPEmitterDamageListener, AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        final String id;
        float currentBoost = 0f;

        EMPEmitterFluxBoostScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
        }

        @Override
        public void reportEMPEmitterHit(ShipAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!target.isFighter()) {
                currentBoost += strength;
            }
        }

        @Override
        public void advance(float amount) {
            currentBoost = Math.min(currentBoost, MAX_STACKS * strength);

            if (currentBoost > 0f) {
                ship.getMutableStats().getFluxDissipation().modifyPercent(id, 100f * currentBoost);
                ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(id, currentBoost);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        Strings.Descriptions.EMPEmitterFluxBoostTitle,
                        String.format(Strings.Descriptions.EMPEmitterFluxBoostDesc1, Utils.asPercentNoDecimal(currentBoost)),
                        false);
            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(id);
                ship.getMutableStats().getHardFluxDissipationFraction().unmodify(id);
            }

            currentBoost -= DECAY_RATE * amount;
            currentBoost = Math.max(0f, currentBoost);
        }
    }
}
