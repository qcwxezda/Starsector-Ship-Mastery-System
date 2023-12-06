package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class DamageTakenNearbyEnemies extends BaseMasteryEffect {

    public static final int MAX_EFFECT_STACKS = 10;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.DamageTakenNearbyEnemies)
                .params(Utils.asPercent(getStrength(selectedModule)), Utils.asInt(getRange(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.DamageTakenNearbyEnemiesPost,
                0f,
                new Color[] {Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR},
                "" + MAX_EFFECT_STACKS,
                Utils.asPercent(getStrength(selectedModule) * MAX_EFFECT_STACKS));
    }

    public float getRange(ShipAPI ship) {
        return getStrength(ship) * 50000f;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(DamageTakenNearbyEnemiesScript.class)) {
            ship.addListener(new DamageTakenNearbyEnemiesScript(ship, getRange(ship), getStrength(ship), id));
        }
    }

    static class DamageTakenNearbyEnemiesScript implements AdvanceableListener {

        final ShipAPI ship;
        final float range;
        final float effectPerShip;
        final String id;
        final IntervalUtil checkInterval = new IntervalUtil(1f, 1f);
        int curCount = 0;

        DamageTakenNearbyEnemiesScript(ShipAPI ship, float range, float effectPerShip, String id) {
            this.ship = ship;
            this.range = range;
            this.effectPerShip = effectPerShip;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            checkInterval.advance(amount);
            if (checkInterval.intervalElapsed()) {
                int count = 0;
                for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
                    if (otherShip.isFighter()) continue;
                    if (otherShip.getOwner() == ship.getOwner()) continue;
                    if (otherShip.getHitpoints() <= 0) continue;
                    if (MathUtils.dist(ship.getLocation(), otherShip.getLocation()) > range + ship.getCollisionRadius() + otherShip.getCollisionRadius()) continue;
                    count++;
                    if (count == MAX_EFFECT_STACKS) break;
                }

                if (count > 0) {
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - count * effectPerShip);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - count * effectPerShip);
                }
                else {
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                }
                curCount = count;
            }

            if (curCount > 0) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/damper_field.png",
                        Strings.Descriptions.DamageTakenNearbyEnemiesTitle,
                        String.format(Strings.Descriptions.DamageTakenNearbyEnemiesDesc1, Utils.asPercentNoDecimal(curCount * effectPerShip)),
                        false);
            }
        }
    }
}
