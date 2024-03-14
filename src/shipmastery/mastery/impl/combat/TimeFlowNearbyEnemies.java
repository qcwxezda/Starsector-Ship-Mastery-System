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
import shipmastery.config.Settings;
import shipmastery.fx.JitterEmitter2;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class TimeFlowNearbyEnemies extends BaseMasteryEffect {

    public static final int MAX_EFFECT_STACKS = 10;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.TimeFlowNearbyEnemies)
                .params(Utils.asPercent(getStrength(selectedModule)), Utils.asInt(getRange(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.TimeFlowNearbyEnemiesPost,
                0f,
                new Color[] {Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR},
                "" + MAX_EFFECT_STACKS,
                Utils.asPercent(getStrength(selectedModule) * MAX_EFFECT_STACKS));
    }

    public float getRange(ShipAPI ship) {
        return getStrength(ship) * 40000f;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(TimeFlowNearbyEnemiesScript.class)) {
            ship.addListener(new TimeFlowNearbyEnemiesScript(ship, getRange(ship), getStrength(ship), id));
        }
    }

    static class TimeFlowNearbyEnemiesScript implements AdvanceableListener {

        final ShipAPI ship;
        final float range;
        final float effectPerShip;
        final String id;
        final IntervalUtil checkInterval = new IntervalUtil(1f, 1f);
        final IntervalUtil burstInterval = new IntervalUtil(0.1f, 0.1f);
        final JitterEmitter2 emitter;
        int curCount = 0;

        TimeFlowNearbyEnemiesScript(ShipAPI ship, float range, float effectPerShip, String id) {
            this.ship = ship;
            this.range = range;
            this.effectPerShip = effectPerShip;
            this.id = id;
            emitter = new JitterEmitter2(ship, ship.getSpriteAPI());
            emitter.enableDynamicAnchoring();
            emitter.color = Color.CYAN;
        }

        @Override
        public void advance(float amount) {
            checkInterval.advance(amount);
            if (checkInterval.intervalElapsed()) {
                int count = 0;
                for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
                    if (otherShip.isFighter()) continue;
                    if (otherShip.getOwner() == ship.getOwner()) continue;
                    if (otherShip.getHitpoints() <= 0f) continue;
                    if (MathUtils.dist(ship.getLocation(), otherShip.getLocation()) > range + ship.getCollisionRadius() + otherShip.getCollisionRadius()) continue;
                    count++;
                    if (count == MAX_EFFECT_STACKS) break;
                }

                if (count > 0) {
                    ship.getMutableStats().getTimeMult().modifyPercent(id, 100f * count * effectPerShip);
                }
                else {
                    ship.getMutableStats().getTimeMult().unmodify(id);
                }
                curCount = count;
            }

            if (curCount > 0) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/temporal_shell.png",
                        Strings.Descriptions.TimeFlowNearbyEnemiesTitle,
                        String.format(Strings.Descriptions.TimeFlowNearbyEnemiesDesc1, Utils.asPercentNoDecimal(curCount * effectPerShip)),
                        false);
                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    emitter.radius = (float) curCount / MAX_EFFECT_STACKS * 15f;
                    Particles.burst(emitter, 1);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return 0f;
    }
}
