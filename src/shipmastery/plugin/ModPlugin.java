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
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.PlayerFleetHandler;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.campaign.RefitHandler;
import shipmastery.campaign.graveyard.InsuranceFraudDetector;
import shipmastery.campaign.graveyard.ShipGraveyardSpawner;
import shipmastery.campaign.items.AlphaKCorePlugin;
import shipmastery.campaign.items.BetaKCorePlugin;
import shipmastery.campaign.items.FracturedGammaCorePlugin;
import shipmastery.campaign.items.KCoreInterface;
import shipmastery.campaign.items.GammaKCorePlugin;
import shipmastery.campaign.recentbattles.RecentBattlesIntel;
import shipmastery.campaign.recentbattles.RecentBattlesTracker;
import shipmastery.campaign.skills.CyberneticAugmentation;
import shipmastery.combat.CombatListenerManager;
import shipmastery.config.LunaLibSettingsListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.procgen.TestGenerator;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        initializeCuratorFaction();
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
    public void onAboutToLinkCodexEntries() {
        CodexDataV2.makeRelated(
                CodexDataV2.getSkillEntryId("sms_shared_knowledge"),
                CodexDataV2.getItemEntryId("sms_construct"),
                CodexDataV2.getCommodityEntryId("sms_gamma_k_core"),
                CodexDataV2.getCommodityEntryId("sms_beta_k_core"),
                CodexDataV2.getCommodityEntryId("sms_alpha_k_core"));
    }

    private void initializeCuratorFaction() {
        FactionSpecAPI thisFaction = Global.getSettings().getFactionSpec("sms_curator");
        thisFaction.getKnownHullMods().addAll(Global.getSettings().getAllHullModSpecs().stream().filter(
                spec -> !spec.hasTag(Tags.RESTRICTED)
                        && !spec.hasTag(Tags.HIDE_IN_CODEX)
                        && !spec.hasTag(Tags.NO_DROP)
                        && !spec.hasTag(Tags.HULLMOD_NO_DROP_SALVAGE)
        ).map(HullModSpecAPI::getId).toList());

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
        new TestGenerator().generate();
        Global.getSector().getPlayerFaction().setRelationship("sms_curator", RepLevel.HOSTILE);

        // Not transient in case player saves while action queue isn't empty
        // Possibly broken though !! - can't save lambdas
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
            Global.getSector().addTransientScript(fleetHandler);
            listeners.addListener(new PlayerFleetHandler(), true);
            FleetHandler.NPC_MASTERY_CACHE.clear();

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
        registerAICorePlugins();

        if (!Settings.ENABLE_RECENT_BATTLES) {
            IntelManagerAPI intelManager = Global.getSector().getIntelManager();
            for (IntelInfoPlugin intel : new ArrayList<>(intelManager.getIntel(RecentBattlesIntel.class))) {
                intelManager.removeIntel(intel);
            }
        }

        registerCuratorFleetPlugins();
    }

    private static void registerCuratorFleetPlugins(){
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
            if (!(plugin instanceof KCoreInterface)) return;
            plugin.createPersonalitySection(null, info);
        }, true);
    }

    private static void registerAICorePlugins() {
        Global.getSector().registerPlugin(new BaseCampaignPlugin() {
            @Override
            public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
                return switch (commodityId) {
                    case "sms_alpha_k_core" -> new PluginPick<>(new AlphaKCorePlugin(), PickPriority.MOD_SPECIFIC);
                    case "sms_beta_k_core" -> new PluginPick<>(new BetaKCorePlugin(), PickPriority.MOD_SPECIFIC);
                    case "sms_gamma_k_core" -> new PluginPick<>(new GammaKCorePlugin(), PickPriority.MOD_SPECIFIC);
                    case "sms_fractured_gamma_core" -> new PluginPick<>(new FracturedGammaCorePlugin(), PickPriority.MOD_SPECIFIC);
                    default -> null;
                };
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