package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class NoCrew extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null
                || stats.getFleetMember().getFleetData() == null
                || stats.getFleetMember().getFleetData().getCommander() == null
                || stats.getFleetMember().getFleetData().getCommander().isPlayer()) return;
        stats.getMinCrewMod().modifyMult(id, 0f);
    }
}
