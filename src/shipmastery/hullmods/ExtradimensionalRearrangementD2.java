package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ExtradimensionalRearrangementD2 extends BaseHullMod {

    public static final float DP_PER_SMOD = 0.1f;

    public float getStrength(MutableShipStatsAPI stats) {
        return DP_PER_SMOD * (stats == null ? 1f : stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() == null) return;
        int nSMods = Misc.getCurrSpecialMods(stats.getVariant());
        if (nSMods == 0) return;
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyPercent(id, 100f*getStrength(stats)*nSMods);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangementD2Effect,
                8f,
                Settings.NEGATIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(getStrength(ship == null ? null : ship.getMutableStats())));
    }
}
