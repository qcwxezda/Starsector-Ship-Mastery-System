package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

@SuppressWarnings("unused")
public class LogisticCrewCheck implements PresetCheckScript {
    @Override
    public float computeScore(ShipHullSpecAPI spec) {
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.LINER)) return 1f;
        return 0;
    }
}
