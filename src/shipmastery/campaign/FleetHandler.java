package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.MasteryUtils;
import shipmastery.util.HullmodUtils;
import shipmastery.util.SizeLimitedMap;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

public class FleetHandler extends BaseCampaignEventListener implements FleetInflationListener, CoreAutoresolveListener, EveryFrameScript {

    /** Commander id -> hull spec id -> levels. Don't use commander's memory as that gets put into the save file */
    public static final int MAX_CACHED_COMMANDERS = 100;
    /** Can be used to set custom mastery levels. Place in commander's memory. Expects a Map<String, Map<Integer, String>>
     *  hull spec id -> level -> which option is activated */
    public static final String CUSTOM_MASTERIES_KEY = "$sms_CustomMasteryData";
    public static final String MINIMUM_MASTERY_LEVEL_KEY = "$sms_MinMasteryLevel";
    /** Can be used to set custom difficulty progression (between 0 and 1). Place in commander's memory. Mostly used
     *  so that the entries in recent battles stay the same regardless of further player progression. */
    public static final String CUSTOM_PROGRESSION_KEY = "$sms_DifficultyProgressionWhenFought";
    /** Variant tag placed on NPC variants to indicate that this handler has processed the fleet. Importantly, the tag will
     *  disappear when the fleet is deflated, signaling that this handler needs to reprocess the fleet. */
    public static final String VARIANT_PROCESSED_TAG = "sms_VariantProcessed";
    public static final Map<String, Map<String, NavigableMap<Integer, String>>> NPC_MASTERY_CACHE = new SizeLimitedMap<>(MAX_CACHED_COMMANDERS);

    public FleetHandler() {
        super(false);
    }

    public static void cacheNPCMasteries(PersonAPI commander, ShipHullSpecAPI spec, NavigableMap<Integer, String> levels) {
        Map<String, NavigableMap<Integer, String>> subMap = NPC_MASTERY_CACHE.computeIfAbsent(commander.getId(), k -> new HashMap<>());
        subMap.put(Utils.getRestoredHullSpecId(spec), levels);
    }

    public static NavigableMap<Integer, String> getCachedNPCMasteries(PersonAPI commander, ShipHullSpecAPI spec) {
        Map<String, NavigableMap<Integer, String>> subMap = NPC_MASTERY_CACHE.get(commander.getId());
        if (subMap == null) return null;
        return subMap.get(Utils.getRestoredHullSpecId(spec));
    }

