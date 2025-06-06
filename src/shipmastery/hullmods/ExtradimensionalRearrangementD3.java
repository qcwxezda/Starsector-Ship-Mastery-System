package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;

public class ExtradimensionalRearrangementD3 extends BaseHullMod {

    public static float HULL_DAMAGE_PER_SECOND = 0.01f;

    public float getStrength(MutableShipStatsAPI stats) {
        return HULL_DAMAGE_PER_SECOND * (stats == null ? 1f : stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener((AdvanceableListener) amount -> {
           if (ship.getPeakTimeRemaining() > 0f) return;
           if (ship.getHitpoints() <= 1f) return;
           float damagePerSecond = ship.getHitpoints() * getStrength(ship.getMutableStats());
           float damage = damagePerSecond * amount;
           ship.setHitpoints(ship.getHitpoints() - damage);
        });
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(Strings.Hullmods.rearrangementD3Effect, 8f);
    }
}
