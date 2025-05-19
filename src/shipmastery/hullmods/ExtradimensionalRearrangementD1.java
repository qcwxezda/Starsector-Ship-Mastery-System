package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ExtradimensionalRearrangementD1 extends BaseHullMod {

    public static final float TIME_MULT = 0.85f;

    public float getStrength(MutableShipStatsAPI stats) {
        return (1f - TIME_MULT) * (stats == null ? 1f : stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getTimeMult().modifyMult(id, 1f - getStrength(stats));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangementD1Effect,
                8f,
                Settings.NEGATIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(getStrength(ship == null ? null : ship.getMutableStats())));
    }
}
