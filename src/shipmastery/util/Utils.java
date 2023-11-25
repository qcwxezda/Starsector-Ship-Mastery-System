package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Pair;

import java.text.DecimalFormat;
import java.util.*;

public abstract class Utils {
    public static DecimalFormat percentFormat = new DecimalFormat("#,##0.#%");
    public static final DecimalFormat oneDecimalPlaceFormat = new DecimalFormat("0.#");

    public static final Map<String, String> wingVariantToIdMap = new HashMap<>();

    static {
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            wingVariantToIdMap.put(spec.getVariantId(), spec.getId());
        }
    }

    public static String getFighterWingId(String variantId) {
        return wingVariantToIdMap.get(variantId);
    }

    public static ShipHullSpecAPI getRestoredHullSpec(ShipHullSpecAPI spec) {
        ShipHullSpecAPI dParentHull = spec.getDParentHull();
        if (!spec.isDefaultDHull() && !spec.isRestoreToBase()) {
            dParentHull = spec;
        }
        if (dParentHull == null && spec.isRestoreToBase()) {
            dParentHull = spec.getBaseHull();
        }

        return dParentHull == null ? spec : dParentHull;
    }

    public static String getRestoredHullSpecId(ShipHullSpecAPI spec) {
        return getRestoredHullSpec(spec).getHullId();
    }

    public static class ListPairMapMap<T, U extends Comparable<U>, V> extends HashMap<T, SortedMap<U, Pair<List<V>, List<V>>>> {

        Pair<List<V>, List<V>> getPair(T key1, U key2, boolean addIfNotPresent) {
            SortedMap<U, Pair<List<V>, List<V>>> inner = get(key1);
            if (inner == null) {
                if (addIfNotPresent) {
                    inner = new TreeMap<>();
                    put(key1, inner);
                } else {
                    return null;
                }

            }
            Pair<List<V>, List<V>> pair = inner.get(key2);
            if (pair == null && addIfNotPresent) {
                pair = new Pair<List<V>, List<V>>(new ArrayList<V>(), new ArrayList<V>());
                inner.put(key2, pair);
            }
            return pair;
        }

        public List<V> get1(T key1, U key2) {
            Pair<List<V>, List<V>> pair = getPair(key1, key2, false);
            return pair == null ? null : pair.one;
        }

        public List<V> get2(T key1, U key2) {
            Pair<List<V>, List<V>> pair = getPair(key1, key2, false);
            return pair == null ? null : pair.two;
        }

        public void add1(T key1, U key2, V value) {
            Pair<List<V>, List<V>> pair = getPair(key1, key2, true);
            pair.one.add(value);
        }

        public void add2(T key1, U key2, V value) {
            Pair<List<V>, List<V>> pair = getPair(key1, key2, true);
            pair.two.add(value);
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

    public static String absValueAsPercent(float num) {
        return asPercent(Math.abs(num));
    }

    public static String asPercent(float num) {return percentFormat.format(num);}

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

    public static boolean hasShield(ShipHullSpecAPI spec) {
        return spec.getShieldType() != ShieldAPI.ShieldType.NONE && spec.getShieldType() != ShieldAPI.ShieldType.PHASE;
    }

    public static PersonAPI getCommanderForFleetMember(FleetMemberAPI fm) {
        if (fm == null) return null;
        // First, check if it has a fleet commander for stats. This won't work in the refit screen, so
        if (fm.getFleetCommanderForStats() != null) return fm.getFleetCommanderForStats();
        // if no fleet commander for stats and on player side, assume it's a player ship in the refit screen
        if (fm.getOwner() == 0) return Global.getSector().getPlayerPerson();
        return null;
    }
}
