package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillsChangeRemoveExcessOPEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.campaign.fleet.FleetData;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.entities.Missile;
import com.fs.starfarer.combat.entities.PlasmaShot;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.*;

public abstract class Utils {
    public static final DecimalFormat percentFormat = new DecimalFormat("#,##0.##%");
    public static final DecimalFormat percentFormatNoDecimal = new DecimalFormat("#,##0%");
    public static final DecimalFormat oneDecimalPlaceFormat = new DecimalFormat("0.#");
    public static final DecimalFormat integerFormat = new DecimalFormat("0");
    public static final DecimalFormat twoDecimalPlaceFormat = new DecimalFormat("0.##");
    public static Comparator<FleetMemberAPI> byDPComparator = new Comparator<FleetMemberAPI>() {
        @Override
        public int compare(FleetMemberAPI m1, FleetMemberAPI m2) {
            float dp1 = m1.getHullSpec().getSuppliesToRecover();
            float dp2 = m2.getHullSpec().getSuppliesToRecover();
            int dpDiff = (int) (dp2 - dp1);
            if (dpDiff != 0) return dpDiff;
            int specDiff = m1.getHullId().compareTo(m2.getHullId());
            if (specDiff != 0) return specDiff;
            return m1.getId().compareTo(m2.getId());
        }
    };

    public static final Map<String, String> wingVariantToIdMap = new HashMap<>();
    public static final Map<String, String> hullmodIdToNameMap = new HashMap<>();


