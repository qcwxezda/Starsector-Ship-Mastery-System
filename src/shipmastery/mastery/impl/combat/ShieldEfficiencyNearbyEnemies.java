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

public class ShieldEfficiencyNearbyEnemies extends BaseMasteryEffect {

    public static final int MAX_EFFECT_STACKS = 10;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.ShieldEfficiencyNearbyEnemies)
                .params(Utils.asPercent(getStrength(selectedModule)), (int) getRange(selectedModule));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.ShieldEfficiencyNearbyEnemiesPost,
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
        if (!ship.hasListenerOfClass(ShieldEfficiencyNearbyEnemiesScript.class)) {
            ship.addListener(new ShieldEfficiencyNearbyEnemiesScript(ship, getRange(ship), getStrength(ship), id));
        }
    }

    static class ShieldEfficiencyNearbyEnemiesScript implements AdvanceableListener {

        final ShipAPI ship;
        final float range;
        final float effectPerShip;
        final String id;
        final IntervalUtil checkInterval = new IntervalUtil(1f, 1f);
        int curCount = 0;

        ShieldEfficiencyNearbyEnemiesScript(ShipAPI ship, float range, float effectPerShip, String id) {
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
                    if (MathUtils.dist(ship.getLocation(), otherShip.getLocation()) > range + ship.getCollisionRadius() + otherShip.getCollisionRadius()) continue;
                    count++;
                    if (count == MAX_EFFECT_STACKS) break;
                }

                if (count > 0) {
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - count * effectPerShip);
                }
                else {
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                }
                curCount = count;
            }

            if (curCount > 0) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/fortress_shield.png",
                        Strings.Descriptions.ShieldEfficiencyNearbyEnemiesTitle,
                        String.format(Strings.Descriptions.ShieldEfficiencyNearbyEnemiesDesc1, Utils.asPercentNoDecimal(curCount * effectPerShip)),
                        false);
            }
        }
    }
}
