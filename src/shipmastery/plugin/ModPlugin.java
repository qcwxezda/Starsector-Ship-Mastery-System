package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityTooltipModifier;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.PlayerFleetHandler;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.campaign.graveyard.InsuranceFraudDetector;
import shipmastery.campaign.graveyard.ShipGraveyardSpawner;
import shipmastery.campaign.items.AmorphousCorePlugin;
import shipmastery.campaign.items.KnowledgeCorePlugin;
import shipmastery.campaign.items.SubknowledgeCorePlugin;
import shipmastery.campaign.recentbattles.RecentBattlesIntel;
import shipmastery.campaign.recentbattles.RecentBattlesTracker;
import shipmastery.campaign.skills.CyberneticAugmentation;
import shipmastery.combat.CombatListenerManager;
import shipmastery.config.LunaLibSettingsListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.procgen.Generator;
import shipmastery.procgen.StationDefenderPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    private static String lastSaveId = null;
    private static final int originalMaxPermaMods = Global.getSettings().getInt("maxPermanentHullmods");
    public static final String RANDOM_MODE_KEY = "$sms_IsRandomMode";
    public static final String GENERATION_SEED_KEY = "$sms_MasteryGenerationSeed";
    public static final ReflectionEnabledClassLoader classLoader;

    static {
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        classLoader = new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        Utils.init();
        ShipMastery.loadMasteries();
        ShipMastery.loadStats();
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            LunaLibSettingsListener.init();
        }
        else {
            Settings.loadSettingsFromJson();
        }
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
        Generator.generate();

        // Not transient in case player saves while action queue isn't empty
        DeferredActionPlugin deferredActionPlugin = new DeferredActionPlugin();
        Global.getSector().addScript(deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(DeferredActionPlugin.INSTANCE_KEY, deferredActionPlugin);
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

        if (!Settings.DISABLE_MAIN_FEATURES) {
            Misc.MAX_PERMA_MODS = 0;
            Global.getSettings().setFloat("maxPermanentHullmods", 0f);
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
            catch (ClassCastException e) {
                throw new RuntimeException("This version of Ship Mastery System is not save-compatible with versions prior to 0.15.0.", e);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                Object refitModifier = Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.campaign.RefitHandler"));
                listeners.addListener(refitModifier, true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add refit tab modifier", e);
            }

            // May need to be permanent if NPC fleets can stay inflated through game loads
            // But this doesn't seem to be the case
            VariantLookup variantLookup = new VariantLookup();
            Global.getSector().addTransientListener(variantLookup);
            // Temporary, remove deprecated object if it still exists
            Global.getSector().getMemoryWithoutUpdate().unset("$shipmastery_VariantLookup");

            PlayerMPHandler xpTracker = new PlayerMPHandler();
            Global.getSector().addTransientScript(xpTracker);
            Global.getSector().addTransientListener(xpTracker);

            ShipGraveyardSpawner graveyardSpawner = new ShipGraveyardSpawner();
            Global.getSector().addTransientListener(graveyardSpawner);
            Global.getSector().addTransientScript(graveyardSpawner);
            listeners.addListener(graveyardSpawner, true);

            FleetHandler fleetHandler = new FleetHandler();
            listeners.addListener(fleetHandler, true);
            Global.getSector().addTransientListener(fleetHandler);
            listeners.addListener(new PlayerFleetHandler(), true);


            CyberneticAugmentation.refreshPlayerMasteredCount();

            // reportCoreTabOpened triggers after the variant is cloned for the to-be-selected ship in the refit screen
            // for some reason, which is too late as the UID tags aren't in the clones,
            // so we need to add the mastery handler when the game loads as well
            PlayerFleetHandler.addMasteryHandlerToPlayerFleet();
        } else {
            Misc.MAX_PERMA_MODS = originalMaxPermaMods;
            Global.getSettings().setFloat("maxPermanentHullmods", (float) originalMaxPermaMods);
            Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).setHiddenEverywhere(true);
            try {
                CampaignPlugin autofitPlugin = (CampaignPlugin) Utils.instantiateClassNoParams(classLoader.loadClass("shipmastery.plugin.SModAutofitCampaignPluginSP"));
                Global.getSector().registerPlugin(autofitPlugin);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add refit autofit plugin", e);
            }
        }

        RecentBattlesTracker recentBattlesTracker = new RecentBattlesTracker();
        Global.getSector().addTransientListener(recentBattlesTracker);
        listeners.addListener(recentBattlesTracker, true);

        registerCommodityTooltipPlugin(listeners);
        registerAICorePlugin();

        if (!Settings.ENABLE_RECENT_BATTLES) {
            IntelManagerAPI intelManager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : new ArrayList<>(intelManager.getIntel(RecentBattlesIntel.class))) {
                intelManager.removeIntel(intel);
            }
        }

        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(StationDefenderPlugin.class)) {
            plugins.addPlugin(new StationDefenderPlugin(), true);
        }
    }

    private static void registerCommodityTooltipPlugin(ListenerManagerAPI listeners) {
        listeners.addListener((CommodityTooltipModifier) (info, width, expanded, stack) -> {
            if (stack.getCommodityId() == null) return;
            String personalityName = Global.getSettings().getPersonaltySpec(KnowledgeCorePlugin.DEFAULT_PERSONALITY_ID).getDisplayName();
            switch (stack.getCommodityId()) {
                case "sms_amorphous_core" -> KnowledgeCorePlugin.createPersonalitySection(
                        info,
                        Strings.Items.amorphousCorePersonalityHeading,
                        String.format(Strings.Items.amorphousCorePersonalityText, stack.getDisplayName()),
                        AmorphousCorePlugin.MIN_LEVEL,
                        AmorphousCorePlugin.MAX_DP_MULT,
                        personalityName);
                case "sms_knowledge_core" -> KnowledgeCorePlugin.createPersonalitySection(
                        info,
                        Strings.Items.knowledgeCorePersonalityHeading,
                        String.format(Strings.Items.knowledgeCorePersonalityText, stack.getDisplayName()),
                        KnowledgeCorePlugin.MAX_LEVEL,
                        KnowledgeCorePlugin.DP_MULT,
                        personalityName);
                case "sms_subknowledge_core" -> KnowledgeCorePlugin.createPersonalitySection(
                        info,
                        Strings.Items.knowledgeCorePersonalityHeading,
                        String.format(Strings.Items.knowledgeCorePersonalityText, stack.getDisplayName()),
                        SubknowledgeCorePlugin.MAX_LEVEL,
                        SubknowledgeCorePlugin.DP_MULT,
                        personalityName);
            }
        }, true);
    }

    private static void registerAICorePlugin() {
        Global.getSector().registerPlugin(new BaseCampaignPlugin() {
            @Override
            public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
                if ("sms_amorphous_core".equals(commodityId)) {
                    return new PluginPick<>(new AmorphousCorePlugin(), PickPriority.MOD_SPECIFIC);
                }
                else if ("sms_knowledge_core".equals(commodityId)) {
                    return new PluginPick<>(new KnowledgeCorePlugin(), PickPriority.MOD_SPECIFIC);
                }
                else if ("sms_subknowledge_core".equals(commodityId)) {
                    return new PluginPick<>(new SubknowledgeCorePlugin(), PickPriority.MOD_SPECIFIC);
                }
                return null;
            }
        });
    }

    private static final String[] reflectionWhitelist = new String[] {
            "shipmastery.campaign.RefitHandler",
            "shipmastery.campaign.Initializer",
            "shipmastery.campaign.AutofitPluginSModOption",
            "shipmastery.campaign.graveyard.ClaimsHistoryGetter",
            "shipmastery.campaign.recentbattles.RecentBattlesReplay",
            "shipmastery.campaign.recentbattles.TooltipCreator",
            "shipmastery.plugin.SModAutofitCampaignPlugin",
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