    static {
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            wingVariantToIdMap.put(spec.getVariantId(), spec.getId());
        }
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            hullmodIdToNameMap.put(spec.getId(), spec.getDisplayName());
        }
    }

    public static String getHullmodName(String hullmodId) {
        return hullmodIdToNameMap.get(hullmodId);
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

    public static String asPercentNoDecimal(float num) {return percentFormatNoDecimal.format(num);}

    public static String asFloatOneDecimal(float num) {return oneDecimalPlaceFormat.format(num);}
    public static String asFloatTwoDecimals(float num) {return twoDecimalPlaceFormat.format(num);}

    public static String asInt(float num) {
        // Should always round down, except in cases where the number is so close to the next int that it's clear
        // that it's just floating point rounding issues (e.g. 1000 * 1.6 becoming 1599.9999).
        return integerFormat.format((int) (num + 0.0001f));
    }


    public static class WeaponSlotCount {

        public final int sb;
        public final int mb;
        public final int lb;
        public final int se;
        public final int me;
        public final int le;
        public final int sm;
        public final int mm;
        public final int lm;

        public WeaponSlotCount(int sb, int mb, int lb, int se, int me, int le, int sm, int mm, int lm) {
            this.sb = sb;
            this.mb = mb;
            this.lb = lb;
            this.se = se;
            this.me = me;
            this.le = le;
            this.sm = sm;
            this.mm = mm;
            this.lm = lm;
        }
    }

    /** Sum will be greater than total number of weapon slots since i.e. synergy counts as 1 energy and 1 missile */
    public static WeaponSlotCount countWeaponSlots(ShipHullSpecAPI spec) {
        int sb = 0, mb = 0, lb = 0, se = 0, me = 0,  le = 0, sm = 0, mm = 0, lm = 0;
        for (WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            switch (slot.getSlotSize()) {
                case SMALL:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            sb++;
                            break;
                        case ENERGY:
                            se++;
                            break;
                        case MISSILE:
                            sm++;
                            break;
                        case UNIVERSAL:
                            sb++;se++;sm++;
                            break;
                        case HYBRID:
                            sb++;se++;
                            break;
                        case SYNERGY:
                            se++;sm++;
                            break;
                        case COMPOSITE:
                            sb++;sm++;
                            break;
                    }
                    break;
                case MEDIUM:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            mb++;
                            break;
                        case ENERGY:
                            me++;
                            break;
                        case MISSILE:
                            mm++;
                            break;
                        case UNIVERSAL:
                            mb++;me++;mm++;
                            break;
                        case HYBRID:
                            mb++;me++;
                            break;
                        case SYNERGY:
                            me++;mm++;
                            break;
                        case COMPOSITE:
                            mb++;mm++;
                            break;
                    }
                    break;
                case LARGE:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            lb++;
                            break;
                        case ENERGY:
                            le++;
                            break;
                        case MISSILE:
                            lm++;
                            break;
                        case UNIVERSAL:
                            lb++;le++;lm++;
                            break;
                        case HYBRID:
                            lb++;le++;
                            break;
                        case SYNERGY:
                            le++;lm++;
                            break;
                        case COMPOSITE:
                            lb++;lm++;
                            break;
                    }
                    break;
            }
        }
        return new WeaponSlotCount(sb, mb, lb, se, me, le, sm, mm, lm);
    }

    public static WeaponSlotCount countWeaponSlotsStrict(ShipHullSpecAPI spec) {
        int sb = 0, mb = 0, lb = 0, se = 0, me = 0,  le = 0, sm = 0, mm = 0, lm = 0;
        for (WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            switch (slot.getSlotSize()) {
                case SMALL:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            sb++;
                            break;
                        case ENERGY:
                            se++;
                            break;
                        case MISSILE:
                            sm++;
                            break;
                    }
                    break;
                case MEDIUM:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            mb++;
                            break;
                        case ENERGY:
                            me++;
                            break;
                        case MISSILE:
                            mm++;
                            break;
                    }
                    break;
                case LARGE:
                    switch (slot.getWeaponType()) {
                        case BALLISTIC:
                            lb++;
                            break;
                        case ENERGY:
                            le++;
                            break;
                        case MISSILE:
                            lm++;
                            break;
                    }
                    break;
            }
        }
        return new WeaponSlotCount(sb, mb, lb, se, me, le, sm, mm, lm);
    }

    public static String joinStringList(List<String> strings) {
        switch (strings.size()) {
            case 0: return "";
            case 1: return strings.get(0);
            case 2: return strings.get(0) + " " + Strings.Misc.and + strings.get(1);
            default:
                StringBuilder join = new StringBuilder();
                for (int i = 0; i < strings.size(); i++) {
                    join.append(strings.get(i));
                    if (i < strings.size() - 1) {
                        join.append(", ");
                    }
                    if (i == strings.size() - 2) {
                        join.append(Strings.Misc.and);
                    }
                }
                return join.toString();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasShield(ShipHullSpecAPI spec) {
        return spec.getShieldType() != ShieldAPI.ShieldType.NONE && spec.getShieldType() != ShieldAPI.ShieldType.PHASE;
    }

    public static boolean fixVariantInconsistencies(MutableShipStatsAPI stats) {
        ShipVariantAPI variant = stats.getVariant();
        List<String> wingIds = variant.getWings();
        if (wingIds != null && !wingIds.isEmpty()) {
            for (int i = stats.getNumFighterBays().getModifiedInt(); i < wingIds.size(); i++) {
                variant.setWingId(i, null);
            }
        }
        if (clampOP(variant, Global.getSector().getPlayerStats())) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    Strings.Misc.excessOPWarning,
                    Misc.getNegativeHighlightColor());
            return true;
        }
        return false;
    }

    public static void fixPlayerFleetInconsistencies() {
        for (FleetMemberAPI fm : Utils.getMembersNoSync(Global.getSector().getPlayerFleet())) {
            ShipVariantAPI variant = fm.getVariant();
            // This just sets hasOpAffectingMods to null, forcing the variant to
            // recompute its statsForOpCosts (e.g. number of hangar bays)
            // (Normally this is naturally set when a hullmod is manually added or removed)
            variant.addPermaMod("sms_masteryHandler");
            Utils.fixVariantInconsistencies(fm.getStats());
        }
    }

    public static MutableValue getPlayerCredits() {
        return ((FleetData) Global.getSector().getPlayerFleet().getFleetData()).getCargoNoSync().getCredits();
    }

    public static List<FleetMember> getMembersNoSync(CampaignFleetAPI fleet) {
        if (fleet == null) return new ArrayList<>();
        return getMembersNoSync(fleet.getFleetData());
    }

    public static List<FleetMember> getMembersNoSync(FleetDataAPI fleetData) {
        if (fleetData == null) return new ArrayList<>();
        // Don't use getMembersNoSync because that also includes NULL spacer members (when dragging stuff around)
        boolean wasNoSync = fleetData.isForceNoSync();
        fleetData.setForceNoSync(true);
        List<FleetMember> members = ((FleetData) fleetData).getMembers();
        fleetData.setForceNoSync(wasNoSync);
        return members == null ? new ArrayList<FleetMember>() : members;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean wasProjectileRemoved(DamagingProjectileAPI proj) {
        if (proj instanceof Missile) {
            return ((Missile) proj).wasRemoved();
        }
        else if (proj instanceof PlasmaShot){
            return proj.getBrightness() <= 0f;
        }
        else {
            return proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
        }
    }

    public static void toHSVA(float[] rgba, float[] dest) {
        float r = rgba[0], g = rgba[1], b = rgba[2];
        float CMax = Math.max(r, Math.max(g, b));
        float CMin = Math.min(r, Math.min(g, b));
        float delta = CMax - CMin;

        if (delta == 0f) {
            dest[0] = 0f;
        } else if (CMax == r) {
            dest[0] = 60f*(((g-b)/delta) % 6f);
        } else if (CMax == g) {
            dest[0] = 60f*((b-r)/delta + 2);
        } else {
            dest[0] = 60f*((r-g)/delta + 4);
        }

        dest[1] = CMax == 0f ? 0f : delta / CMax;
        dest[2] = CMax;
        dest[3] = rgba[3];
    }

    public static Color mixColor(Color a, Color b, float t) {
        float[] colorA = new float[4];
        float[] colorB = new float[4];
        float[] newColor = new float[4];
        a.getComponents(colorA);
        b.getComponents(colorB);
        for (int i = 0; i < 4; i++) {
            newColor[i] = (1f - t) * colorA[i] + t * colorB[i];
        }
        return new Color(newColor[0], newColor[1], newColor[2], newColor[3]);
    }

    /** Adapted from SkillsChangeRemoveExcessOPEffect */
    public static boolean clampOP(ShipVariantAPI variant, MutableCharacterStatsAPI stats) {
        int maxOP = SkillsChangeRemoveExcessOPEffect.getMaxOP(variant.getHullSpec(), stats);
        int op = variant.computeOPCost(stats);
        int remove = op - maxOP;
        if (remove > 0) {
            int caps = variant.getNumFluxCapacitors();
            int curr = Math.min(caps, remove);
            variant.setNumFluxCapacitors(caps - curr);
            remove -= curr;
            if (remove > 0) {
                int vents = variant.getNumFluxVents();
                curr = Math.min(vents, remove);
                variant.setNumFluxVents(vents - curr);
                remove -= curr;
            }
            if (remove > 0) {
                for (String modId : variant.getNonBuiltInHullmods()) {
                    HullModSpecAPI mod = Global.getSettings().getHullModSpec(modId);
                    curr = mod.getCostFor(variant.getHullSpec().getHullSize());
                    variant.removeMod(modId);
                    remove -= curr;
                    if (remove <= 0) break;
                }
            }
            return true;
        }
        return false;
    }

    public static void maintainStatusForPlayerShip(ShipAPI ship, Object id, String spriteName, String title, String desc, boolean isDebuff) {
        if (ship != Global.getCombatEngine().getPlayerShip()) return;
        Global.getCombatEngine().maintainStatusForPlayerShip(id, spriteName, title, desc, isDebuff);
    }

    public static float[][] clone2DArray(float[][] arr) {
        float[][] res = new float[arr.length][];
        for (int i = 0; i < arr.length; i++) {
            res[i] = arr[i].clone();
        }
        return res;
    }

    public static float getSelectionWeightScaledByValue(float value, float valueForOneWeight, boolean lowerValuesHigherWeight) {
        float f = (float) (Math.log(1f + value/valueForOneWeight)/Math.log(2f));
        if (!lowerValuesHigherWeight) return f;
        else return getSelectionWeightScaledByValue(1f/value, 1f/valueForOneWeight, false);
    }

    public static Set<WeaponAPI.WeaponType> getDominantWeaponTypes(ShipHullSpecAPI spec) {
        WeaponSlotCount count = countWeaponSlots(spec);
        Set<WeaponAPI.WeaponType> dominantTypes = new HashSet<>();
        int maxLarge = Math.max(count.le, Math.max(count.lm, count.lb));
        if (maxLarge >= 1) {
            if (count.le == maxLarge) dominantTypes.add(WeaponAPI.WeaponType.ENERGY);
            if (count.lb == maxLarge) dominantTypes.add(WeaponAPI.WeaponType.BALLISTIC);
            if (count.lm == maxLarge) dominantTypes.add(WeaponAPI.WeaponType.MISSILE);
            return dominantTypes;
        }
        int maxMedium = Math.max(count.me, Math.max(count.mm, count.mb));
        if (maxMedium >= 1) {
            if (count.me == maxMedium) dominantTypes.add(WeaponAPI.WeaponType.ENERGY);
            if (count.mb == maxMedium) dominantTypes.add(WeaponAPI.WeaponType.BALLISTIC);
            if (count.mm == maxMedium) dominantTypes.add(WeaponAPI.WeaponType.MISSILE);
            return dominantTypes;
        }
        int maxSmall = Math.max(count.se, Math.max(count.sm, count.sb));
        if (count.se == maxSmall) dominantTypes.add(WeaponAPI.WeaponType.ENERGY);
        if (count.sb == maxSmall) dominantTypes.add(WeaponAPI.WeaponType.BALLISTIC);
        if (count.sm == maxSmall) dominantTypes.add(WeaponAPI.WeaponType.MISSILE);
        return dominantTypes;
    }
}
