package shipmastery.campaign;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
import shipmastery.util.SizeLimitedMap;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.util.*;

public class FleetHandler implements FleetInflationListener {

    /** Commander id -> hull spec id -> levels. Don't use commander's memory as that gets put into the save file */
    public static final Map<String, Map<String, NavigableMap<Integer, Boolean>>> NPC_MASTERY_CACHE = new SizeLimitedMap<>(Settings.MAX_CACHED_COMMANDERS);

    public static void cacheNPCMasteries(PersonAPI commander, ShipHullSpecAPI spec, NavigableMap<Integer, Boolean> levels) {
        Map<String, NavigableMap<Integer, Boolean>> subMap = NPC_MASTERY_CACHE.get(commander.getId());
        if (subMap == null) {
            subMap = new HashMap<>();
            NPC_MASTERY_CACHE.put(commander.getId(), subMap);
        }
        subMap.put(Utils.getRestoredHullSpecId(spec), levels);
    }

    public static NavigableMap<Integer, Boolean> getCachedNPCMasteries(PersonAPI commander, ShipHullSpecAPI spec) {
        Map<String, NavigableMap<Integer, Boolean>> subMap = NPC_MASTERY_CACHE.get(commander.getId());
        if (subMap == null) return null;
        return subMap.get(Utils.getRestoredHullSpecId(spec));
    }

