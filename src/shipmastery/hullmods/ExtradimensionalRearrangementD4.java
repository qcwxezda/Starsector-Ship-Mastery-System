package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ExtradimensionalRearrangementD4 extends BaseHullMod {

    public static final float CR_LOSS_PER_100_DAMAGE = 0.04f;
    public static final float MAX_CR_LOSS_PER_HIT = 1f;

    public float getStrength(MutableShipStatsAPI stats) {
        return CR_LOSS_PER_100_DAMAGE * (stats == null ? 1f : stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener((DamageTakenModifier) (param, target, damage, point, shieldHit) -> {
            if (ship.getCurrentCR() <= 0.3f) return null;
            float amount = damage.getDamage();
            if (damage.isDps()) amount *= damage.getDpsDuration();
            float hullSizeMult = 1f+Utils.hullSizeToInt(ship.getHullSize());
            ship.setCurrentCR(Math.max(0f, ship.getCurrentCR() -
                    Math.min(MAX_CR_LOSS_PER_HIT/hullSizeMult,
                            getStrength(ship.getMutableStats())*amount/(10000f*hullSizeMult))));
            return null;
        });
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(Strings.Hullmods.rearrangementD4Effect, 8f);
    }
}
