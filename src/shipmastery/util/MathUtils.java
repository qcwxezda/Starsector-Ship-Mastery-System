package shipmastery.util;

import com.fs.starfarer.api.util.Misc;
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

    public static Vector2f inDirectionWithLength(Vector2f dir, float length) {
        return (Vector2f) safeNormalize(new Vector2f(dir)).scale(length);
    }

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    public static float randBetween(float a, float b) {
        return a + (b-a) * Misc.random.nextFloat();
    }
}
