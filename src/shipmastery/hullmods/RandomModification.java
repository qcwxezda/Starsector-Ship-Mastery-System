package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.config.Settings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomModification extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null) return;
        Random random = new Random(stats.getFleetMember().getId().hashCode());
        List<Float> randoms = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            randoms.add(random.nextFloat() * 0.2f - 0.1f);
        }
        stats.getTimeMult().modifyMult(id, 1f + randoms.get(0));
        stats.getMaxSpeed().modifyMult(id, 1f + randoms.get(1));
        stats.getMaxTurnRate().modifyMult(id, 1f + randoms.get(1));
        stats.getAcceleration().modifyMult(id, 1f + randoms.get(1));
        stats.getDeceleration().modifyMult(id, 1f + randoms.get(1));
        stats.getTurnAcceleration().modifyMult(id, 1f + randoms.get(1));
        stats.getFluxDissipation().modifyMult(id, 1f + randoms.get(2));
        stats.getFluxCapacity().modifyMult(id, 1f + randoms.get(3));
        stats.getShieldDamageTakenMult().modifyMult(id, 1f + randoms.get(4));
        stats.getHullBonus().modifyMult(id, 1f + randoms.get(5));
        stats.getArmorBonus().modifyMult(id, 1f + randoms.get(6));
        stats.getSensorProfile().modifyMult(id, 1f + randoms.get(7));
        stats.getSensorStrength().modifyMult(id, 1f + randoms.get(8));
        stats.getPeakCRDuration().modifyMult(id, 1f + randoms.get(9));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0 && ship.getFleetMemberId() != null) {
            Random random = new Random(ship.getFleetMemberId().hashCode());
            return Utils.asFloatTwoDecimals(random.nextFloat() * 0.2f + 0.9f);
        }
        return null;
    }

    @Override
    public Color getBorderColor() {
        return Settings.MASTERY_COLOR;
    }

    @Override
    public Color getNameColor() {
        return Settings.MASTERY_COLOR;
    }
}
