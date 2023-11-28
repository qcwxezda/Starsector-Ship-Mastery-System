package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommanderLookup {
    private final Map<String, PersonAPI> idToCommander = new HashMap<>();
    public static final String INSTANCE_KEY = "$shipmastery_CommanderLookup";
    public static final String COMMANDER_ID_TAG = "shipmastery_commander_id_";

    public static void addCommander(String id, PersonAPI commander) {
        getInstance().idToCommander.put(id, commander);
    }

    public static PersonAPI getCommander(String id) {
        return getInstance().idToCommander.get(id);
    }

    public static PersonAPI getCommander(ShipVariantAPI variant) {
        if (variant == null) return null;
        for (String tag : variant.getTags()) {
            if (tag.startsWith(COMMANDER_ID_TAG)) {
                return getCommander(tag.substring(COMMANDER_ID_TAG.length()));
            }
        }
        return null;
    }

    public static void tagVariant(ShipVariantAPI variant, PersonAPI commander) {
        for (Iterator<String> iterator = variant.getTags().iterator(); iterator.hasNext(); ) {
            String tag = iterator.next();
            if (tag.startsWith(COMMANDER_ID_TAG)) {
                iterator.remove();
            }
        }
        variant.addTag(COMMANDER_ID_TAG + commander.getId());
    }

    public static CommanderLookup getInstance() {
        return (CommanderLookup) Global.getSector().getMemoryWithoutUpdate().get(INSTANCE_KEY);
    }
}
