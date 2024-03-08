package shipmastery.util;

import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;

public abstract class MathUtils {
    public static float sgnPos(float x) {
        return x < 0 ? -1f : 1f;
    }

    public static float dist(Vector2f a, Vector2f b) {
        return (float) Math.sqrt((b.x-a.x)*(b.x-a.x) + (b.y-a.y)*(b.y-a.y));
    }

    public static Vector2f safeNormalize(Vector2f v) {
        if (v.x*v.x + v.y*v.y > 0) {
            v.normalise();
        }
        return v;
    }

    public static Vector2f randomPointOnLine(Vector2f a, Vector2f b) {
        float t = Misc.random.nextFloat();
        return new Vector2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    public static Vector2f inDirectionWithLength(Vector2f dir, float length) {
        return (Vector2f) safeNormalize(new Vector2f(dir)).scale(length);
    }

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    public static Vector2f randomPointInCircle(Vector2f center, float radius) {
        float theta = Misc.random.nextFloat() * 2f * (float) Math.PI;
        float r = radius * (float) Math.sqrt(Misc.random.nextFloat());
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    public static Vector2f randomPointInRing(Vector2f center, float inRadius, float outRadius) {
        float theta = Misc.random.nextFloat() * 2f * (float) Math.PI;
        float r = (float) Math.sqrt(Misc.random.nextFloat() * (outRadius*outRadius - inRadius*inRadius) + inRadius*inRadius);
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    /** Assumes that the quadratic is concave.
     *  Input the value of the quadratic at T = 0 (start), T = maxTime (end), and the quadratic's peak.
     *  Returns the linear and quadratic coefficients. */
    public static Pair<Float, Float> getRateAndAcceleration(float start, float end, float peak, float maxTime) {
        float sqrtTerm = (float) Math.sqrt((peak - end) * (peak - start));
        float a = 2f * (-2f*sqrtTerm + end - 2f*peak + start) / (maxTime*maxTime);
        float r = 2f * (sqrtTerm + peak - start) / maxTime;
        return new Pair<>(r, a);
    }

    /** Misc.getAngleDiff is unsigned; this is signed */
    public static float angleDiff(float a, float b) {
        return ((a - b) % 360 + 540) % 360 - 180;
    }

    public static boolean isClockwise(Vector2f v1, Vector2f v2) {
        return v1.y * v2.x > v1.x * v2.y;
    }

    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(min, x));
    }

    public static float randBetween(float a, float b) {
        return a + (b-a) * Misc.random.nextFloat();
    }
}
