package shipmastery.util;

import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.sun.istack.internal.NotNull;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public abstract class CollisionUtils {
    public static List<Vector2f> intersectSegmentCircle(Vector2f a, Vector2f b, Vector2f o, float r) {
        List<Vector2f> pts = new ArrayList<>();

        float x1 = a.x - o.x, y1 = a.y - o.y;
        float x2 = b.x - o.x, y2 = b.y - o.y;
        float dx = x2 - x1, dy = y2 - y1, dr = (float) Math.sqrt(dx*dx + dy*dy), D = x1*y2 - x2*y1;

        float disc = r*r*dr*dr - D*D;
        if (disc < 0) {
            return pts;
        }

        float vx = MathUtils.sgnPos(dy)*dx*(float) Math.sqrt(disc);
        float vy = Math.abs(dy)*(float) Math.sqrt(disc);
        float rx1 = (D*dy + vx) / (dr*dr) + o.x, rx2 = (D*dy - vx) / (dr*dr) + o.x;
        float ry1 = (-D*dx + vy) / (dr*dr) + o.y, ry2 = (-D*dx - vy) / (dr*dr) + o.y;

        float mx = Math.min(a.x, b.x), Mx = Math.max(a.x, b.x);
        float my = Math.min(a.y, b.y), My = Math.max(a.y, b.y);
        if (rx1 <= Mx && rx1 >= mx && ry1 <= My && ry1 >= my) {
            pts.add(new Vector2f(rx1, ry1));
        }
        if (rx2 <= Mx && rx2 >= mx && ry2 <= My && ry2 >= my) {
            pts.add(new Vector2f(rx2, ry2));
        }
        return pts;
    }

    /** Get the two extremal points of the shield's active arc */
    public static Pair<Vector2f, Vector2f> getShieldBounds(@NotNull ShieldAPI shield) {
        Vector2f shieldBound1 = Misc.getUnitVectorAtDegreeAngle(shield.getFacing() + shield.getActiveArc() / 2f);
        shieldBound1.scale(shield.getRadius());
        Vector2f.add(shieldBound1, shield.getLocation(), shieldBound1);
        Vector2f shieldBound2 = Misc.getUnitVectorAtDegreeAngle(shield.getFacing() - shield.getActiveArc() / 2f);
        shieldBound2.scale(shield.getRadius());
        Vector2f.add(shieldBound2, shield.getLocation(), shieldBound2);
        return new Pair<>(shieldBound1, shieldBound2);
    }

    /** Returns the point closest to {@code a} that intersects with {@code shield}, or {@code null} if no such
     *  point exists. */
    public static Vector2f rayCollisionCheckShield(Vector2f a, Vector2f b, ShieldAPI shield) {
        if (shield == null) return null;

        // Check if [a] itself is inside the shield
        if (Misc.getDistance(a, shield.getLocation()) <= shield.getRadius() && shield.isWithinArc(a)) {
            return a;
        }

        // Shield consists of 3 parts; two segments and an arc. Check collision with each of them separately and
        // return the closest point.
        Pair<Vector2f, Vector2f> shieldBounds = getShieldBounds(shield);

        List<Vector2f> pts = new ArrayList<>();
        pts.add(Misc.intersectSegments(a, b, shield.getLocation(), shieldBounds.one));
        pts.add(Misc.intersectSegments(a, b, shield.getLocation(), shieldBounds.two));
        pts.addAll(intersectSegmentCircle(a, b, shield.getLocation(), shield.getRadius()));

        Vector2f closestPt = null;
        float closestDist = Float.MAX_VALUE;
        for (Vector2f pt : pts) {
            if (pt == null || !shield.isWithinArc(pt)) continue;
            float dist = Misc.getDistance(a, pt);
            if ((closestPt == null) || Misc.getDistance(a, pt) < closestDist) {
                closestPt = pt;
                closestDist = dist;
            }
        }

        return closestPt;
    }

    public static List<Vector2f> randomPointsOnBounds(ShipAPI ship, int count) {
        BoundsAPI bounds = ship.getExactBounds();
        if (bounds == null) return null;
        bounds.update(ship.getLocation(), ship.getFacing());
        List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();
        WeightedRandomPicker<BoundsAPI.SegmentAPI> picker = new WeightedRandomPicker<>();
        for (BoundsAPI.SegmentAPI segment : segments) {
            picker.add(segment, MathUtils.dist(segment.getP1(), segment.getP2()));
        }
        List<Vector2f> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BoundsAPI.SegmentAPI chosenSegment = picker.pick();
            Vector2f pt = MathUtils.lerp(chosenSegment.getP1(), chosenSegment.getP2(), Misc.random.nextFloat());
            points.add(pt);
        }
        return points;
    }
}
