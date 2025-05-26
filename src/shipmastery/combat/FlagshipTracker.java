package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.combat.listeners.EndOfCombatListener;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.MasteryUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FlagshipTracker implements EndOfCombatListener {
    final Map<PersonAPI, ShipAPI> commanderToShipMap = new HashMap<>();
    final Map<PersonAPI, List<EffectActivationRecord>> activationRecordMap = new HashMap<>();

    void advance(CombatEngineAPI engine) {
        // Don't track in title screen...
        if (Global.getSector() == null) return;

        CombatFleetManagerAPI[] fleetManagers = new CombatFleetManagerAPI[] {
                engine.getFleetManager(0),
                engine.getFleetManager(1)
        };

        // Double for loop in advance looks bad, but |fleetManagers| = 2 and the number of commanders on a side
        // is usually just one.
        for (CombatFleetManagerAPI manager : fleetManagers) {
            for (PersonAPI commander : manager.getAllFleetCommanders()) {
                ShipAPI oldFlagship = commanderToShipMap.get(commander);
                ShipAPI newFlagship = commander.isPlayer() ? engine.getPlayerShip() : manager.getShipFor(commander);
                if (oldFlagship != newFlagship) {
                    onFlagshipChanged(commander, newFlagship);
                }
            }
        }
    }

    void onFlagshipChanged(final PersonAPI commander, final ShipAPI newFlagship) {
        //System.out.println("Flagship changed for " + commander.getNameString() + ", to " + newFlagship);
        commanderToShipMap.put(commander, newFlagship);
        List<EffectActivationRecord> effects = activationRecordMap.get(commander);
        if (effects != null) {
            for (int i = effects.size() - 1; i >= 0; i--) {
                EffectActivationRecord record = effects.get(i);
                applyFlagshipStatusChange(record.effect, commander, record.ship.getMutableStats(), record.ship, false);
            }
        }
        activationRecordMap.remove(commander);
        if (newFlagship == null) return;
        final List<EffectActivationRecord> newEffects = new ArrayList<>();
        MasteryUtils.applyAllActiveMasteryEffects(
                commander,
                newFlagship.getHullSpec(), effect -> {
                    applyFlagshipStatusChange(effect, commander, newFlagship.getMutableStats(), newFlagship, true);
                    newEffects.add(new EffectActivationRecord(effect, newFlagship));
                });
        activationRecordMap.put(commander, newEffects);
    }

    void applyFlagshipStatusChange(MasteryEffect effect, PersonAPI commander, MutableShipStatsAPI stats, ShipAPI ship, boolean isGained) {
        if (isGained) {
            effect.onFlagshipStatusGained(commander, stats, ship);
        }
        else {
            effect.onFlagshipStatusLost(commander, stats, ship);
        }
        if (!effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
            for (ShipAPI module : ship.getChildModulesCopy()) {
                applyFlagshipStatusChange(effect, commander, module.getMutableStats(), module, isGained);
            }
        }
    }

    @Override
    public void onCombatEnd() {
        // This guarantees that for every onFlagshipStatusGained (with non-null ship), there is a
        // onFlagshipStatusLost
        for (PersonAPI person : new HashSet<>(activationRecordMap.keySet())) {
            onFlagshipChanged(person, null);
        }
    }

    record EffectActivationRecord(MasteryEffect effect, ShipAPI ship) {}
}
