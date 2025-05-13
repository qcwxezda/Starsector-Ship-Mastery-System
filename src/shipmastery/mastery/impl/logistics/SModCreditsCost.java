package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class SModCreditsCost extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.SModCreditsCost,
                Strings.Descriptions.SModCreditsCostNeg,
                true, true, getIncreasePlayer());
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        modify(TransientSettings.SMOD_CREDITS_COST_MULT, id, getMultPlayer());
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.SMOD_CREDITS_COST_MULT.unmodify(id);
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
