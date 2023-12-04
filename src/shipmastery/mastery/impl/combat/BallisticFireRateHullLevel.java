package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
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

import java.awt.*;

public class BallisticFireRateHullLevel extends BaseMasteryEffect {

    static final float MIN_HULL_LEVEL = 0.25f;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrengthForPlayer());
        String str2 = Utils.asPercent(2f*getStrengthForPlayer());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BallisticFireRateHullLevel).params(str, str2, str);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BallisticFireRateHullLevelPost, 0f, Misc.getNegativeHighlightColor(),
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
            checkerInterval.advance(amount);

            float hullLevel = ship.getHullLevel();
            float effectMult = Math.min(1f, (1f - hullLevel) / (1f - minHullLevel));
            float effectLevel = maxEffectLevel * effectMult;
            if (checkerInterval.intervalElapsed()) {
                ship.getMutableStats().getBallisticRoFMult().modifyPercent(id, 100f * effectLevel);
                ship.getMutableStats().getBallisticProjectileSpeedMult().modifyPercent(id, 200f * effectLevel);
                ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(id, 1f - effectLevel);
            }

            if (effectLevel > 0f) {
                Utils.maintainStatusForPlayerShip(ship,
                        id + "1",
                        "graphics/icons/hullsys/ammo_feeder.png",
                        Strings.Descriptions.BallisticFireRateHullLevelTitle,
                        String.format(Strings.Descriptions.BallisticFireRateHullLevelDesc1, Utils.asPercent(effectLevel), Utils.asPercent(2f*effectLevel)),
                        false);
                Utils.maintainStatusForPlayerShip(ship,
                        id + "2",
                        "graphics/icons/hullsys/ammo_feeder.png",
                        Strings.Descriptions.BallisticFireRateHullLevelTitle,
                        String.format(Strings.Descriptions.BallisticFireRateHullLevelDesc2, Utils.asPercent(effectLevel)),
                        false);
                outlineEmitter.alphaMult = effectMult * 0.4f;
                outlineEmitter.widthGrowth = effectMult * 20f;
                outlineInterval.advance(amount);
                if (outlineInterval.intervalElapsed()) {
                    Particles.burst(outlineEmitter, 4);
                }
            }

            if (!Global.getCombatEngine().isEntityInPlay(ship)) {
                ship.removeListener(this);
            }
        }
    }
}
