package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class SpeedHullLevel extends BaseMasteryEffect {

    static final float MIN_HULL_LEVEL = 0.25f;
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.SpeedHullLevel).params(Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SpeedHullLevelPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Utils.asPercent(MIN_HULL_LEVEL));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(SpeedHullLevelScript.class)) {
            ship.addListener(new SpeedHullLevelScript(ship, getStrength(ship), MIN_HULL_LEVEL, id));
        }
    }

    static class SpeedHullLevelScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxEffectLevel, minHullLevel;
        final IntervalUtil checkerInterval = new IntervalUtil(1f, 1f);
        final String id;
        final OverlayEmitter emitter;
        final IntervalUtil trailInterval = new IntervalUtil(0.2f, 0.2f);

        SpeedHullLevelScript(ShipAPI ship, float maxEffectLevel, float minHullLevel, String id) {
            this.ship = ship;
            this.maxEffectLevel = maxEffectLevel;
            this.minHullLevel = minHullLevel;
            this.id = id;
            emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 0.8f);
            emitter.layer = CombatEngineLayers.BELOW_SHIPS_LAYER;
            emitter.fadeInFrac = 0.1f;
            emitter.fadeOutFrac = 0.9f;
            emitter.color = new Color(255, 150, 100, 255);
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
                ship.getMutableStats().getMaxSpeed().modifyPercent(id, 100f * effectLevel);
            }

            if (effectLevel > 0f) {
                Utils.maintainStatusForPlayerShip(ship,
                        id + "1",
                        "graphics/icons/hullsys/infernium_injector.png",
                        Strings.Descriptions.SpeedHullLevelTitle,
                        String.format(Strings.Descriptions.SpeedHullLevelDesc1, Utils.asPercentNoDecimal(effectLevel), Utils.asPercentNoDecimal(2f*effectLevel)),
                        false);
                emitter.alphaMult = effectMult * 0.5f;

                trailInterval.advance(amount);
                if (trailInterval.intervalElapsed()) {
                    Particles.burst(emitter, 1);
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
        return 1f;
    }
}
