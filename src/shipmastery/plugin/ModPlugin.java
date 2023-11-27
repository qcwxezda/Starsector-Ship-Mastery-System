package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import shipmastery.ShipMastery;
import shipmastery.ShipMasteryNPC;
import shipmastery.campaign.RefitHandler;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.CommanderLookup;

import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        ShipMastery.loadMasteryData();
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
        try {
            EveryFrameScript refitModifier = (EveryFrameScript) getClassLoader().loadClass("shipmastery.campaign.RefitHandler").newInstance();
            Global.getSector().addTransientScript(refitModifier);
            if (!listeners.hasListenerOfClass(RefitHandler.class)) {
                listeners.addListener(refitModifier, true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add refit tab modifier", e);
        }

        DeferredActionPlugin deferredActionPlugin = new DeferredActionPlugin();
        Global.getSector().addTransientScript(deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(DeferredActionPlugin.INSTANCE_KEY, deferredActionPlugin);
        Global.getSector().getMemoryWithoutUpdate().set(CommanderLookup.INSTANCE_KEY, new CommanderLookup());
        Global.getSector().addTransientListener(new ShipMasteryNPC(false));
        ShipMasteryNPC.CACHED_NPC_FLEET_MASTERIES.clear();
    }

    private static final String[] reflectionWhitelist = new String[] {
            "shipmastery.campaign.RefitHandler",
            "shipmastery.util.ReflectionUtils",
            "shipmastery.util.ClassRefs",
            "shipmastery.ui",
            "shipmastery.stats.logistics"
    };

    private static ReflectionEnabledClassLoader getClassLoader() {
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