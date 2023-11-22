package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import shipmastery.ShipMasteryNPC;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FlagshipTracker {
    ShipAPI trackedPlayerFlagship;
    ShipAPI trackedEnemyFlagship;

    List<EffectActivationRecord> playerEffects = new ArrayList<>();
    List<EffectActivationRecord> enemyEffects = new ArrayList<>();

    void advance(CombatEngineAPI engine) {
        // Only track in campaign...
        if (Global.getSector() == null) return;

        final ShipAPI playerFlagship = engine.getPlayerShip();
        CombatFleetManagerAPI enemyFleetManager = engine.getFleetManager(FleetSide.ENEMY);
        ShipAPI enemyFlagship = enemyFleetManager.getShipFor(enemyFleetManager.getFleetCommander());

        if (playerFlagship != trackedPlayerFlagship || enemyFlagship != trackedEnemyFlagship) {
            if (playerFlagship != trackedPlayerFlagship) {
                onFlagshipChanged(playerFlagship, playerEffects);
                trackedPlayerFlagship = playerFlagship;
            }
            if (enemyFlagship != trackedEnemyFlagship) {
                onFlagshipChanged(enemyFlagship, enemyEffects);
                trackedEnemyFlagship = enemyFlagship;
            }
        }
    }

    void onFlagshipChanged(final ShipAPI newFlagship, final List<EffectActivationRecord> activationList) {
        for (int i = activationList.size() - 1; i >= 0; i--) {
            EffectActivationRecord record = activationList.get(i);
            record.effect.onFlagshipStatusLost(record.ship);
        }
        activationList.clear();
        MasteryUtils.applyAllActiveMasteryEffects(
                newFlagship.getFleetMember() == null ? null : newFlagship.getFleetMember().getFleetCommander(),
                newFlagship.getHullSpec(), new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        effect.onFlagshipStatusGained(newFlagship);
                        activationList.add(new EffectActivationRecord(effect, newFlagship));
                    }
                });
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
