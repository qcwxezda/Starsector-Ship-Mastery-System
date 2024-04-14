package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.config.Settings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Random;

public class RandomModification extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null) return;
        Random random = new Random(stats.getFleetMember().getId().hashCode());
        stats.getTimeMult().modifyMult(id, 1.05f + random.nextFloat() * 0.05f);
        stats.getMaxSpeed().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getMaxTurnRate().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getAcceleration().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getDeceleration().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getTurnAcceleration().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getFluxDissipation().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getFluxCapacity().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getShieldDamageTakenMult().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getHullBonus().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getArmorBonus().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getSensorProfile().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getSensorStrength().modifyPercent(id, -10f + random.nextFloat() * 15f);
        stats.getPeakCRDuration().modifyPercent(id, -10f + random.nextFloat() * 15f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0 && ship.getFleetMemberId() != null) {
            Random random = new Random(ship.getFleetMemberId().hashCode());
            return Utils.asFloatTwoDecimals(1.05f + random.nextFloat() * 0.05f);
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
