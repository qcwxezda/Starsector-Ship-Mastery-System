package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class RestorationCost extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.RestorationCost,
                Strings.Descriptions.RestorationCostNeg,
                true, true, getIncreasePlayer());
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        modifyPlayer(TransientSettings.SHIP_RESTORE_COST_MULT, id);
        Global.getSettings().setFloat("baseRestoreCostMult", TransientSettings.SHIP_RESTORE_COST_MULT.getModifiedValue());
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.SHIP_RESTORE_COST_MULT.unmodify(id);
        Global.getSettings().setFloat("baseRestoreCostMult", TransientSettings.SHIP_RESTORE_COST_MULT.getModifiedValue());
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // boring, don't select
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}
