package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

@SuppressWarnings("unused")
public class AutomatedCheck implements PresetCheckScript {
    @Override
    public float computeScore(ShipHullSpecAPI spec) {
        if (spec.isBuiltInMod(HullMods.AUTOMATED)) return 1f;
        return 0f;
    }
}
