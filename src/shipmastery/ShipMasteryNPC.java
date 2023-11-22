package shipmastery;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Pair;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Utils;

import java.util.*;

public abstract class ShipMasteryNPC {

    /** NPC fleets with commanders below this level won't generate masteries */
    public static final int MIN_COMMANDER_LEVEL = 4;

    /** Fleet commander id > ship hull spec id -> mastery level -> is option 2?
    /* No point caching multiple fleet ids as they change every time the dialog is closed and reopened */
    public static Map<Pair<String, ShipHullSpecAPI>, NavigableMap<Integer, Boolean>> CACHED_NPC_FLEET_MASTERIES = new HashMap<>();

    public static void generateMasteryLevelsForNPCFleet(List<CampaignFleetAPI> nonPlayerSide) {
        CACHED_NPC_FLEET_MASTERIES.clear();

        for (CampaignFleetAPI fleet : nonPlayerSide) {
            PersonAPI commander = fleet.getCommander();
            if (commander == null) continue;
            if (commander.getStats() == null) continue;
            if (commander.getStats().getLevel() < MIN_COMMANDER_LEVEL) continue;

            for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
                Pair<String, ShipHullSpecAPI> key = new Pair<>(commander.getId(), spec);
                if (CACHED_NPC_FLEET_MASTERIES.containsKey(key)) continue;
                if (!ShipMastery.hasMasteryData(spec)) {
                    try {
                        ShipMastery.generateMasteries(spec);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Failed to generate mastery effects from assignments for " + spec.getHullName() +"!", e);
                    }
                }
                int masteryLevel = Math.min(randIntUpToCommanderLevel(spec, commander, 0), randIntUpToCommanderLevel(spec, commander, 1));
                masteryLevel = Math.min(masteryLevel, ShipMastery.getMaxMastery(spec));
                NavigableMap<Integer, Boolean> levelsMap = new TreeMap<>();
                for (int i = 1; i <= masteryLevel; i++) {
                    List<MasteryEffect> effectsOption2 = ShipMastery.getMasteryEffects(spec, i, true);
                    if (effectsOption2.isEmpty()) {
                        levelsMap.put(i, false);
                    }
                    else {
                        levelsMap.put(i, randIntUpToCommanderLevel(spec, commander, 2) % 2 == 0);
                    }
                }
                CACHED_NPC_FLEET_MASTERIES.put(key, levelsMap);
            }
        }
    }

    /** NPC fleet ids are regenerated each time they are interacted with so can't use those */
    public static int randIntUpToCommanderLevel(ShipHullSpecAPI spec, PersonAPI commander, int seed) {
        return (commander.getId() + spec.getHullId() + seed).hashCode() % (commander.getStats().getLevel() + 1);
    }
}
