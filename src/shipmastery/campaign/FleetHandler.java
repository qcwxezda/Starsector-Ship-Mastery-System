package shipmastery.campaign;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
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
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.*;

import java.util.*;

public class FleetHandler extends BaseCampaignEventListener implements FleetInflationListener {

    /** Commander id -> hull spec id -> levels. Don't use commander's memory as that gets put into the save file */
    public static final int MAX_CACHED_COMMANDERS = 100;
    /** Can be used to set custom mastery levels. Expects a Map<String, Map<Integer, Boolean>>
     *  hull spec id -> level -> which option is activated */
    public static final String CUSTOM_MASTERIES_KEY = "$sms_CustomMasteryData";
    public static final Map<String, Map<String, NavigableMap<Integer, Boolean>>> NPC_MASTERY_CACHE = new SizeLimitedMap<>(MAX_CACHED_COMMANDERS);

    public FleetHandler() {
        super(false);
    }

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
    public static ShipVariantAPI addHandlerMod(ShipVariantAPI variant, ShipVariantAPI root, CampaignFleetAPI fleet) {
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            variant = variant.clone();
            variant.setGoalVariant(false);
            variant.setSource(VariantSource.REFIT);
        }
        VariantLookup.addVariantInfo(variant, root, fleet);
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
            variant.setModuleVariant(id, addHandlerMod(variant.getModuleVariant(id), root, fleet));
        }
        return variant;
    }


    public static final float EXISTING_HULLMOD_WEIGHT = 3f, PRIORITY_HULLMOD_WEIGHT = 2f, STANDARD_HULLMOD_WEIGHT = 1f;
    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        PersonAPI commander = fleet.getCommander();
        CoreAutofitPlugin auto = new CoreAutofitPlugin(commander);
        Random random = new Random(fleet.getId().hashCode());
        auto.setRandom(random);

        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
            if (fm.isStation()) continue;
            ShipHullSpecAPI spec = fm.getVariant().getHullSpec();
            MutableShipStatsAPI stats = fm.getStats();
            NavigableMap<Integer, Boolean> masteries = getActiveMasteriesForCommander(commander, spec, fleet.getFlagship());
            fm.setVariant(addHandlerMod(fm.getVariant(), fm.getVariant(), fleet), false, false);
            final ShipVariantAPI variant = fm.getVariant();

            boolean repeatAutofit = false;
            for (Map.Entry<Integer, Boolean> entry : masteries.entrySet()) {
                for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, entry.getKey(), entry.getValue())) {
                    effect.applyEffectsBeforeShipCreation(variant.getHullSize(), stats);
                    repeatAutofit |= effect.hasTag(MasteryTags.TRIGGERS_AUTOFIT);
                }
            }

            if (inflater instanceof AutofitPlugin.AutofitPluginDelegate && !isNoAutofit(fleet, fm)) {
                AutofitPlugin.AutofitPluginDelegate delegate = (AutofitPlugin.AutofitPluginDelegate) inflater;
                boolean canAutofit = delegate.getAvailableFighters() != null && delegate.getAvailableWeapons() != null;

                auto.setChecked(CoreAutofitPlugin.STRIP, false);
                auto.setChecked(CoreAutofitPlugin.UPGRADE, random.nextFloat() < Math.min(0.1f + inflater.getQuality()*0.5f, 0.5f));
                auto.setChecked(CoreAutofitPlugin.RANDOMIZE, true);

                if (repeatAutofit && canAutofit) {
                    auto.doFit(variant, variant.clone(), 0, delegate);
                }

                float sModsToAdd = SModUtils.getMaxSMods(fm) - variant.getSMods().size();
                if (sModsToAdd > 0) {
                    WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

                    for (String hullmod : variant.getNonBuiltInHullmods()) {
                        picker.add(hullmod, EXISTING_HULLMOD_WEIGHT);
                    }

                    // Add a bunch of different always-applicable hull mods for variety
                    FactionAPI faction = fleet.getFaction();
                    addIfApplicable(HullMods.TURRETGYROS, false, picker, variant, faction);
                    addIfApplicable(HullMods.ARMOREDWEAPONS, false, picker, variant, faction);
                    addIfApplicable(HullMods.AUTOREPAIR, false, picker, variant, faction);
                    addIfApplicable(HullMods.AUXILIARY_THRUSTERS, false, picker, variant, faction);
                    addIfApplicable(HullMods.BLAST_DOORS, false, picker, variant, faction);
                    addIfApplicable(HullMods.ECCM, false, picker, variant, faction);
                    addIfApplicable(HullMods.MAGAZINES, false, picker, variant, faction);
                    addIfApplicable(HullMods.MISSLERACKS, false, picker, variant, faction);
                    addIfApplicable(HullMods.FLUXBREAKERS, false, picker, variant, faction);
                    addIfApplicable(HullMods.FLUX_COIL, false, picker, variant, faction);
                    addIfApplicable(HullMods.FLUX_DISTRIBUTOR, false, picker, variant, faction);
                    addIfApplicable(HullMods.HEAVYARMOR, false, picker, variant, faction);
                    addIfApplicable(HullMods.INSULATEDENGINE, false, picker, variant, faction);
                    addIfApplicable(HullMods.POINTDEFENSEAI, false, picker, variant, faction);
                    addIfApplicable(HullMods.SOLAR_SHIELDING, false, picker, variant, faction);
                    addIfApplicable(HullMods.UNSTABLE_INJECTOR, false, picker, variant, faction);

                    addIfApplicable(HullMods.AUGMENTEDENGINES, true, picker, variant, faction);
                    addIfApplicable(HullMods.INSULATEDENGINE, true, picker, variant, faction);
                    addIfApplicable(HullMods.SURVEYING_EQUIPMENT, true, picker, variant, faction);
                    addIfApplicable(HullMods.ADDITIONAL_BERTHING, true, picker, variant, faction);
                    addIfApplicable(HullMods.AUXILIARY_FUEL_TANKS, true, picker, variant, faction);
                    addIfApplicable(HullMods.EFFICIENCY_OVERHAUL, true, picker, variant, faction);
                    addIfApplicable(HullMods.EXPANDED_CARGO_HOLDS, true, picker, variant, faction);
                    addIfApplicable(HullMods.SOLAR_SHIELDING, true, picker, variant, faction);

                    for (int i = 0; i < sModsToAdd; i++) {
                        if (picker.isEmpty()) break;
                        if (random.nextFloat() > Settings.NPC_SMOD_QUALITY_MOD + inflater.getQuality()) continue;
                        variant.addPermaMod(picker.pickAndRemove(), true);
                    }
                }

                // If s-modding granted additional OP, do another fit
                if (variant.getUnusedOP(commander == null ? null : commander.getStats()) > 0f && canAutofit) {
                    auto.doFit(variant, variant.clone(), 0, (AutofitPlugin.AutofitPluginDelegate) inflater);
                }

                // Adjust CR if mastery effects affected that
                // TODO: figure out if this causes unwanted spontaneous repairs...
                fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
                // Do this again just to make sure mastery handler is at bottom of hullmod list=
                fm.setVariant(addHandlerMod(fm.getVariant(), fm.getVariant(), fleet), false, false);

//                float diff = fm.getRepairTracker().getMaxCR() - crBeforeModification;
//                if (diff > 0f) {
//                    fm.getRepairTracker().setCR(Math.min(fm.getRepairTracker().getMaxCR(), fm.getRepairTracker().getCR() + diff));
//                }
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

    public boolean isNoAutofit(CampaignFleetAPI fleet, FleetMemberAPI fm) {
        boolean forceAutofit = fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS);
        // From DefaultFleetInflater.java
        if (!forceAutofit && fm.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT)) {
            return true;
        }
        if (!forceAutofit && fm.getVariant() != null && fm.getVariant().hasTag(Tags.TAG_NO_AUTOFIT)) {
            return true;
        }

        if (fleet.getFaction() == null || !fleet.getFaction().isPlayerFaction()) {
            if (!forceAutofit && fm.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
                return true;
            }
            //noinspection RedundantIfStatement
            if (!forceAutofit && fm.getVariant() != null && fm.getVariant().hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
                return true;
            }
        }
        return false;
    }

    private void addIfApplicable(String hullmod, boolean civilian, WeightedRandomPicker<String> addTo, ShipVariantAPI check, FactionAPI faction) {
        if (!check.getPermaMods().contains(hullmod) && faction.knowsHullMod(hullmod)) {
            if (civilian == check.isCivilian()) {
                addTo.add(hullmod, faction.isHullModPriority(hullmod) ? PRIORITY_HULLMOD_WEIGHT : STANDARD_HULLMOD_WEIGHT);
            }
        }
    }

    public static FleetMemberAPI getFlagship(PersonAPI commander) {
        MutableCharacterStatsAPI stats = commander.getStats();
        // commander.getFleet() can be null, so have to use stats.getFleet
        if (stats == null) return null;
        CampaignFleetAPI fleet = stats.getFleet();
        if (fleet == null || fleet.getFleetData() == null || fleet.getFleetData().getMembersListCopy().isEmpty()) return null;
        return fleet.getFlagship();
    }

    public static NavigableMap<Integer, Boolean> getActiveMasteriesForCommander(final PersonAPI commander, ShipHullSpecAPI spec) {
        return getActiveMasteriesForCommander(commander, spec, getFlagship(commander));
    }

    public static NavigableMap<Integer, Boolean> getActiveMasteriesForCommander(final PersonAPI commander, ShipHullSpecAPI spec, FleetMemberAPI flagship) {
        if (commander == null || commander.isDefault()) return new TreeMap<>();
        if (commander.isPlayer()) return ShipMastery.getPlayerActiveMasteriesCopy(spec);

        NavigableMap<Integer, Boolean> cachedMasteries = getCachedNPCMasteries(commander, spec);
        if (cachedMasteries != null) return cachedMasteries;

        spec = Utils.getRestoredHullSpec(spec);
        NavigableMap<Integer, Boolean> map = new TreeMap<>();
        if (commander.getMemoryWithoutUpdate().contains(CUSTOM_MASTERIES_KEY)) {
            //noinspection unchecked
            Map<String, Map<Integer, Boolean>> custom =
                    (Map<String, Map<Integer, Boolean>>) commander.getMemoryWithoutUpdate().get(CUSTOM_MASTERIES_KEY);
            if (custom.containsKey(spec.getHullId())) {
                map.putAll(custom.get(spec.getHullId()));
            }
        }

        Random random = new Random(getCommanderAndHullSeed(commander, spec));

        int maxLevel = commander.getStats().getLevel() + Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER;

        String flagshipSpecId = flagship == null ? null : Utils.getRestoredHullSpecId(flagship.getHullSpec());
        if (Objects.equals(spec.getHullId(), flagshipSpecId)) {
            maxLevel += Settings.NPC_MASTERY_FLAGSHIP_BONUS;
        }
        else if (random.nextFloat() > Settings.NPC_MASTERY_DENSITY) return map;


        int level = 0, cap = ShipMastery.getMaxMasteryLevel(spec);

        for (int i = 0; i < maxLevel; i++) {
            if (i == 0 || random.nextFloat() <= Settings.NPC_MASTERY_QUALITY) {
                level++;
                if (level > cap) break;

                // Mapping might already exist due to custom masteries
                if (map.containsKey(level)) continue;

                boolean isOption2 = random.nextBoolean();
                if (!isOption2) {
                    map.put(level, false);
                }
                else {
                    List<MasteryEffect> option2 = ShipMastery.getMasteryEffects(spec, level, true);
                    map.put(level, !option2.isEmpty());
                }
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
        return (commander.getId() + spec.getHullId() + "___").hashCode();
    }
}
