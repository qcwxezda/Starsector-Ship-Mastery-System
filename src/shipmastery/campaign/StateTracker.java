package shipmastery.campaign;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.jetbrains.annotations.Nullable;
import shipmastery.combat.listeners.EndOfCombatListener;

public class StateTracker {
    public static final String COMBAT_STATE = "com.fs.starfarer.combat.CombatState";
    private static String currentState = "";
    private static CombatEngineAPI lastKnownEngine;

    public static void setState(String newState, @Nullable CombatEngineAPI engine) {
        if (engine != null) {
            lastKnownEngine = engine;
        }
        if (!currentState.equals(newState)) {
            if (COMBAT_STATE.equals(currentState) && lastKnownEngine != null) {
                for (EndOfCombatListener listener : lastKnownEngine.getListenerManager().getListeners(
                        EndOfCombatListener.class)) {
                    listener.onCombatEnd();
                    lastKnownEngine = null;
                }
            }
            currentState = newState;
        }
    }
}
