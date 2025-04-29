package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillsChangeRemoveExcessOPEffect;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
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
import org.json.JSONArray;
import org.json.JSONObject;
import shipmastery.ShipMastery;
import shipmastery.data.MasteryInfo;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.impl.stats.ModifyStatsEffect;
import shipmastery.stats.ShipStat;

import java.awt.Color;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Utils {
    public static final DecimalFormat percentFormat = new DecimalFormat("#,##0.##%");
    public static final DecimalFormat percentFormatOneDecimal = new DecimalFormat("#,##0.#%");
    public static final DecimalFormat percentFormatNoDecimal = new DecimalFormat("#,##0%");
    public static final DecimalFormat oneDecimalPlaceFormat = new DecimalFormat("0.#");
    public static final DecimalFormat integerFormat = new DecimalFormat("0");
    public static final DecimalFormat twoDecimalPlaceFormat = new DecimalFormat("0.##");
    public static final Comparator<FleetMemberAPI> byDPComparator = (m1, m2) -> {
        float dp1 = m1.getHullSpec().getSuppliesToRecover();
        float dp2 = m2.getHullSpec().getSuppliesToRecover();
        int dpDiff = (int) (dp2 - dp1);
        if (dpDiff != 0) return dpDiff;
        int specDiff = m1.getHullId().compareTo(m2.getHullId());
        if (specDiff != 0) return specDiff;
        return m1.getId().compareTo(m2.getId());
    };

    public static final Map<String, String> wingVariantToIdMap = new HashMap<>();
    public static final Map<String, String> hullmodIdToNameMap = new HashMap<>();
    public static final Map<String, ShipHullSpecAPI> hullIdToRestored = new HashMap<>();
    public static final Map<String, Set<String>> baseHullToAllSkinsMap = new HashMap<>();
    public static final Map<String, DifficultyData> difficultyDataMap = new HashMap<>();
    public static DifficultyData defaultDifficultyData;
    public static final String defaultFactionId = "<default>";

    public static final Set<String> allHullSpecIds = new HashSet<>();

    public static void init() {
        // Initialize helper structures
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            wingVariantToIdMap.put(spec.getVariantId(), spec.getId());
        }
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            hullmodIdToNameMap.put(spec.getId(), spec.getDisplayName());
        }
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec != getRestoredHullSpec(spec)) continue;
            allHullSpecIds.add(spec.getHullId());
        }
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
            if (spec != restoredSpec) continue;

            String id = spec.getHullId();
            String baseId = spec.getBaseHullId();

            Set<String> skins = baseHullToAllSkinsMap.computeIfAbsent(baseId, k -> new HashSet<>());
            skins.add(id);
        }
        try {
            JSONArray factionsArray = Global.getSettings().getMergedSpreadsheetData("faction_id", "data/shipmastery/faction_difficulty.csv");
            // Populate default first
            for (int i = 0; i < factionsArray.length(); i++) {
                JSONObject object = factionsArray.getJSONObject(i);
                String id = object.getString("faction_id");
                if (defaultFactionId.equals(id)) {
                    float avgMod = (float) object.getDouble("average_modifier");
                    float stDev = (float) object.getDouble("stdev");
                    float flagshipBonus = (float) object.getDouble("flagship_bonus");
                    float sModProb = (float) object.getDouble("base_smod_prob");
                    float perDMod = (float) object.getDouble("smod_prob_mult_per_dmod");
                    DifficultyData data = new DifficultyData(avgMod, stDev, flagshipBonus, sModProb, perDMod);
                    difficultyDataMap.put(id, data);
                    defaultDifficultyData = data;
                    break;
                }
            }
            if (defaultDifficultyData != null) {
                for (int i = 0; i < factionsArray.length(); i++) {
                    JSONObject object = factionsArray.getJSONObject(i);
                    String id = object.getString("faction_id");
                    float avgMod = (float) object.optDouble("average_modifier", defaultDifficultyData.averageModifier());
                    float stDev = (float) object.optDouble("stdev", defaultDifficultyData.stDev);
                    float flagshipBonus = (float) object.optDouble("flagship_bonus", defaultDifficultyData.flagshipBonus());
                    float sModProb = (float) object.optDouble("base_smod_prob", defaultDifficultyData.baseSModProb());
                    float perDMod = (float) object.optDouble("smod_prob_mult_per_dmod", defaultDifficultyData.sModProbMultPerDMod());
                    DifficultyData data = new DifficultyData(avgMod, stDev, flagshipBonus, sModProb, perDMod);
                    difficultyDataMap.put(id, data);
                    if (defaultFactionId.equals(id)) {
                        defaultDifficultyData = data;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load or read data/shipmastery/faction_difficulty.csv: ", e);
        }
    }

    public record DifficultyData(float averageModifier, float stDev, float flagshipBonus, float baseSModProb, float sModProbMultPerDMod) {}

    public static String getHullmodName(String hullmodId) {
        return hullmodIdToNameMap.get(hullmodId);
    }

    public static ShipHullSpecAPI getRestoredHullSpecOneStep(ShipHullSpecAPI spec) {
        ShipHullSpecAPI dParentHull = spec.getDParentHull();
        if (!spec.isDefaultDHull() && !spec.isRestoreToBase()) {
            dParentHull = spec;
        }
        if (dParentHull == null && spec.isRestoreToBase()) {
            dParentHull = spec.getBaseHull();
        }

        return dParentHull == null ? spec : dParentHull;
    }

    public static ShipHullSpecAPI getRestoredHullSpec(ShipHullSpecAPI spec) {
        ShipHullSpecAPI memo = hullIdToRestored.get(spec.getHullId());
        if (memo != null) return memo;

        ShipHullSpecAPI prevSpec = null;
        while (spec != prevSpec) {
            prevSpec = spec;
            spec = getRestoredHullSpecOneStep(spec);
        }

        hullIdToRestored.put(spec.getHullId(), spec);
        return spec;
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
        return switch (hullSize) {
            case DESTROYER -> 1;
            case CRUISER -> 2;
            case CAPITAL_SHIP -> 3;
            default -> 0;
        };
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

    public static String asPercent(float num) {
        if (num < 0.01f) {
            return percentFormat.format(num);
        }
        else if (num < 0.1f) {
            return asPercentOneDecimal(num);
        } else {
            return asPercentNoDecimal(num);
        }
    }
    public static String asPercentOneDecimal(float num) {return percentFormatOneDecimal.format(num);}

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
        public final int stotal, mtotal, ltotal;

        /** sWeight + mWeight + lWeight should add up to 1. Then computeWeaponWeight returns values in [0, 1]. */
        public float computeWeaponWeight(WeaponAPI.WeaponType type, float sWeight, float mWeight) {
            int sCount, mCount, lCount;
            switch (type) {
                case BALLISTIC -> {sCount = sb; mCount = mb; lCount = lb;}
                case ENERGY -> {sCount = se; mCount = me; lCount = le;}
                case MISSILE -> {sCount = sm; mCount = mm; lCount = lm;}
                default -> throw new UnsupportedOperationException("Only ballistic, energy, and missile supported");
            }
            float lWeight = 1f - sWeight - mWeight;
            float totalWeight = sWeight * stotal + mWeight * mtotal + lWeight * ltotal;
            if (totalWeight <= 0f) return 0f;
            return (sWeight * sCount + mWeight * mCount + lWeight * lCount) / totalWeight;
        }


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
            stotal = sb + se + sm;
            mtotal = mb + me + mm;
            ltotal = lb + lm + lm;
        }
    }

    private static WeaponSlotCount countWeaponSlots(ShipHullSpecAPI spec, boolean includeMixedTypes) {
        int sb = 0, mb = 0, lb = 0, se = 0, me = 0,  le = 0, sm = 0, mm = 0, lm = 0;
        for (WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            if (slot.isBuiltIn() || slot.isDecorative() || !slot.isWeaponSlot()) continue;
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
                            if (includeMixedTypes) {sb++;se++;sm++;}
                            break;
                        case HYBRID:
                            if (includeMixedTypes) {sb++;se++;}
                            break;
                        case SYNERGY:
                            if (includeMixedTypes) {se++;sm++;}
                            break;
                        case COMPOSITE:
                            if (includeMixedTypes) {sb++;sm++;}
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
                            if (includeMixedTypes) {mb++;me++;mm++;}
                            break;
                        case HYBRID:
                            if (includeMixedTypes) {mb++;me++;}
                            break;
                        case SYNERGY:
                            if (includeMixedTypes) {me++;mm++;}
                            break;
                        case COMPOSITE:
                            if (includeMixedTypes) {mb++;mm++;}
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
                            if (includeMixedTypes) {lb++;le++;lm++;}
                            break;
                        case HYBRID:
                            if (includeMixedTypes) {lb++;le++;}
                            break;
                        case SYNERGY:
                            if (includeMixedTypes) {le++;lm++;}
                            break;
                        case COMPOSITE:
                            if (includeMixedTypes) {lb++;lm++;}
                            break;
                    }
                    break;
            }
        }
        return new WeaponSlotCount(sb, mb, lb, se, me, le, sm, mm, lm);
    }

    /** Sum will be greater than total number of weapon slots since i.e. synergy counts as 1 energy and 1 missile */
    public static WeaponSlotCount countWeaponSlots(ShipHullSpecAPI spec) {
        return countWeaponSlots(spec, true);
    }

    public static WeaponSlotCount countWeaponSlotsStrict(ShipHullSpecAPI spec) {
        return countWeaponSlots(spec, false);
    }

    public static String joinList(List<?> items) {
        switch (items.size()) {
            case 0: return "";
            case 1: return items.get(0).toString();
            case 2: return items.get(0).toString() + " " + Strings.Misc.and + items.get(1).toString();
            default:
                StringBuilder join = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    join.append(items.get(i));
                    if (i < items.size() - 1) {
                        join.append(", ");
                    }
                    if (i == items.size() - 2) {
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

    /** {@code isInPlayerFleet} distinguishes between ships actually in the player's fleet and the temporary ships
     *  used for refit screen purposes. Only the latter should refund LPCs, etc. */
    public static boolean fixVariantInconsistencies(MutableShipStatsAPI stats, boolean isInPlayerFleet) {
        ShipVariantAPI variant = stats.getVariant();
        List<String> wingIds = variant.getWings();
        // TODO: is there a better way to handle this? This doesn't handle the cass where a wing is both modular and
        // built-in on the same ship
        Set<String> nonBuiltIn = new HashSet<>(variant.getNonBuiltInWings());
        boolean changed = false;
        if (wingIds != null && !wingIds.isEmpty()) {
            for (int i = stats.getNumFighterBays().getModifiedInt(); i < wingIds.size(); i++) {
                if (variant.getWingId(i) != null) {
                    if (!isInPlayerFleet && nonBuiltIn.contains(variant.getWingId(i))) {
                        Global.getSector().getPlayerFleet().getCargo().addFighters(variant.getWingId(i), 1);
                    }
                    variant.setWingId(i, null);
                    changed = true;
                }
            }
        }
        if (clampOP(variant, Global.getSector().getPlayerStats())) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    Strings.Misc.excessOPWarning,
                    Misc.getNegativeHighlightColor());
            changed = true;
        }
        return changed;
    }

    public static void fixPlayerFleetInconsistencies() {
        for (FleetMemberAPI fm : Utils.getMembersNoSync(Global.getSector().getPlayerFleet())) {
            ShipVariantAPI variant = fm.getVariant();
            // This just sets hasOpAffectingMods to null, forcing the variant to
            // recompute its statsForOpCosts (e.g. number of hangar bays)
            // (Normally this is naturally set when a hullmod is manually added or removed)
            variant.addPermaMod("sms_masteryHandler");
            Utils.fixVariantInconsistencies(fm.getStats(), true);
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
        return members == null ? new ArrayList<>() : members;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean wasProjectileRemoved(DamagingProjectileAPI proj) {
        if (proj instanceof Missile) {
            return proj.wasRemoved();
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

    /** Weight is 0 from 0 to breakpoint 1,
     * increases exponentially from 0 to 1 between breakpoint1 and breakpoint2,
     * and finally increases linearly from 1 to 3 between breakpoint2 and breakpoint3. */
    public static float getSelectionWeightScaledByValueIncreasing(float value, float breakpoint1, float breakpoint2, float breakpoint3) {
        if (value <= breakpoint1) return 0f;
        else if (value <= breakpoint2) {
            return (float) ((Math.exp((value - breakpoint1) / (breakpoint2 - breakpoint1)) - 1f) / (Math.E - 1f));
        }
        else if (value <= breakpoint3){
            return 1f + 2f * (value - breakpoint2) / (breakpoint3 - breakpoint2);
        }
        else {
            return 3f;
        }
    }

    /** Weight is 3 from 0 to breakpoint 1,
     * decreases linearly from 3 to 1 between breakpoint1 and breakpoint2,
     * and finally decreases exponentially from 1 to 0 between breakpoint2 and breakpoint3. */
    public static float getSelectionWeightScaledByValueDecreasing(float value, float breakpoint1, float breakpoint2, float breakpoint3) {
        if (value <= breakpoint1) return 3f;
        else if (value <= breakpoint2) {
            return 3f - 2f * (value - breakpoint1) / (breakpoint2 - breakpoint1);
        } else if (value <= breakpoint3) {
            return (float) (1f / (1f - 1f / Math.E) * ((Math.exp((breakpoint2 - value) / (breakpoint3 - breakpoint2))) - 1f / Math.E));
        } else {
            return 0f;
        }
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

    public static float getShieldToHullArmorRatio(ShipHullSpecAPI spec) {
        if (!hasShield(spec)) return 0f;
        float averageHullArmor = 0.5f * (spec.getArmorRating()*spec.getArmorRating()/100f + spec.getHitpoints());
        float effectiveShields = spec.getFluxCapacity() / spec.getBaseShieldFluxPerDamageAbsorbed();
        return effectiveShields / averageHullArmor;
    }

    public static <T> T instantiateClassNoParams(Class<T> cls) throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
        try {
            //noinspection unchecked
            return (T) mh.invoke();
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public static void dumpMasteryFrequencies() {
        Map<String, Integer> freqs = new HashMap<>();
        Map<String, Integer> statFreqs = new HashMap<>();
        int maxKeyLength = 0;
        int maxStatKeyLength = 0;

        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec != getRestoredHullSpec(spec)) continue;

            for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
                List<String> optionIds = ShipMastery.getMasteryOptionIds(spec, i);
                List<MasteryEffect> effects = new ArrayList<>();
                for (String id : optionIds) {
                    effects.addAll(ShipMastery.getMasteryEffects(spec, i, id));
                }
                for (MasteryEffect effect : effects) {
                    String shortId = ShipMastery.getId(effect.getClass());
                    MasteryInfo info = ShipMastery.getMasteryInfo(shortId);
                    String id = shortId + "(" + info.tier + ")";
                    maxKeyLength = Math.max(maxKeyLength, id.length());

                    if (effect instanceof ModifyStatsEffect mEffect) {
                        for (ShipStat stat : mEffect.getAffectedStats()) {
                            String statId = stat.id + "(" + stat.tier + ")";
                            maxStatKeyLength = Math.max(maxStatKeyLength, statId.length());
                            statFreqs.compute(statId, (k, statFreq) -> statFreq == null ? 1 : statFreq + 1);
                        }
                    }

                    freqs.compute(id, (k, freq) -> freq == null ? 1 : freq + 1);
                }
            }
        }

        try (PrintWriter pw = new PrintWriter("mastery_freqs.txt")) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(freqs.entrySet());
            entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            List<Map.Entry<String, Integer>> statEntries = new ArrayList<>(statFreqs.entrySet());
            statEntries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            for (Map.Entry<String, Integer> entry : entries) {
                String sb = entry.getKey() + ":      " +
                        " ".repeat(Math.max(0, maxKeyLength - entry.getKey().length())) +
                        entry.getValue();
                pw.println(sb);
            }
            pw.println();
            for (Map.Entry<String, Integer> entry : statEntries) {
                String sb = entry.getKey() + ":      " +
                        " ".repeat(Math.max(0, maxStatKeyLength - entry.getKey().length())) +
                        entry.getValue();
                pw.println(sb);
            }
        } catch (Exception ignore) {}
    }
}
