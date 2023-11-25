package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.Iterator;

public class RecallDeviceDestruction extends BaseMasteryEffect {

    static final float EFFECT_RADIUS = 500f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init("placeholder...");
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null || ship.getSystem() == null || !"recalldevice".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(RecallDeviceDestructionScript.class)) {
            ship.addListener(new RecallDeviceDestructionScript());
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(RecallDeviceDestructionScript.class);
    }

    public static class RecallDeviceDestructionScript extends BaseShipSystemListener {
        @Override
        public void onActivate(ShipAPI ship) {
            Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 2f*EFFECT_RADIUS + 2f*ship.getCollisionRadius(), 2f*EFFECT_RADIUS + 2f*ship.getCollisionRadius());
            while (itr.hasNext()) {
                Object o = itr.next();
                if (!(o instanceof CombatEntityAPI)) continue;
                CombatEntityAPI entity = (CombatEntityAPI) o;
                if (MathUtils.dist(ship.getLocation(), entity.getLocation()) > EFFECT_RADIUS + ship.getCollisionRadius()) continue;
                if (!(o instanceof ShipAPI)) continue;
                ShipAPI target = (ShipAPI) o;
                if (target.isFighter() && target.getOwner() != ship.getOwner()) {
                    Global.getCombatEngine().removeEntity(target);

                    // This will spawn an entire wing, delete non-leaders...
                    ShipAPI newShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).spawnShipOrWing(
                             Utils.getFighterWingId(target.getVariant().getHullVariantId()), ship.getLocation(), Misc.random.nextFloat() * 360f);

                    for (ShipAPI fighter : newShip.getWing().getWingMembers()) {
                        if (!fighter.isWingLeader()) {
                            Global.getCombatEngine().removeEntity(fighter);
                        }
                        else {
                        }
                    }

                    float particlesPerSecond = 100f, maxDuration = 0.5f;
                    JitterEmitter jitter = new JitterEmitter(entity, target.getSpriteAPI(), Color.WHITE, 0f, entity.getCollisionRadius(), 0.5f, true, 0.8f,
                                                             (int) (particlesPerSecond*maxDuration));
                    jitter.setBaseIntensity(0.6f);
                    Particles.stream(jitter, 1, particlesPerSecond, maxDuration);
                }
            }
        }
    }
}
