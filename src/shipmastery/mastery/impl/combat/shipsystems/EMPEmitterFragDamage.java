package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.EMPEmitterDamageListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EMPEmitterFragDamage extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EMPEmitterFragDamage)
                                 .params(getSystemName(), Utils.asInt(getStrength(selectedVariant)));
    }

    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(EMPEmitterFragDamageScript.class)) {
            ship.addListener(new EMPEmitterFragDamageScript(ship, getStrength(ship)));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "emp";
    }

    static class EMPEmitterFragDamageScript implements EMPEmitterDamageListener {
        final ShipAPI ship;
        final float strength;
        EMPEmitterFragDamageScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
        }

        @Override
        public void reportEMPEmitterHit(ShipAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            Global.getCombatEngine().applyDamage(
                    "EMP_SHIP_SYSTEM_PARAM",
                    target,
                    point,
                    strength,
                    DamageType.FRAGMENTATION,
                    0f,
                    false,
                    false,
                    ship,
                    true);
        }
    }
}
