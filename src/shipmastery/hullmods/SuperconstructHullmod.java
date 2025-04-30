package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.config.Settings;

import java.awt.Color;

public class SuperconstructHullmod extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return "" + 1;
    }

    @Override
    public Color getBorderColor() {
        return Settings.MASTERY_COLOR;
    }
}
