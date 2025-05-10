package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Random;

public class ExtradimensionalRearrangement1 extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null) return;

        stats.getTimeMult().modifyPercent(id, 20f);

        float[] params = getParams(stats.getFleetMember().getId().hashCode());
        stats.getMaxSpeed().modifyPercent(id, params[0]);

        stats.getMaxTurnRate().modifyPercent(id, params[1]);
        stats.getAcceleration().modifyPercent(id, params[1]);
        stats.getDeceleration().modifyPercent(id, params[1]);
        stats.getTurnAcceleration().modifyPercent(id, params[1]);

        stats.getFluxCapacity().modifyPercent(id, params[2]);
        stats.getFluxDissipation().modifyPercent(id, params[3]);
        stats.getHullBonus().modifyPercent(id, params[4]);
        stats.getArmorBonus().modifyPercent(id, params[5]);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || ship.getFleetMemberId() == null) return;
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect1, 8f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(0.2f));
        float[] params = getParams(ship.getFleetMemberId().hashCode());
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect2, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[0]/100f));
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect3, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[1]/100f));
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect4, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[2]/100f));
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect5, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[3]/100f));
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect6, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[4]/100f));
        tooltip.addPara(Strings.Hullmods.rearrangement1Effect7, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent(-params[5]/100f));
    }

    private float[] getParams(int seed) {
        Random random = new Random(seed);
        return new float[] {
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat()),
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat()),
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat()),
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat()),
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat()),
                -20f + 10f * Math.max(random.nextFloat(), random.nextFloat())
        };
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
