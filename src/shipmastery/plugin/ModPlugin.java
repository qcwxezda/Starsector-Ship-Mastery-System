package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import shipmastery.ShipMastery;
import shipmastery.campaign.*;
import shipmastery.config.LunaLibSettingsListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.procgen.Generator;
import shipmastery.procgen.StationDefenderPlugin;
import shipmastery.util.VariantLookup;

import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    private static String lastSaveId = null;
    public static final String RANDOM_MODE_KEY = "$sms_IsRandomMode";
    private static ReflectionEnabledClassLoader classLoader = null;

    @Override
    public void onApplicationLoad() throws Exception {
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
    public void onGameLoad(boolean newGame) {
        Boolean randomMode = (Boolean) Global.getSector().getPersistentData().get(RANDOM_MODE_KEY);
        // Added to a game for the first time
        if (randomMode == null) {
            randomMode = Settings.ENABLE_RANDOM_MODE;
            Global.getSector().getPersistentData().put(RANDOM_MODE_KEY, randomMode);
            Generator.generate();
        }

        ShipMastery.loadMasteryTable();
        // Time to generate masteries is roughly 1 second per 10,000 ship hull specs
        String id = Global.getSector().getPlayerPerson().getId();
        try {
            if (!id.equals(lastSaveId)) {
                lastSaveId = id;
                ShipMastery.initMasteries(randomMode);
                ShipMastery.generateAndApplyMasteries(true);
            } else {
                ShipMastery.generateAndApplyMasteries(false);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        ClassLoader classLoader = getClassLoader();
        try {
            Object refitModifier = classLoader.loadClass("shipmastery.campaign.RefitHandler").newInstance();
            listeners.addListener(refitModifier, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add refit tab modifier", e);
        }

        try {
            EveryFrameScript initializer = (EveryFrameScript) classLoader.loadClass("shipmastery.campaign.Initializer").newInstance();
            Global.getSector().addTransientScript(initializer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add initializer", e);
        }
//
//        try {
//            // Note: getting the RefitHandler listeners requires that the calling class and RefitHandler are using the
//            // same classloader, hence using the custom classloader here
//            SModAutofitCampaignPlugin plugin = (SModAutofitCampaignPlugin) classLoader.loadClass("shipmastery.plugin.SModAutofitCampaignPlugin").newInstance();
//            Global.getSector().registerPlugin(plugin);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to add campaign plugin", e);
//        }

        DeferredActionPlugin deferredActionPlugin = new DeferredActionPlugin();
        Global.getSector().addTransientScript(deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(DeferredActionPlugin.INSTANCE_KEY, deferredActionPlugin);

        PlayerMPHandler xpTracker = new PlayerMPHandler(false);
        Global.getSector().addTransientScript(xpTracker);
        Global.getSector().addTransientListener(xpTracker);

        VariantLookup variantLookup = new VariantLookup(false);
        Global.getSector().getMemoryWithoutUpdate().set(VariantLookup.INSTANCE_KEY, variantLookup);
        Global.getSector().addTransientListener(variantLookup);
        ShipGraveyardSpawner graveyardSpawner = new ShipGraveyardSpawner();
        Global.getSector().addTransientListener(graveyardSpawner);
        Global.getSector().addTransientScript(graveyardSpawner);
        listeners.addListener(graveyardSpawner);

        FleetHandler fleetHandler = new FleetHandler(false);
        listeners.addListener(fleetHandler, true);
        Global.getSector().addTransientListener(fleetHandler);
        listeners.addListener(new PlayerFleetHandler(), true);

        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(StationDefenderPlugin.class)) {
            plugins.addPlugin(new StationDefenderPlugin(), true);
        }

        // reportCoreTabOpened triggers after the variant is cloned for the to-be-selected ship in the refit screen
        // for some reason, which is too late as the UID tags aren't in the clones,
        // so we need to add the mastery handler when the game loads as well
        PlayerFleetHandler.addMasteryHandlerToPlayerFleet();
    }

    private static final String[] reflectionWhitelist = new String[] {
            "shipmastery.campaign.RefitHandler",
            "shipmastery.campaign.Initializer",
            "shipmastery.campaign.CoreAutofitPluginExt",
            "shipmastery.plugin.SModAutofitCampaignPlugin",
            "shipmastery.util.ReflectionUtils",
            "shipmastery.util.ClassRefs",
            "shipmastery.ui",
    };

    public static ReflectionEnabledClassLoader getClassLoader() {
        if (classLoader != null) return classLoader;
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        return classLoader = new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
    }

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