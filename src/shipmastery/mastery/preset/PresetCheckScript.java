package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

public interface PresetCheckScript {
    /** Score > 0 means the preset is eligible for the given spec. The preset with the highest score will be selected for each ship hull spec. */
    float computeScore(ShipHullSpecAPI spec);
}