    /** Modifies and returns the given variant if it's not a stock, goal, or empty variant
     *  (those can be duplicated across multiple ships).
     *  Otherwise, returns a modified copy of that variant. */
    public static ShipVariantAPI addHandlerMod(ShipVariantAPI variant, ShipVariantAPI root, PersonAPI commander) {
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            variant = variant.clone();
            variant.setSource(VariantSource.REFIT);
        }
        VariantLookup.addVariantInfo(variant, root, commander);
        // Bypass the arbitrary checks in removeMod since we're adding it back anyway
        // Makes sure the mastery handler is the last hullmod processed (backing DS is LinkedHashSet)
        variant.getHullMods().remove("sms_masteryHandler");
        variant.getHullMods().add("sms_masteryHandler");
        // This also sets hasOpAffectingMods to null, forcing variants to
        // recompute their statsForOpCosts
        // (Normally this is naturally set when a hullmod is manually added or removed)
        variant.addPermaMod("sms_masteryHandler");
        // Add the tracker to any modules as well
        for (String id : variant.getModuleSlots()) {
            variant.setModuleVariant(id, addHandlerMod(variant.getModuleVariant(id), root, commander));
        }
        return variant;
    }

    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
            final ShipVariantAPI variant = fm.getVariant();
            ShipHullSpecAPI spec = variant.getHullSpec();
            variant.addPermaMod("sms_masteryHandler", false);
            MutableShipStatsAPI stats = variant.getStatsForOpCosts();
            PersonAPI commander = fleet.getCommander();
            NavigableMap<Integer, Boolean> masteries = getActiveMasteriesForCommander(commander, spec, fleet.getFlagship());
            fm.setVariant(addHandlerMod(variant, variant, commander), false, false);
            for (Map.Entry<Integer, Boolean> entry : masteries.entrySet()) {
                for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, entry.getKey(), entry.getValue())) {
                    effect.applyEffectsBeforeShipCreation(variant.getHullSize(), stats);
                }
            }
            float sMods = stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).computeEffective(0f) - variant.getSMods().size();

            if (sMods > 0) {
                WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
                picker.addAll(variant.getNonBuiltInHullmods());

                // Add a bunch of different always-applicable hull mods for variety
                FactionAPI faction = fleet.getFaction();
                addIfApplicable(HullMods.TURRETGYROS, picker, variant, faction);
                addIfApplicable(HullMods.ARMOREDWEAPONS, picker, variant, faction);
                addIfApplicable(HullMods.AUTOREPAIR, picker, variant, faction);
                addIfApplicable(HullMods.AUXILIARY_THRUSTERS, picker, variant, faction);
                addIfApplicable(HullMods.BLAST_DOORS, picker, variant, faction);
                addIfApplicable(HullMods.ECCM, picker, variant, faction);
                addIfApplicable(HullMods.MAGAZINES, picker, variant, faction);
                addIfApplicable(HullMods.MISSLERACKS, picker, variant, faction);
                addIfApplicable(HullMods.FLUXBREAKERS, picker, variant, faction);
                addIfApplicable(HullMods.FLUX_COIL, picker, variant, faction);
                addIfApplicable(HullMods.FLUX_DISTRIBUTOR, picker, variant, faction);
                addIfApplicable(HullMods.HEAVYARMOR, picker, variant, faction);
                addIfApplicable(HullMods.INSULATEDENGINE, picker, variant, faction);
                addIfApplicable(HullMods.POINTDEFENSEAI, picker, variant, faction);
                addIfApplicable(HullMods.SOLAR_SHIELDING, picker, variant, faction);
                addIfApplicable(HullMods.UNSTABLE_INJECTOR, picker, variant, faction);

                for (int i = (int) sMods; i >= 0; i--) {
                    if (picker.isEmpty()) break;
                    variant.addPermaMod(picker.pickAndRemove(), true);
                }
            }

            if (!masteries.isEmpty()) {
                int level = masteries.lastEntry().getKey();
                if (level >= 1) {
                    level = Math.min(level, 9);
                    fm.getVariant().addMod("sms_npcIndicator" + level);
                }
            }
        }
    }

    private void addIfApplicable(String hullmod, WeightedRandomPicker<String> addTo, ShipVariantAPI check, FactionAPI faction) {
        if (!check.getPermaMods().contains(hullmod) && faction.knowsHullMod(hullmod)) {
            addTo.add(hullmod, faction.isHullModPriority(hullmod) ? 2f : 1f);
        }
    }

    public static FleetMemberAPI getFlagship(PersonAPI commander) {
        MutableCharacterStatsAPI stats = commander.getStats();
        if (stats == null) return null;
        CampaignFleetAPI fleet = stats.getFleet();
        if (fleet == null) return null;
        return fleet.getFlagship();
    }

    public static NavigableMap<Integer, Boolean> getActiveMasteriesForCommander(final PersonAPI commander, ShipHullSpecAPI spec) {
        return getActiveMasteriesForCommander(commander, spec, getFlagship(commander));
    }

    public static NavigableMap<Integer, Boolean> getActiveMasteriesForCommander(final PersonAPI commander, ShipHullSpecAPI spec, FleetMemberAPI flagship) {
        if (commander == null) return new TreeMap<>();
        if (commander.isPlayer()) return ShipMastery.getPlayerActiveMasteriesCopy(spec);

        NavigableMap<Integer, Boolean> cachedMasteries = getCachedNPCMasteries(commander, spec);
        if (cachedMasteries != null) return cachedMasteries;

        spec = Utils.getRestoredHullSpec(spec);
        Random random = new Random(getCommanderAndHullSeed(commander, spec));
        if (random.nextFloat() > Settings.NPC_MASTERY_DENSITY) return new TreeMap<>();

        int maxLevel = commander.getStats().getLevel() + Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER;

        String flagshipSpecId = flagship == null ? null : Utils.getRestoredHullSpecId(flagship.getHullSpec());
        if (Objects.equals(spec.getHullId(), flagshipSpecId)) {
            maxLevel += Settings.NPC_MASTERY_FLAGSHIP_BONUS;
        }

        int level = 0, cap = ShipMastery.getMaxMasteryLevel(spec);
        NavigableMap<Integer, Boolean> map = new TreeMap<>();

        for (int i = 0; i < maxLevel; i++) {
            if (i == 0 || random.nextFloat() <= Settings.NPC_MASTERY_QUALITY) {
                level++;

                boolean isOption2 = random.nextBoolean();
                if (!isOption2) {
                    map.put(level, false);
                }
                else {
                    List<MasteryEffect> option2 = ShipMastery.getMasteryEffects(spec, level, true);
                    map.put(level, !option2.isEmpty());
                }

                if (level >= cap) break;
            }
        }

        // Once NPC mastery levels have been generated for the first time, activate the corresponding masteries
        MasteryUtils.applyMasteryEffects(spec, map, false, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect) {
                effect.onActivate(commander);
            }
        });

        cacheNPCMasteries(commander, spec, map);

        return map;
    }

    public static int getCommanderAndHullSeed(PersonAPI commander, ShipHullSpecAPI spec) {
        return ("" + commander.getId() + spec.getHullId() + "___").hashCode();
    }
}
