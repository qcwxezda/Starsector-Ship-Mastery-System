package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

@SuppressWarnings("unused")
public class DwellerCheck implements PresetCheckScript {
    @Override
    public float computeScore(ShipHullSpecAPI spec) {
        return spec.hasTag("dweller") ? 100f : 0f;
    }
}