    /** Modifies and returns the given variant if it's not a stock, goal, or empty variant
     *  (those can be duplicated across multiple ships).
     *  Otherwise, returns a modified copy of that variant. */
    public static ShipVariantAPI addHandlerMod(ShipVariantAPI variant, ShipVariantAPI root, FleetMemberAPI member) {
        boolean variantIsRoot = Objects.equals(variant, root);
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            variant = variant.clone();
            variant.setGoalVariant(false);
            variant.setSource(VariantSource.REFIT);
        }
        if (variantIsRoot) root = variant;
        VariantLookup.addVariantInfo(variant, root, member);
        // Bypass the arbitrary checks in removeMod since we're adding it back anyway
        // Makes sure the mastery handler is the last hullmod processed (backing DS is LinkedHashSet)
        variant.getHullMods().remove("sms_mastery_handler");
        variant.getHullMods().add("sms_mastery_handler");
        // This also sets hasOpAffectingMods to null, forcing variants to
        // recompute their statsForOpCosts
        // (Normally this is naturally set when a hullmod is manually added or removed)
        variant.addPermaMod("sms_mastery_handler");
        // Add the tracker to any modules as well
        for (String id : variant.getModuleSlots()) {
            variant.setModuleVariant(id, addHandlerMod(variant.getModuleVariant(id), root, member));
        }
        return variant;
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (dialog.getInteractionTarget() instanceof CampaignFleetAPI fleet) {
            addMasteriesToFleet(fleet);
        }
    }

    public static void addMasteriesToFleet(CampaignFleetAPI fleet) {
        // Ignore custom production "fleets", they will have the player as their commander
        PersonAPI commander = fleet.getCommander();
        if (commander.isPlayer()) return;

        List<? extends FleetMemberAPI> members = Utils.getMembersNoSync(fleet);
        var inflater = fleet.getInflater();
        CoreAutofitPlugin auto = new CoreAutofitPlugin(commander);
        Random random = new Random(commander.getId().hashCode());
        auto.setRandom(random);

        String factionId = fleet.getFaction() == null ? Utils.defaultFactionId : fleet.getFaction().getId();
        Utils.DifficultyData difficultyData = Utils.difficultyDataMap.getOrDefault(factionId, Utils.defaultDifficultyData);

        Map<String, List<FleetMemberAPI>> membersOfSpecMap = new HashMap<>();
        members.forEach(
                fm -> membersOfSpecMap.computeIfAbsent(Utils.getRestoredHullSpecId(fm.getHullSpec()), lst -> new ArrayList<>()).add(fm)
        );

        for (FleetMemberAPI fm : members) {
            // Fleets may have some ships that are deflated and others that aren't, due to different autofit rules
            // per ship
            if (fm.getVariant().hasTag(VARIANT_PROCESSED_TAG)) continue;
            ShipHullSpecAPI spec = fm.getVariant().getHullSpec();
            MutableShipStatsAPI stats = fm.getStats();

            float maxBeforeModification = fm.getRepairTracker().getMaxCR();
            float crBeforeModification = fm.getRepairTracker().getCR();
            NavigableMap<Integer, String> masteries = getActiveMasteriesForCommander(
                    commander,
                    spec,
                    membersOfSpecMap.get(Utils.getRestoredHullSpecId(spec)),
                    fleet.getFlagship());
            fm.setVariant(addHandlerMod(fm.getVariant(), fm.getVariant(), fm), false, false);

            final ShipVariantAPI variant = fm.getVariant();
            boolean repeatAutofit = false;
            for (Map.Entry<Integer, String> entry : masteries.entrySet()) {
                for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, entry.getKey(), entry.getValue())) {
                    effect.applyEffectsBeforeShipCreation(variant.getHullSize(), stats);
                    repeatAutofit |= effect.hasTag(MasteryTags.TRIGGERS_AUTOFIT);
                }
            }

            DeferredActionPlugin.performLater(() -> {
                float newMax = fm.getRepairTracker().getMaxCR();
                float diff = newMax - maxBeforeModification;
                if (diff > 0f) {
                    fm.getRepairTracker().setCR(Math.min(crBeforeModification + diff, fm.isStation() ? 1f : newMax));
                }
            }, 0f);

            if (!isNoAutofit(fleet, fm)) {
                boolean canAutofit = false;
                if (inflater instanceof AutofitPlugin.AutofitPluginDelegate delegate) {
                    canAutofit = delegate.getAvailableFighters() != null && delegate.getAvailableWeapons() != null;
                    auto.setChecked(CoreAutofitPlugin.STRIP, false);
                    auto.setChecked(CoreAutofitPlugin.UPGRADE, random.nextFloat() < Math.min(0.1f + inflater.getQuality() * 0.5f, 0.5f));
                    auto.setChecked(CoreAutofitPlugin.RANDOMIZE, true);
                    if (repeatAutofit && canAutofit) {
                        auto.doFit(variant, variant.clone(), 0, delegate);
                    }
                }

                int sModsToAdd = HullmodUtils.getMaxSMods(fm) - variant.getSMods().size();
                float prob = difficultyData.baseSModProb() * (float) Math.pow(difficultyData.sModProbMultPerDMod(), DModManager.getNumDMods(variant));
                addAdditionalSModsToVariant(variant, sModsToAdd, fleet, random, prob);

                // If s-modding granted additional OP, do another fit
                if (variant.getUnusedOP(commander.getStats()) > 0f && canAutofit) {
                    auto.doFit(variant, variant.clone(), 0, (AutofitPlugin.AutofitPluginDelegate) inflater);
                }

                // Adjust CR if mastery effects affected that
                //fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
                // Do this again just to make sure mastery handler is at bottom of hullmod list
                variant.getHullMods().remove("sms_mastery_handler");
                variant.getHullMods().add("sms_mastery_handler");
                variant.addPermaMod("sms_mastery_handler");
            }

            variant.addTag(VARIANT_PROCESSED_TAG);

            if (!masteries.isEmpty()) {
                int level = masteries.lastEntry().getKey();
                if (level >= 1) {
                    level = Math.min(level, 15);
                    fm.getVariant().addMod("sms_npc_indicator" + level);
                }
            }
        }
    }

    private static void addAdditionalSModsToVariant(ShipVariantAPI variant, int count, CampaignFleetAPI fleet, Random random, float chanceToAddPer) {
        for (String id : variant.getModuleSlots()) {
            addAdditionalSModsToVariant(variant.getModuleVariant(id), count, fleet, random, chanceToAddPer);
        }

        if (count <= 0 || variant.getHullSpec().getOrdnancePoints(null) <= 0) return;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        picker.setRandom(random);

        for (String hullmod : variant.getNonBuiltInHullmods()) {
            var spec = Global.getSettings().getHullModSpec(hullmod);
            if (spec == null || spec.hasTag(Tags.HULLMOD_NO_BUILD_IN)) continue;
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

        addIfApplicable(HullMods.AUGMENTEDENGINES, true, picker, variant, faction);
        addIfApplicable(HullMods.INSULATEDENGINE, true, picker, variant, faction);
        addIfApplicable(HullMods.SURVEYING_EQUIPMENT, true, picker, variant, faction);
        addIfApplicable(HullMods.ADDITIONAL_BERTHING, true, picker, variant, faction);
        addIfApplicable(HullMods.AUXILIARY_FUEL_TANKS, true, picker, variant, faction);
        addIfApplicable(HullMods.EFFICIENCY_OVERHAUL, true, picker, variant, faction);
        addIfApplicable(HullMods.EXPANDED_CARGO_HOLDS, true, picker, variant, faction);
        addIfApplicable(HullMods.SOLAR_SHIELDING, true, picker, variant, faction);

        for (int i = 0; i < count; i++) {
            if (picker.isEmpty()) break;
            if (random.nextFloat() > chanceToAddPer) continue;
            variant.addPermaMod(picker.pickAndRemove(), true);
        }
    }


    public static final float EXISTING_HULLMOD_WEIGHT = 30f, PRIORITY_HULLMOD_WEIGHT = 2f, STANDARD_HULLMOD_WEIGHT = 1f;
    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        addMasteriesToFleet(fleet);
    }

    public static boolean isNoAutofit(CampaignFleetAPI fleet, FleetMemberAPI fm) {
        boolean forceAutofit = fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS);
        if (forceAutofit) return false;

        ShipVariantAPI variant = fm.getVariant();

        // Adapted from DefaultFleetInflater.java
        if (fm.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT)) {
            return true;
        }
        if (variant != null && variant.hasTag(Tags.TAG_NO_AUTOFIT)) {
            return true;
        }

        if (fleet.getFaction() == null || !fleet.getFaction().isPlayerFaction()) {
            if (fm.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
                return true;
            }
            return variant != null && variant.hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER);
        }
        return false;
    }

    private static void addIfApplicable(String hullmod, boolean civilian, WeightedRandomPicker<String> addTo, ShipVariantAPI check, FactionAPI faction) {
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
        if (fleet == null || fleet.getFleetData() == null) return null;
        return fleet.getFlagship();
    }

    public static List<? extends FleetMemberAPI> getMembersNoSync(PersonAPI commander) {
        MutableCharacterStatsAPI stats = commander.getStats();
        // commander.getFleet() can be null, so have to use stats.getFleet
        if (stats == null) return null;
        CampaignFleetAPI fleet = stats.getFleet();
        if (fleet == null || fleet.getFleetData() == null) return null;
        return Utils.getMembersNoSync(fleet);
    }

    public static NavigableMap<Integer, String> getActiveMasteriesForCommander(final PersonAPI commander, ShipHullSpecAPI spec) {
        return getActiveMasteriesForCommander(commander, spec, null, getFlagship(commander));
    }

    // If membersOfSpec is not provided, it will be rebuilt from the commander's fleet data
    public static NavigableMap<Integer, String> getActiveMasteriesForCommander(
            final PersonAPI commander,
            ShipHullSpecAPI spec,
            @Nullable List<? extends FleetMemberAPI> membersOfSpec,
            FleetMemberAPI flagship) {
        if (commander == null || commander.isDefault()) return new TreeMap<>();
        if (commander.isPlayer()) return ShipMastery.getPlayerActiveMasteriesCopy(spec);

        NavigableMap<Integer, String> cachedMasteries = getCachedNPCMasteries(commander, spec);
        if (cachedMasteries != null) return cachedMasteries;

        spec = Utils.getRestoredHullSpec(spec);
        NavigableMap<Integer, String> map = new TreeMap<>();
        if (commander.getMemoryWithoutUpdate().contains(CUSTOM_MASTERIES_KEY)) {
            //noinspection unchecked
            Map<String, Map<Integer, String>> custom =
                    (Map<String, Map<Integer, String>>) commander.getMemoryWithoutUpdate().get(CUSTOM_MASTERIES_KEY);
            if (custom.containsKey(spec.getHullId())) {
                map.putAll(custom.get(spec.getHullId()));
            }
        }

        String factionId = commander.getFaction() == null ? Utils.defaultFactionId : commander.getFaction().getId();
        Utils.DifficultyData data = Utils.difficultyDataMap.getOrDefault(factionId, Utils.defaultDifficultyData);

        float progression;
        if (commander.getMemoryWithoutUpdate().contains(CUSTOM_PROGRESSION_KEY)) {
            Float savedProgression = (Float) commander.getMemoryWithoutUpdate().get(CUSTOM_PROGRESSION_KEY);
            progression = savedProgression == null ? 0f : savedProgression;
        } else {
            progression = Settings.NPC_PROGRESSION_ENABLED ? PlayerMPHandler.getDifficultyProgression() : 0f;
        }

        Random random = new Random(getCommanderAndHullSeed(commander, spec));

        float bonus = data.averageModifier();
        float averageLevel = commander.getStats().getLevel()/3f + bonus + getNPCLevelModifier(progression);
        float masteryStrength = data.masteryStrengthBonus();
        commander.getStats().getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).modifyPercent(FleetHandler.class.getName(), 100f*masteryStrength);

        String flagshipSpecId = flagship == null ? null : Utils.getRestoredHullSpecId(flagship.getHullSpec());
        if (Objects.equals(spec.getHullId(), flagshipSpecId)) {
            averageLevel += data.flagshipBonus();
        }

        float actualLevel = (float) random.nextGaussian(averageLevel, data.stDev());
        int cap = ShipMastery.getMaxMasteryLevel(spec);
        actualLevel = Math.max(actualLevel, commander.getMemoryWithoutUpdate().getFloat(MINIMUM_MASTERY_LEVEL_KEY));
        float fracPart = actualLevel - (int) actualLevel;
        if (random.nextFloat() < fracPart) {
            actualLevel++;
        }

        List<? extends FleetMemberAPI> fmsOfSpec;
        if (membersOfSpec != null) {
            fmsOfSpec = membersOfSpec;
        } else {
            var members = getMembersNoSync(commander);
            ShipHullSpecAPI finalSpec = spec;
            fmsOfSpec = members == null ? new ArrayList<>() : members
                    .stream().filter(fm -> Objects.equals(finalSpec.getHullId(), Utils.getRestoredHullSpecId(fm.getHullSpec()))).toList();
        }

        for (int level = 1; level <= actualLevel; level++) {
            if (level > cap) break;
            // Mapping might already exist due to custom masteries
            if (map.containsKey(level)) continue;
            String optionId = pickOptionForMasteryLevel(spec, fmsOfSpec, level, random);
            if (optionId != null) {
                map.put(level, optionId);
            }
        }

        // Once NPC mastery levels have been generated for the first time, activate the corresponding masteries
        MasteryUtils.applyMasteryEffects(spec, map, false, effect -> effect.onActivate(commander));
        cacheNPCMasteries(commander, spec, map);

        return map;
    }

    // Default mastery level picker.
    public static @Nullable String pickOptionForMasteryLevel(
            ShipHullSpecAPI spec,
            @NotNull List<? extends FleetMemberAPI> membersOfSpec,
            int level,
            Random random) {
        List<String> allKeys = ShipMastery.getMasteryOptionIds(spec, level);
        if (allKeys.isEmpty()) return null;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(random);
        for (String option : allKeys) {
            float p = (float) ShipMastery.getMasteryEffects(spec, level, option)
                    .stream()
                    .mapToDouble(effect ->
                            membersOfSpec.stream()
                                    .mapToDouble(effect::getNPCWeight)
                                    .reduce(Double::sum)
                                    .orElse(0f))
                    .reduce(Double::sum).orElse(0f);
            picker.add(option, p + 0.00000001f);
        }

        return picker.pick();
    }

    public static float getNPCLevelModifier(float progression) {
        return (1f - progression) * Settings.NPC_MASTERY_LEVEL_MODIFIER + progression * Settings.NPC_MASTERY_LEVEL_MODIFIER_CAP;
    }

    public static int getCommanderAndHullSeed(PersonAPI commander, ShipHullSpecAPI spec) {
        return (commander.getId() + spec.getHullId() + "___").hashCode();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    private CampaignFleetAPI lastSeenFleet;
    @Override
    public void advance(float amount) {
        var dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) return;
        var target = dialog.getInteractionTarget();
        if (!(target instanceof CampaignFleetAPI fleet)) return;
        if (fleet != lastSeenFleet) {
            addMasteriesToFleet(fleet);
        }
        lastSeenFleet = fleet;
    }

    @Override
    public void modifyDataForFleet(BattleAutoresolverPluginImpl.FleetAutoresolveData data) {
        var commander = data.fleet.getCommander();
        if (commander == null) return;

        data.members.forEach(memberData -> {
            int level = getActiveMasteriesForCommander(commander, memberData.member.getHullSpec()).size();
            float origStrength = memberData.strength;
            memberData.strength *= Math.min(1.35f, 1f + level * 0.035f);
            data.fightingStrength += memberData.strength - origStrength;
        });
    }
}
