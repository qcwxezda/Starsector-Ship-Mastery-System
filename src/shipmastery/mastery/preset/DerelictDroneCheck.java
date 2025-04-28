package shipmastery.mastery.preset;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

@SuppressWarnings("unused")
public class DerelictDroneCheck implements PresetCheckScript {
    @Override
    public float computeScore(ShipHullSpecAPI spec) {
        if (spec.hasTag("derelict") && spec.isBuiltInMod(HullMods.AUTOMATED)) return 3f;
        return 0f;
    }
}
