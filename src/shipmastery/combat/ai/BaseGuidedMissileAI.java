package shipmastery.combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.util.EngineUtils;
import shipmastery.util.TargetChecker;

public abstract class BaseGuidedMissileAI implements MissileAIPlugin, GuidedMissileAI {

    protected CombatEntityAPI target;
    protected final MissileAPI missile;
    protected final float maxSeekRange;
    protected final ShipAPI.HullSize smallestAutoTarget;
    protected final IntervalUtil findTargetInterval = new IntervalUtil(0.2f, 0.3f);
    protected final TargetChecker validTargetChecker = new TargetChecker() {
        @Override
        public boolean check(CombatEntityAPI entity) {
            if (entity == null) return false;
            if (!Global.getCombatEngine().isEntityInPlay(entity)) return false;
            if (entity.getOwner() == missile.getOwner() || entity.getOwner() == 100) {
                return false;
            }

            // This AI doesn't seek non-ship targets, so a non-ship target would have to have been
            // forced via setTarget; assume it's valid.
            if (!(entity instanceof ShipAPI ship)) return true;

            return ship.isAlive() && !ship.isShuttlePod();
        }
    };

    /** maxSeekRangeFactor is a fraction of the missile's maximum range. */
    public BaseGuidedMissileAI(MissileAPI missile, float maxSeekRangeFactor) {
        this.missile = missile;
        maxSeekRange = maxSeekRangeFactor * missile.getMaxRange();
        WeaponAPI weapon = missile.getWeapon();
        if (weapon.hasAIHint(WeaponAPI.AIHints.STRIKE)) {
            smallestAutoTarget =
                    weapon.hasAIHint(WeaponAPI.AIHints.USE_VS_FRIGATES)
                            ? ShipAPI.HullSize.FRIGATE
                            : ShipAPI.HullSize.DESTROYER;
        }
        else {
            smallestAutoTarget = ShipAPI.HullSize.FIGHTER;
        }
        findTargetInterval.forceIntervalElapsed();
    }

    protected void smoothTurn(float targetAngle, boolean clockwise) {
        float facingAngle = missile.getFacing();
        float turnSpeed = missile.getAngularVelocity();
        float maxAngularAcc = missile.getTurnAcceleration();

        float facingError = ((targetAngle - facingAngle) * (clockwise ? -1f : 1f) + 720f) % 360f;

        if (turnSpeed < 0f && !clockwise) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }
        else if (turnSpeed > 0f && clockwise) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        }
        else {
            // If stopping turning now would reach or overshoot the target, stop turning
            // by accelerating in the opposite direction
            float stoppingDist = turnSpeed*turnSpeed/(2f*maxAngularAcc);
            if (stoppingDist >= facingError) {
                missile.giveCommand(clockwise ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);
            }
            else {
                missile.giveCommand(clockwise ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
            }
        }
    }

    private ShipAPI findNewTarget() {
        // Does the ship have a valid target?
        if (missile.getSource() != null) {
            ShipAPI sourceTarget = missile.getSource().getShipTarget();
            if (validTargetChecker.check(sourceTarget) && Misc.getDistance(missile.getLocation(), sourceTarget.getLocation()) <= maxSeekRange + sourceTarget.getCollisionRadius()) {
                return sourceTarget;
            }
        }

        // Just find the closest target
        return EngineUtils.getClosestEntity(
                missile.getLocation(),
                smallestAutoTarget,
                maxSeekRange,
                true,
                validTargetChecker
        );
    }

    public Vector2f getInterceptionLoS() {
        Vector2f interceptor = getInterceptionPoint(1f);
        Vector2f.sub(interceptor, missile.getLocation(), interceptor);
        return interceptor;
    }

    public Vector2f getInterceptionPoint(float accuracy) {
        return Global.getCombatEngine().getAimPointWithLeadForAutofire(missile, accuracy, target, missile.getMaxSpeed());
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    /** Returns whether the missile should continue seeking a target. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean preAdvance(float amount) {
        if (missile.isFizzling() || missile.isExpired()) {
            return false;
        }

        findTargetInterval.advance(amount);
        if (!validTargetChecker.check(target) && findTargetInterval.intervalElapsed()) {
            setTarget(findNewTarget());
        }

        if (!validTargetChecker.check(target)) {
            return false;
        }

        if (target instanceof ShipAPI ship) {
            return !ship.isPhased() && !ship.getCollisionClass().equals(CollisionClass.NONE);
        }

        return true;
    }
}
