package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import shipmastery.combat.listeners.EndOfCombatListener;

public class StateTracker {
    public static String COMBAT_STATE = "com.fs.starfarer.combat.CombatState";
    private static String currentState = "";

    public static void setState(String newState) {
        if (!currentState.equals(newState)) {
            if (COMBAT_STATE.equals(currentState)) {
                for (EndOfCombatListener listener : Global.getCombatEngine().getListenerManager().getListeners(
                        EndOfCombatListener.class)) {
                    listener.onCombatEnd();
                }
            }
            currentState = newState;
        }
    }
}
