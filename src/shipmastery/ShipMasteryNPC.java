package shipmastery;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.*;

public class ShipMasteryNPC extends BaseCampaignEventListener {

    /**
     * NPC fleets with commanders below this level won't generate masteries
     */
    public static final int MIN_COMMANDER_LEVEL = 5;

    /**
     * Fleet commander id > ship hull spec id -> mastery level -> is option 2?
     * /* No point caching multiple fleet ids as they change every time the dialog is closed and reopened
     */
    public static final Map<String, Map<String, NavigableMap<Integer, Boolean>>> CACHED_NPC_FLEET_MASTERIES =
            new LinkedHashMap<String, Map<String, NavigableMap<Integer, Boolean>>>() {
                private static final int MAX_ENTRIES = 20;
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, Map<String, NavigableMap<Integer, Boolean>>> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };

    public ShipMasteryNPC(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (fleet.getCommander() == null) return;
        CACHED_NPC_FLEET_MASTERIES.remove(fleet.getCommander().getId());
    }

    /** Can be called for player commander. In that case it's the same as calling {@code ShipMastery.getActiveMasteriesCopy}. */
    public static NavigableMap<Integer, Boolean> getActiveMasteriesForCommander(final PersonAPI commander,
                                                                                ShipHullSpecAPI spec) {
        if (commander == null) return new TreeMap<>();
        if (commander.isPlayer()) return ShipMastery.getPlayerActiveMasteriesCopy(spec);

        if (commander.getStats().getLevel() < MIN_COMMANDER_LEVEL) return new TreeMap<>();
        spec = Utils.getRestoredHullSpec(spec);
        String commanderId = commander.getId();
        String specId = spec.getHullId();

        Map<String, NavigableMap<Integer, Boolean>> subMap = CACHED_NPC_FLEET_MASTERIES.get(commanderId);
        if (subMap == null) {
            subMap = new HashMap<>();
            CACHED_NPC_FLEET_MASTERIES.put(commanderId, subMap);
        }
        if (subMap.containsKey(specId)) {
            return subMap.get(specId);
        }

        // Not cached, need to regenerate
        int masteryLevel = Math.min(randIntUpToCommanderLevel(spec, commander, 0),
                                    randIntUpToCommanderLevel(spec, commander, 1));
        masteryLevel = Math.min(masteryLevel, randIntUpToCommanderLevel(spec, commander, 2));
        masteryLevel = Math.min(masteryLevel, ShipMastery.getPlayerMaxMastery(spec));
        NavigableMap<Integer, Boolean> levelsMap = new TreeMap<>();
        for (int i = 1; i <= masteryLevel; i++) {
            List<MasteryEffect> effectsOption2 = ShipMastery.getMasteryEffects(spec, i, true);
            if (effectsOption2.isEmpty()) {
                levelsMap.put(i, false);
            } else {
                levelsMap.put(i, randIntUpToCommanderLevel(spec, commander, 3) % 2 == 0);
            }
        }
        subMap.put(specId, levelsMap);

        // Once NPC mastery levels have been generated for the first time, activate the corresponding masteries
        MasteryUtils.applyAllActiveMasteryEffects(commander, spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect) {
                effect.onActivate(commander);
            }
        });

        return levelsMap;
    }

    /**
     * NPC fleet ids are regenerated each time they are interacted with so can't use those
     */
    public static int randIntUpToCommanderLevel(ShipHullSpecAPI spec, PersonAPI commander, int seed) {
        return (("" + seed + commander.getId() + spec.getHullId() + "___").hashCode() & 0x7fffffff) % (commander.getStats().getLevel() + 1);
    }
}
