package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

@SuppressWarnings("unused")
public class ThreatCheck implements PresetCheckScript {
    @Override
    public float computeScore(ShipHullSpecAPI spec) {
        return spec.hasTag("threat") ? 100f : 0f;
    }
}
