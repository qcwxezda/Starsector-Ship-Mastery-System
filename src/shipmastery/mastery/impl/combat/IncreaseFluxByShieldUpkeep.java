package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class IncreaseFluxByShieldUpkeep extends BaseMasteryEffect {
    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.INCREASE_FLUX_BY_SHIELD_UPKEEP_POST, 5f);
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
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        float increase = Math.max(getStrength(), -1f);
        return MultiplicativeMasteryEffect.makeGenericDescription(
                Strings.INCREASE_FLUX_BY_SHIELD_UPKEEP,
                Strings.INCREASE_FLUX_BY_SHIELD_UPKEEP_NEG,
                true,
                false,
                increase,
                increase * 10f);
    }
}
