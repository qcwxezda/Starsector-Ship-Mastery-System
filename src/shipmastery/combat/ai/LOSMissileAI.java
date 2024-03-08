package shipmastery.combat.ai;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import shipmastery.util.MathUtils;

public class LOSMissileAI extends BaseGuidedMissileAI {
    private Vector2f targetVelLastFrame = new Vector2f();
    protected final float N = 3f;

    public LOSMissileAI(MissileAPI missile, float maxSeekRangeFactor) {
        super(missile, maxSeekRangeFactor);
    }

    private void pursueTargetLOS(float amount) {

        // Proportional navigation
        Vector3f Rt = new Vector3f(target.getLocation().x, target.getLocation().y, 0f);
        Vector3f Rm = new Vector3f(missile.getLocation().x, missile.getLocation().y, 0f);
        Vector3f Vt = new Vector3f(target.getVelocity().x, target.getVelocity().y, 0f);
        Vector3f Vm = new Vector3f(missile.getVelocity().x, missile.getVelocity().y, 0f);
        Vector3f Rd = new Vector3f(), Vd = new Vector3f();
        Vector3f.sub(Rt, Rm, Rd);
        Vector3f.sub(Vt, Vm, Vd);
        Vector3f res = new Vector3f();
        Vector3f.cross(Rd, Vd, res);
        res.scale(1f / Rd.lengthSquared());
        Vector3f.cross(Vm, res, res);
        res.scale(-N * Vd.length() / Vm.length());
        Vector2f a = new Vector2f(res.x, res.y);

        Vector2f targetAcc = new Vector2f();
        Vector2f.sub(target.getVelocity(), targetVelLastFrame, targetAcc);
        targetAcc.scale(1f/amount);

        float normalizer = (N * targetAcc.length()) / 2f;

        // a = v^2 / r = w^2 r ==> w = a / v
        float acc = (a.length() + normalizer) * ((MathUtils.isClockwise(missile.getVelocity(), a) ? -1f : 1f));
        float w = acc / missile.getVelocity().length();

        // Don't let the PN algorithm intercept at too steep of an angle. This
        // also handles shots that were fired backwards.
        float losAngle = Misc.getAngleInDegrees(new Vector2f(Rd.x, Rd.y));
        float angleDiff = MathUtils.angleDiff(missile.getFacing(), losAngle);
        if (Math.abs(angleDiff) > 60f) {
            w = 1000000f * (angleDiff < 0f ? 1f : -1f);
        }

        if (missile.getAngularVelocity() < w * Misc.DEG_PER_RAD) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        } else {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        }

        targetVelLastFrame = target.getVelocity();
    }

    @Override
    public void advance(float amount) {
        missile.giveCommand(ShipCommand.ACCELERATE);

        if (!preAdvance(amount)) {
            return;
        }

        pursueTargetLOS(amount);
    }
}
