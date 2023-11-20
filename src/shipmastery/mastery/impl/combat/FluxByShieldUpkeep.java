package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class FluxByShieldUpkeep extends BaseMasteryEffect {
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FluxByShieldUpkeepPost, 5f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Note: not recommended in general to modify stats in applyEffectsAfterShipCreation
        // however, flux capacity and flux dissipation do get properly updated here
        // other stats may not
        if (ship.getShield() == null || ship.getShield().getType() == ShieldAPI.ShieldType.PHASE) return;
        MutableShipStatsAPI stats = ship.getMutableStats();
        float upkeep = stats.getShieldUpkeepMult().getModifiedValue() * stats.getVariant().getHullSpec().getShieldSpec().getUpkeepCost();
        float flatIncrease = upkeep * getStrength();
        stats.getFluxDissipation().modifyFlat(id, flatIncrease);
        stats.getFluxCapacity().modifyFlat(id, flatIncrease * 10f);
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float increase = Math.max(getStrength(), -1f);
        return MultiplicativeMasteryEffect.makeGenericDescriptionStatic(
                Strings.Descriptions.FluxByShieldUpkeep,
                Strings.Descriptions.FluxByShieldUpkeepNeg,
                increase > 0f,
                true,
                false,
                increase,
                increase * 10f);
    }
}
