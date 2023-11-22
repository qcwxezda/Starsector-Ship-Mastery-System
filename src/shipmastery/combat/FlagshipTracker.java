package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlagshipTracker {
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
                ShipAPI newFlagship = manager.getShipFor(commander);
                if (oldFlagship != newFlagship) {
                    onFlagshipChanged(commander, newFlagship);
                    commanderToShipMap.put(commander, newFlagship);
                    System.out.println("Flagship changed for " + commander.getNameString() + ", to " + newFlagship);
                }
            }
        }
    }

    void onFlagshipChanged(final PersonAPI commander, final ShipAPI newFlagship) {
        List<EffectActivationRecord> effects = activationRecordMap.get(commander);
        if (effects != null) {
            for (int i = effects.size() - 1; i >= 0; i--) {
                EffectActivationRecord record = effects.get(i);
                record.effect.onFlagshipStatusLost(commander, record.ship.getMutableStats(), record.ship);
            }
        }
        activationRecordMap.remove(commander);
        if (newFlagship == null) return;
        final List<EffectActivationRecord> newEffects = new ArrayList<>();
        MasteryUtils.applyAllActiveMasteryEffects(
                newFlagship.getFleetMember() == null ? null : newFlagship.getFleetMember().getFleetCommander(),
                newFlagship.getHullSpec(), new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        effect.onFlagshipStatusGained(commander, newFlagship.getMutableStats(), newFlagship);
                        newEffects.add(new EffectActivationRecord(effect, newFlagship));
                    }
                });
        activationRecordMap.put(commander, newEffects);
    }

    static class EffectActivationRecord {
        MasteryEffect effect;
        ShipAPI ship;

        EffectActivationRecord(MasteryEffect effect, ShipAPI ship) {
            this.effect = effect;
            this.ship = ship;
        }
    }
}
