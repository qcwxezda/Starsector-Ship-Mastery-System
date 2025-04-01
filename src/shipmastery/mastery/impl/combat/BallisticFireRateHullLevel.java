package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BallisticFireRateHullLevel extends BaseMasteryEffect {

    static final float MIN_HULL_LEVEL = 0.25f;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrengthForPlayer());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BallisticFireRateHullLevel).params(str, str);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BallisticFireRateHullLevelPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Utils.asPercent(MIN_HULL_LEVEL));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(BallisticFireRateHullLevelScript.class)) {
            ship.addListener(new BallisticFireRateHullLevelScript(ship, getStrength(ship), MIN_HULL_LEVEL, id));
        }
    }

    static class BallisticFireRateHullLevelScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxEffectLevel, minHullLevel;
        final IntervalUtil checkerInterval = new IntervalUtil(1f, 1f);
        final String id;
        final EntityBurstEmitter outlineEmitter;
        final IntervalUtil outlineInterval = new IntervalUtil(0.3f, 0.5f);

        BallisticFireRateHullLevelScript(ShipAPI ship, float maxEffectLevel, float minHullLevel, String id) {
            this.ship = ship;
            this.maxEffectLevel = maxEffectLevel;
            this.minHullLevel = minHullLevel;
            this.id = id;
            outlineEmitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), Color.RED, 6, 0f, 1f);
            outlineEmitter.enableDynamicAnchoring();
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive() || ship.getHitpoints() <= 0f) {
                ship.removeListener(this);
            }
            checkerInterval.advance(amount);

            float hullLevel = ship.getHullLevel();
            float effectMult = Math.min(1f, (1f - hullLevel) / (1f - minHullLevel));
            float effectLevel = maxEffectLevel * effectMult;
            if (checkerInterval.intervalElapsed()) {
                ship.getMutableStats().getBallisticRoFMult().modifyPercent(id, 100f * effectLevel);
                ship.getMutableStats().getBallisticProjectileSpeedMult().modifyPercent(id, 100f * effectLevel);
                ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(id, 1f - effectLevel);
            }

            if (effectLevel > 0f) {
                Utils.maintainStatusForPlayerShip(ship,
                        id + "1",
                        "graphics/icons/hullsys/ammo_feeder.png",
                        Strings.Descriptions.BallisticFireRateHullLevelTitle,
                        String.format(Strings.Descriptions.BallisticFireRateHullLevelDesc1, Utils.asPercentNoDecimal(effectLevel)),
                        false);
                Utils.maintainStatusForPlayerShip(ship,
                        id + "2",
                        "graphics/icons/hullsys/ammo_feeder.png",
                        Strings.Descriptions.BallisticFireRateHullLevelTitle,
                        String.format(Strings.Descriptions.BallisticFireRateHullLevelDesc2, Utils.asPercentNoDecimal(effectLevel)),
                        false);
                outlineEmitter.alphaMult = effectMult * 0.4f;
                outlineEmitter.widthGrowth = effectMult * 10f;
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
        float weight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.BALLISTIC, 0.2f, 0.3f);
        if (weight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 0.4f, 1f);
    }
}
