package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;

import java.util.HashMap;
import java.util.Map;

public class CommanderLookup {
    private final Map<String, PersonAPI> idToCommander = new HashMap<>();
    public static final String INSTANCE_KEY = "$shipmastery_CommanderLookup";

    public static void addCommander(String id, PersonAPI commander) {
        getInstance().idToCommander.put(id, commander);
    }

    public static PersonAPI getCommander(String id) {
        return getInstance().idToCommander.get(id);
    }

    public static CommanderLookup getInstance() {
        return (CommanderLookup) Global.getSector().getMemory().get(INSTANCE_KEY);
    }
}
