package shipmastery.campaign.listeners;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;

import java.util.ArrayList;
import java.util.List;

public class PlayerGainedMPListenerHandler {
    private static PlayerGainedMPListenerHandler instance;
    private final List<PlayerGainedMPListener> listeners = new ArrayList<>();

    public static PlayerGainedMPListenerHandler getInstance() {
        if (instance == null) instance = new PlayerGainedMPListenerHandler();
        return instance;
    }

    public static float modifyPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source) {
        for (var listener : getInstance().listeners) {
            amount = listener.modifyPlayerMPGain(spec, amount, source);
        }
        return amount;
    }

    public static void reportPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source) {
        for (var listener : getInstance().listeners) {
            listener.reportPlayerMPGain(spec, amount, source);
        }
    }

    public static void registerListener(PlayerGainedMPListener listener) {
        getInstance().listeners.add(listener);
    }

    public static void clearListeners() {
        getInstance().listeners.clear();
    }
}
