package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MaxOPForHull extends MultiplicativeMasteryEffect {

    static final float HULL_LOSS_PER_PERCENT_OP = 0.03f;
    static final float MIN_HULL_MODIFIER = 0.1f;
    static final float MAX_HULL_MODIFIER = 0.9f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        MasteryDescription description = makeGenericDescription(
                Strings.Descriptions.MaxOPForHull,
                null,
                true,
                false,
                getIncrease(),
                0.01f,
                HULL_LOSS_PER_PERCENT_OP);
        description.colors[1] = Misc.getNegativeHighlightColor();
        description.colors[2] = Misc.getNegativeHighlightColor();
        return description;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        int opExcess = -stats.getVariant().getUnusedOP(null);
        int maxOP = stats.getVariant().getHullSpec().getOrdnancePoints(null);


        if (maxOP == 0) return;
        float frac = 1f - (float) opExcess / maxOP * HULL_LOSS_PER_PERCENT_OP * 100f;
        if (frac < 1f) {
            frac = Math.min(frac, MAX_HULL_MODIFIER);
            frac = Math.max(frac, MIN_HULL_MODIFIER);
            stats.getHullBonus().modifyMult(id, frac);
        }
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {
        Global.getSector().getPlayerStats().getShipOrdnancePointBonus().modifyMult(id, getMult());
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {
        Global.getSector().getPlayerStats().getShipOrdnancePointBonus().unmodify(id);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.MaxOPForHullPost, 5f, Misc.getNegativeHighlightColor(),
                        Utils.absValueAsPercent(1f - MAX_HULL_MODIFIER),
                        Utils.absValueAsPercent(1f - MIN_HULL_MODIFIER));
    }
}
