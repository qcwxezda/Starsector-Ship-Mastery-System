package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ExtradimensionalRearrangement4 extends BaseHullMod {

    public static final int[] DP_REDUCTION = {1, 2, 3, 5};

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, -DP_REDUCTION[Utils.hullSizeToInt(hullSize)]);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangement4Effect,
                8f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                "" + DP_REDUCTION[0],
                "" + DP_REDUCTION[1],
                "" + DP_REDUCTION[2],
                "" + DP_REDUCTION[3]);
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
