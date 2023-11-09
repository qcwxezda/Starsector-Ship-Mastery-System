package shipmastery.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.util.Misc;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    @Override
    public void onGameLoad(boolean newGame) {
        Misc.MAX_PERMA_MODS = 0;
    }
}