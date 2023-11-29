package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

public class PlasmaBurnCollision extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init( "");
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"microburn".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(PlasmaBurnCollisionScript.class)) {
            ship.addListener(new PlasmaBurnCollisionScript(ship, getStrength(ship), id));
        }
    }

    static class PlasmaBurnCollisionScript extends BaseShipSystemListener implements DamageDealtModifier,
                                                                                     DamageTakenModifier {

        final ShipAPI ship;
        final float mult;
        final String id;
        boolean active;
        boolean test;

        PlasmaBurnCollisionScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
        }

        @Override
        public void onActivate() {
            active = true;
        }

        @Override
        public void onFullyDeactivate() {
            active = false;
        }

        @Override
        public void advanceWhileOn(float amount) {
            test = false;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            System.out.println(param + ", " + target);
            if (param == null) {
                System.out.println("Param is null");
                damage.getModifier().modifyMult(id, 100f);
                return id;
            }
            if (!test) {
                test = true;
                Global.getCombatEngine().applyDamage(target, pt, 100f, DamageType.ENERGY, 100, false, false, ship, true);
            }
            return null;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f pt, boolean shieldHit) {
            if (param == ship && target == ship) {
                damage.getModifier().unmodify(id);
                return id;
            }
            return null;
        }
    }
}
