package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.MasteryDescription;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class Utils {

    public static Map<String, String> hullIdToBaseHullIdMap = new HashMap<>();

    public static class ListMapMap<T, U extends Comparable<U>, V> extends HashMap<T, SortedMap<U, List<V>>> {
        public List<V> get(T key1, U key2) {
            Map<U, List<V>> inner = get(key1);
            return inner == null ? null : inner.get(key2);
        }

        public void add(T key1, U key2, V value) {
            SortedMap<U, List<V>> inner = get(key1);
            if (inner == null) {
                inner = new TreeMap<>();
                put(key1, inner);
            }
            List<V> values = inner.get(key2);
            if (values == null) {
                values = new ArrayList<>();
                inner.put(key2, values);
            }
            values.add(value);
        }
    }

    public static void populateHullIdMap() {
        hullIdToBaseHullIdMap.clear();
        Map<String, String> hullIdToVariant = new HashMap<>();
        for (String variantId : Global.getSettings().getAllVariantIds()) {
            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
            hullIdToVariant.put(variant.getHullSpec().getHullId(), variantId);
        }

        for (Map.Entry<String, String> entry : hullIdToVariant.entrySet()) {
            String baseId = entry.getKey();

            // Already populated by a parent; skip
            if (hullIdToBaseHullIdMap.containsKey(baseId)) {
                continue;
            }

            Queue<ShipVariantAPI> variants = new LinkedList<>();
            variants.add(Global.getSettings().getVariant(entry.getValue()));
            while (!variants.isEmpty()) {
                ShipVariantAPI variant = variants.poll();
                String childId = variant.getHullSpec().getHullId();
                hullIdToBaseHullIdMap.put(childId, baseId);

                for (String moduleId : variant.getModuleSlots()) {
                    variants.add(variant.getModuleVariant(moduleId));
                }
            }
        }
    }

    public static String shortenText(String text, String font, float limit) {
        if (text == null) {
            return null;
        }
        float ellipsesWidth = Global.getSettings().computeStringWidth("...", font);
        float maxWidth = limit * 0.95f - ellipsesWidth;
        if (Global.getSettings().computeStringWidth(text, font) <= maxWidth) {
            return text;
        }
        int left = 0, right = text.length();

        String newText = text;
        while (right > left) {
            int mid = (left + right) / 2;
            newText = text.substring(0, mid);
            if (Global.getSettings().computeStringWidth(newText, font) > maxWidth) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return newText + "...";
    }

    public static int hullSizeToInt(ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case DESTROYER:
                return 1;
            case CRUISER:
                return 2;
            case CAPITAL_SHIP:
                return 3;
            default:
                return 0;
        }
    }

    public static Color darkenColor(Color color) {
        return new Color(color.getRed() / 3, color.getGreen() / 3, color.getBlue() / 3);
    }

    public static Object[] interleaveArrays(Object[] arr1, Object[] arr2) {
        Object[] arr3 = new Object[arr1.length + arr2.length];
        int i = 0, j = 0;

        while (i < arr1.length && j < arr2.length) {
            int k = i + j;
            if (k % 2 == 0) {
                arr3[k] = arr1[i++];
            } else {
                arr3[k] = arr2[j++];
            }
        }

        if (i < arr1.length) {
            System.arraycopy(arr1, i, arr3, i+j, arr1.length - i);
        }
        else if (j < arr2.length) {
            System.arraycopy(arr2, j, arr3, i+j, arr2.length - j);
        }

        return arr3;
    }

    public static String getString(String key1, String key2) {
        return Global.getSettings().getString(key1, key2);
    }

    /** For modules, returns the hull id of the root ship. */
    public static String getBaseHullId(ShipHullSpecAPI spec) {
        if (hullIdToBaseHullIdMap.containsValue(spec.getHullId())) {
            return spec.getHullId();
        }

        ShipHullSpecAPI dParentHull = spec.getDParentHull();
        if (!spec.isDefaultDHull() && !spec.isRestoreToBase()) {
            dParentHull = spec;
        }
        if (dParentHull == null && spec.isRestoreToBase()) {
            dParentHull = spec.getBaseHull();
        }

        String id = dParentHull != null ? dParentHull.getHullId() : spec.getHullId();
        return hullIdToBaseHullIdMap.containsKey(id) ? hullIdToBaseHullIdMap.get(id) : id;
    }

    public static String absValueAsPercent(float num) {
        return (int) (Math.abs(num * 100f)) + "%";
    }

    public static String joinStringList(List<String> strings) {
        switch (strings.size()) {
            case 0: return "";
            case 1: return strings.get(0);
            case 2: return strings.get(0) + " " + Strings.AND_STR + strings.get(1);
            default:
                StringBuilder join = new StringBuilder();
                for (int i = 0; i < strings.size(); i++) {
                    join.append(strings.get(i));
                    if (i < strings.size() - 1) {
                        join.append(", ");
                    }
                    if (i == strings.size() - 2) {
                        join.append(Strings.AND_STR);
                    }
                }
                return join.toString();
        }
    }

    public static MasteryDescription makeGenericNegatableDescription(float value, String posString, String negString, boolean showAsPercent) {
        return MasteryDescription.init(value > 0f ? posString: negString)
                                 .params(showAsPercent ? Utils.absValueAsPercent(value) : Math.abs(value))
                                 .colors(value > 0f ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
    }

    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(min, x));
    }

    public static int clamp(int x, int min, int max) {
        return Math.min(max, Math.max(min, x));
    }
}
