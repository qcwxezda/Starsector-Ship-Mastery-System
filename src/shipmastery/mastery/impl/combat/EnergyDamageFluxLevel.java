package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import particleengine.Particles;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class EnergyDamageFluxLevel extends BaseMasteryEffect {

    static final float MAX_FLUX_LEVEL = 0.75f;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrengthForPlayer());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EnergyDamageFluxLevel).params(str, str);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EnergyDamageFluxLevelPost, 0f, Misc.getTextColor(),
                        Utils.asPercent(MAX_FLUX_LEVEL));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(EnergyDamageFluxLevelScript.class)) {
            ship.addListener(new EnergyDamageFluxLevelScript(ship, getStrength(ship), MAX_FLUX_LEVEL, id));
        }
    }

    static class EnergyDamageFluxLevelScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxEffectLevel, maxFluxLevel;
        final IntervalUtil checkerInterval = new IntervalUtil(1f, 1f);
        final String id;
        final EntityBurstEmitter outlineEmitter;
        final IntervalUtil outlineInterval = new IntervalUtil(0.3f, 0.5f);

        EnergyDamageFluxLevelScript(ShipAPI ship, float maxEffectLevel, float maxFluxLevel, String id) {
            this.ship = ship;
            this.maxEffectLevel = maxEffectLevel;
            this.maxFluxLevel = maxFluxLevel;
            this.id = id;
            outlineEmitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), new Color(100, 200, 255), 6, 5f, 1.5f);
            outlineEmitter.enableDynamicAnchoring();
            outlineEmitter.fadeInFrac = 0.2f;
            outlineEmitter.fadeOutFrac = 0.8f;
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive() || ship.getHitpoints() <= 0f) {
                ship.removeListener(this);
            }
            checkerInterval.advance(amount);

            float effectMult = Math.min(1f, ship.getFluxLevel() / maxFluxLevel);
            float effectLevel = maxEffectLevel * effectMult;
            if (ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting()) {
                effectLevel = 0f;
            }

            if (checkerInterval.intervalElapsed()) {
                ship.getMutableStats().getEnergyWeaponDamageMult().modifyPercent(id, 100f * effectLevel);
            }

            if (effectLevel > 0f) {
                Utils.maintainStatusForPlayerShip(ship,
                        id ,
                        "graphics/icons/hullsys/high_energy_focus.png",
                        Strings.Descriptions.EnergyDamageFluxLevelTitle,
                        String.format(Strings.Descriptions.EnergyDamageFluxLevelDesc1, Utils.asPercentNoDecimal(effectLevel)),
                        false);
                outlineEmitter.alphaMult = effectMult * 0.25f;
                outlineEmitter.widthGrowth = effectMult * 8f;
                outlineInterval.advance(amount);
                if (outlineInterval.intervalElapsed()) {
                    Particles.burst(outlineEmitter, 6);
                }
            }

            if (!Global.getCombatEngine().isEntityInPlay(ship)) {
                ship.removeListener(this);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.se + 2f * wsc.me + 4f * wsc.le;
        if (count <= 0f) return null;
        return Utils.getSelectionWeightScaledByValue(count, 3f, false);
    }
}
