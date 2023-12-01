package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import lunalib.lunaSettings.LunaSettings;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.PlayerFleetHandler;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.VariantLookup;

import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        ShipMastery.loadMasteryData();

        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            Settings.SettingsListener settingsListener = new Settings.SettingsListener();
            LunaSettings.addSettingsListener(settingsListener);
            settingsListener.settingsChanged("shipmasterysystem");
        }
        else {
            Settings.loadSettingsFromJson();
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ShipMastery.loadMasteryTable();
        // Time to generate masteries is roughly 1 second per 10,000 ship hull specs
        try {
            ShipMastery.generateAllMasteries();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to generate mastery effects", e);
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        ClassLoader classLoader = makeClassLoader();
        try {
            Object refitModifier = classLoader.loadClass("shipmastery.campaign.RefitHandler").newInstance();
            if (!listeners.hasListenerOfClass(RefitHandler.class)) {
                listeners.addListener(refitModifier, true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add refit tab modifier", e);
        }

        try {
            EveryFrameScript initializer = (EveryFrameScript) classLoader.loadClass("shipmastery.campaign.Initializer").newInstance();
            Global.getSector().addTransientScript(initializer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add initializer", e);
        }

        DeferredActionPlugin deferredActionPlugin = new DeferredActionPlugin();
        Global.getSector().addTransientScript(deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(DeferredActionPlugin.INSTANCE_KEY, deferredActionPlugin);

        VariantLookup variantLookup = new VariantLookup(false);
        Global.getSector().getMemoryWithoutUpdate().set(VariantLookup.INSTANCE_KEY, variantLookup);
        Global.getSector().addTransientListener(variantLookup);

        FleetHandler fleetHandler = new FleetHandler(false);
        listeners.addListener(fleetHandler, true);
        Global.getSector().addTransientListener(fleetHandler);
        listeners.addListener(new PlayerFleetHandler(), true);

        // reportCoreTabOpened triggers after the variant is cloned for the to-be-selected ship in the refit screen
        // for some reason, which is too late as the UID tags aren't in the clones,
        // so we need to add the mastery handler when the game loads as well
        PlayerFleetHandler.addMasteryHandlerToPlayerFleet();
    }

    private static final String[] reflectionWhitelist = new String[] {
            "shipmastery.campaign.RefitHandler",
            "shipmastery.campaign.Initializer",
            "shipmastery.util.ReflectionUtils",
            "shipmastery.util.ClassRefs",
            "shipmastery.ui",
            "shipmastery.stats.logistics"
    };

    private static ReflectionEnabledClassLoader makeClassLoader() {
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        return new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
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