package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityTooltipModifier;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.achievements.MasteredMany;
import shipmastery.campaign.items.BasePseudocorePlugin;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.campaign.CuratorFleetHandler;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.PlayerFleetHandler;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.campaign.RefitHandler;
import shipmastery.campaign.graveyard.InsuranceFraudDetector;
import shipmastery.campaign.graveyard.ShipGraveyardSpawner;
import shipmastery.campaign.items.PseudocorePlugin;
import shipmastery.campaign.listeners.FleetSyncListenerHandler;
import shipmastery.campaign.listeners.PlayerGainedMPListenerHandler;
import shipmastery.campaign.recentbattles.RecentBattlesIntel;
import shipmastery.campaign.recentbattles.RecentBattlesTracker;
import shipmastery.combat.CombatListenerManager;
import shipmastery.config.LunaLibSettingsListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.hullmods.PseudocoreUplinkHullmod;
import shipmastery.aicoreinterface.FracturedGammaCoreInterface;
import shipmastery.procgen.Generator;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    private static String lastSaveId = null;
    public static final String RANDOM_MODE_KEY = "$sms_IsRandomMode";
    public static final String GENERATION_SEED_KEY = "$sms_MasteryGenerationSeed";
    public static final ReflectionEnabledClassLoader classLoader;

    static {
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        classLoader = new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        ShipMastery.loadAliases();
        Utils.init();
        initializeCuratorFaction();
        ShipMastery.loadMasteries();
        ShipMastery.loadStats();
        ShipMastery.loadAICoreInterfaces();
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            LunaLibSettingsListener.init();
        }
        else {
            Settings.loadSettingsFromJson();
        }

        var miscAptitudeSpec = Global.getSettings().getSkillSpec("sms_aptitude_misc");
        miscAptitudeSpec.addTag("npc_only");
        miscAptitudeSpec.addTag("ai_core_only");
        miscAptitudeSpec.addTag("hide_in_codex");

        // Load these particular portrait sprites manually as we do not want these to be
        // random officers
        particleengine.Utils.getLoadedSprite("graphics/portraits/sms_portrait_warped_pseudocore.png");
        particleengine.Utils.getLoadedSprite("graphics/portraits/sms_portrait_crystalline_pseudocore.png");
        particleengine.Utils.getLoadedSprite("graphics/portraits/sms_portrait_amorphous_pseudocore.png");
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        CodexDataV2.makeRelated(
                CodexDataV2.getSkillEntryId("sms_shared_knowledge"),
                CodexDataV2.getItemEntryId("sms_construct"),
                CodexDataV2.getCommodityEntryId("sms_gamma_pseudocore"),
                CodexDataV2.getCommodityEntryId("sms_beta_pseudocore"),
                CodexDataV2.getCommodityEntryId("sms_alpha_pseudocore"));
        CodexDataV2.makeRelated(
                CodexDataV2.getSkillEntryId("sms_warped_knowledge"),
                CodexDataV2.getCommodityEntryId("sms_warped_pseudocore"));
        CodexDataV2.makeRelated(
                CodexDataV2.getSkillEntryId("sms_crystalline_knowledge"),
                CodexDataV2.getCommodityEntryId("sms_crystalline_pseudocore"));
    }

    private void initializeCuratorFaction() {
        FactionSpecAPI thisFaction = Global.getSettings().getFactionSpec("sms_curator");
        thisFaction.getKnownHullMods().addAll(Global.getSettings().getAllHullModSpecs().stream().filter(
                spec -> !spec.hasTag(Tags.RESTRICTED)
                        && !spec.hasTag(Tags.HIDE_IN_CODEX)
                        && !spec.hasTag(Tags.NO_DROP)
                        && !spec.hasTag(Tags.HULLMOD_NO_DROP_SALVAGE)
        ).map(HullModSpecAPI::getId).toList());
        thisFaction.getKnownHullMods().remove(HullMods.PHASE_ANCHOR);

        Set<String> allowedHiddenFactions = new HashSet<>();
        allowedHiddenFactions.add("lions_guard");
        allowedHiddenFactions.add("mercenary");
        allowedHiddenFactions.add("remnant");
        allowedHiddenFactions.add("derelict");

        for (FactionSpecAPI faction : Global.getSettings().getAllFactionSpecs()) {
            if (!faction.isShowInIntelTab() && !allowedHiddenFactions.contains(faction.getId())) continue;
            if (Objects.equals(faction.getId(), thisFaction.getId())) continue;
            thisFaction.getKnownFighters().addAll(
                    faction.getKnownFighters().stream().filter(
                            id -> {
                                var spec = Global.getSettings().getFighterWingSpec(id);
                                return !spec.hasTag(Tags.RESTRICTED) && !spec.hasTag(Tags.HIDE_IN_CODEX);
                            }).toList());
            thisFaction.getKnownWeapons().addAll(
                    faction.getKnownWeapons().stream().filter(
                            id -> {
                                var spec = Global.getSettings().getWeaponSpec(id);
                                return !spec.hasTag(Tags.RESTRICTED)
                                        && !spec.hasTag(Tags.HIDE_IN_CODEX)
                                        && !spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM);
                            }).toList());
            var knownShips = faction.getKnownShips().stream().filter(
                    id -> {
                        var spec = Global.getSettings().getHullSpec(id);
                        return spec == Utils.getRestoredHullSpec(spec)
                            && !spec.hasTag(Tags.RESTRICTED)
                            && !spec.hasTag(Tags.DWELLER)
                            && !spec.hasTag(Tags.THREAT)
                            && !spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.HIDE_IN_CODEX)
                            && !spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.CIVILIAN)
                            && !spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.MODULE)
                            && !spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION);
                    }).toList();
            thisFaction.getKnownShips().addAll(knownShips);
            thisFaction.getShipsWhenImporting().addAll(knownShips);
        }

        for (int i = 0; i < 4; i++) {
            int finalI = i;
            List<ShipHullSpecAPI> mostFP = thisFaction.getKnownShips().stream()
                    .map(x -> Global.getSettings().getHullSpec(x))
                    .filter(x -> Utils.hullSizeToInt(x.getHullSize()) == finalI && x == Utils.getRestoredHullSpec(x))
                    .sorted((a, b) -> b.getFleetPoints()-a.getFleetPoints())
                    .toList();
            Map<String, Integer> manufacturerCount = new HashMap<>();
            float dpLimit = 20f * (i + 1);
            int picked = 0;
            int limit = 20;
            for (int j = 0; j < mostFP.size(); j++) {
                var spec = mostFP.get(j);
                int maxPerManufacturer = 3;
                var manufacturer = spec.getManufacturer();
                if ("High Tech".equals(manufacturer) || "Midline".equals(manufacturer) || "Low Tech".equals(manufacturer)) {
                    maxPerManufacturer *= 2;
                }
                if (j == 0 || (spec.getSuppliesToRecover() <= dpLimit && manufacturerCount.getOrDefault(manufacturer, 0) < maxPerManufacturer)) {
                    thisFaction.getPriorityShips().add(spec.getHullId());
                    if (manufacturer != null) {
                        manufacturerCount.compute(manufacturer, (k, v) -> v == null ? 1 : v + 1);
                    }
                    dpLimit *= 0.965f;
                    picked++;
                    if (picked > limit) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void beforeGameSave() {
        // Technically it's possible to save inside a dialog using console commands, but this should never normally be needed
        // since the memory key is set with an expiry of 0
        Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().unset(RefitHandler.CURRENT_REFIT_SHIP_KEY);
    }

    @Override
    public void afterGameSave() {
        ShipMastery.clearRerolledSpecsThisSave();
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        if (wasEnabledBefore) return;

        boolean randomMode = Settings.ENABLE_RANDOM_MODE;
        Global.getSector().getPersistentData().put(RANDOM_MODE_KEY, randomMode);
        new Generator().generate();
        Global.getSector().getPlayerFaction().ensureAtBest("sms_curator", RepLevel.HOSTILE);

        // Not transient in case player saves while action queue isn't empty
        // Possibly broken though !! - can't save lambdas
        DeferredActionPlugin deferredActionPlugin = new DeferredActionPlugin();
        Global.getSector().addScript(deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(DeferredActionPlugin.INSTANCE_KEY, deferredActionPlugin);

        // Start with the modspec, rather than the mod just unlocked, to highlight that the option exists
        Global.getSector().getPlayerFleet().getCargo().addHullmods(Strings.Hullmods.ENGINEERING_OVERRIDE, 1);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        boolean randomMode = (boolean) Global.getSector().getPersistentData().get(RANDOM_MODE_KEY);
        if (newGame) {
            String seed = Settings.RANDOM_GENERATION_SEED;
            if (seed == null || seed.trim().isEmpty()) {
                seed = Global.getSector().getPlayerPerson().getId();
            }
            Global.getSector().getPersistentData().put(GENERATION_SEED_KEY, seed);
        } else {
            String savedKey = (String) Global.getSector().getPersistentData().get(GENERATION_SEED_KEY);
            if (savedKey == null) {
                Global.getSector().getPersistentData().put(GENERATION_SEED_KEY, Global.getSector().getPlayerPerson().getId());
            }
        }

        ShipMastery.loadMasteryTable();
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        CombatListenerManager.clearLastBattleCreationContext();
        new EngineUtils.ClearCacheOnCombatEnd().onCombatEnd();
        PlayerGainedMPListenerHandler.clearListeners();
        FleetSyncListenerHandler.clearListeners();

        try {
            EveryFrameScript initializer = (EveryFrameScript) Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.campaign.Initializer"));
            Global.getSector().addTransientScript(initializer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add initializer", e);
        }

        if (Global.getSettings().getModManager().isModEnabled("nexerelin") &&
                !listeners.hasListenerOfClass(InsuranceFraudDetector.class)) {
            listeners.addListener(new InsuranceFraudDetector(), false);
        }

        Object coreTabListenerHandler;
        MethodHandle registerCoreTabListener;
        try {
            coreTabListenerHandler = Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.campaign.listeners.CoreTabListenerHandler"));
            // Can't do normal casting cause different classloaders
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            registerCoreTabListener = lookup.findVirtual(coreTabListenerHandler.getClass(), "registerListener", MethodType.methodType(void.class, CoreTabListener.class));
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (!Settings.DISABLE_MAIN_FEATURES) {
            Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).setHiddenEverywhere(false);
            // Time to generate masteries is roughly 1 second per 10,000 ship hull specs
            // (Tradeoff between saving the masteries in file and generating them on the fly from seed)
            String id = Global.getSector().getPlayerPerson().getId();
            try {
                if (!id.equals(lastSaveId)) {
                    lastSaveId = id;
                    ShipMastery.initMasteries(randomMode);
                    ShipMastery.activatePlayerMasteries();
                } else {
                    ShipMastery.clearRerolledMasteries();
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

            Object refitHandler;
            try {
                refitHandler = Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.campaign.RefitHandler"));
                listeners.addListener(refitHandler, true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add refit tab modifier", e);
            }

            Object fleetPanelHandler;
            try {
                fleetPanelHandler = Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.campaign.FleetPanelHandler"));
                Global.getSector().addTransientScript((EveryFrameScript) fleetPanelHandler);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add fleet panel modifier", e);
            }

            try {
                registerCoreTabListener.invoke(coreTabListenerHandler, refitHandler);
                registerCoreTabListener.invoke(coreTabListenerHandler, fleetPanelHandler);
                listeners.addListener(coreTabListenerHandler, true);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to add core UI listener modifier", e);
            }

            // May need to be permanent if NPC fleets can stay inflated through game loads
            // But this doesn't seem to be the case
            new VariantLookup();
            new PlayerMPHandler();
            new FleetHandler();
            new PlayerFleetHandler();

            MasteredMany.refreshPlayerMasteredCount();

            // reportCoreTabOpened triggers after the variant is cloned for the to-be-selected ship in the refit screen
            // for some reason, which is too late as the UID tags aren't in the clones,
            // so we need to add the mastery handler when the game loads as well
            PlayerFleetHandler.addMasteryHandlerToPlayerFleet();
        } else {
            Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).setHiddenEverywhere(true);
            try {
                CampaignPlugin autofitPlugin = (CampaignPlugin) Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.plugin.SModAutofitCampaignPluginSP"));
                Global.getSector().registerPlugin(autofitPlugin);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add refit autofit plugin", e);
            }
        }

        listeners.addListener(new CuratorFleetHandler(), true);

        new ShipGraveyardSpawner();
        new RecentBattlesTracker();

        registerCommodityTooltipPlugin(listeners);
        registerAICorePlugins();
        new FracturedGammaCoreInterface.IntegrationScript();
        var pseudocorePluginHandler = new BasePseudocorePlugin.Handler();
        FleetSyncListenerHandler.registerListener(pseudocorePluginHandler);
        try {
            registerCoreTabListener.invoke(coreTabListenerHandler, pseudocorePluginHandler);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        FleetSyncListenerHandler.registerListener(new PseudocoreUplinkHullmod());

        if (!Settings.ENABLE_RECENT_BATTLES) {
            IntelManagerAPI intelManager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : new ArrayList<>(intelManager.getIntel(RecentBattlesIntel.class))) {
                intelManager.removeIntel(intel);
            }
        }

        registerCuratorFleetPlugins();
    }

    private static void registerCuratorFleetPlugins() {
        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(ConcealedEntityDefenderPlugin.class)) {
            plugins.addPlugin(new ConcealedEntityDefenderPlugin(), true);
        }
        plugins.addPlugin(new CuratorOfficerPlugin(), true);
    }

    private static void registerCommodityTooltipPlugin(ListenerManagerAPI listeners) {
        listeners.addListener((CommodityTooltipModifier) (info, width, expanded, stack) -> {
            if (stack.getCommodityId() == null) return;
            var plugin = Misc.getAICoreOfficerPlugin(stack.getCommodityId());
            if (!(plugin instanceof PseudocorePlugin)) return;
            plugin.createPersonalitySection(null, info);
        }, true);
    }

    private static void registerAICorePlugins() {
        Global.getSector().registerPlugin(new BaseCampaignPlugin() {
            @Override
            public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
                var plugin = PseudocorePlugin.getPluginForPseudocore(commodityId);
                if (plugin == null) return null;
                return new PluginPick<>(plugin, PickPriority.MOD_SPECIFIC);
            }
        });
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship == null || ship.getCaptain() == null) return null;
        String id = ship.getCaptain().getAICoreId();
        if (id == null) return null;
        var plugin = PseudocorePlugin.getPluginForPseudocore(id);
        if (plugin != null) {
            ShipAIConfig config = new ShipAIConfig();
            config.alwaysStrafeOffensively = true;
            config.backingOffWhileNotVentingAllowed = true;
            config.turnToFaceWithUndamagedArmor = false;
            config.burnDriveIgnoreEnemies = false;
            return new PluginPick<>(Global.getSettings().createDefaultShipAI(ship, config), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    private static final String[] reflectionWhitelist = new String[] {
            "shipmastery.campaign.RefitHandler",
            "shipmastery.campaign.listeners.CoreTabListenerHandler",
            "shipmastery.campaign.FleetPanelHandler",
            "shipmastery.campaign.Initializer",
            "shipmastery.campaign.AutofitPluginSModOption",
            "shipmastery.campaign.graveyard.ClaimsHistoryGetter",
            "shipmastery.campaign.recentbattles.RecentBattlesReplay",
            "shipmastery.campaign.recentbattles.TooltipCreator",
            "shipmastery.plugin.SModAutofitCampaignPlugin",
            "shipmastery.util.FleetMemberTooltipCreator",
            "shipmastery.util.ReflectionUtils",
            "shipmastery.util.ClassRefs",
            "shipmastery.ui",
    };

    public static class ReflectionEnabledClassLoader extends URLClassLoader {

        public ReflectionEnabledClassLoader(URL url, ClassLoader parent) {
            super(new URL[]{url}, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang.reflect")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // Be the defining classloader for all classes in the reflection whitelist
            // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
            // by the system classloader, without the intermediate delegations.
            for (String str : reflectionWhitelist) {
                if (name.startsWith(str)) {
                    return findClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}