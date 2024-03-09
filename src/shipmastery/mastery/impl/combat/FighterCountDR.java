package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
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

public class FighterCountDR extends BaseMasteryEffect {

    public static final int MAX_STACKS_PER_WING = 5;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FighterCountDR).params(
                Utils.asInt(getMaxRange(selectedModule)),
                Utils.asPercent(getDRPerFighter(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        int maxStacks = getMaxStacks(selectedModule);
        tooltip.addPara(
                Strings.Descriptions.FighterCountDRPost,
                0f,
                new Color[] {Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asInt(maxStacks),
                Utils.asPercent(maxStacks * getDRPerFighter(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getNumFighterBays() <= 0) return;
        if (!ship.hasListenerOfClass(FighterCountDRScript.class)) {
            ship.addListener(
                    new FighterCountDRScript(
                            ship,
                            getDRPerFighter(ship),
                            getMaxStacks(ship),
                            getMaxRange(ship),
                            id));
        }
    }

    public float getDRPerFighter(ShipAPI ship) {
        return getStrength(ship) / ship.getNumFighterBays();
    }

    public int getMaxStacks(ShipAPI ship) {
        return ship.getNumFighterBays() * MAX_STACKS_PER_WING;
    }

    public float getMaxRange(ShipAPI ship) {
        return getStrength(ship) * 12500f;
    }

    static class FighterCountDRScript implements AdvanceableListener {

        final ShipAPI ship;
        final float amountPerFighter;
        final float maxRange;
        final int maxStacks;
        final String id;
        final IntervalUtil checkInterval = new IntervalUtil(1f, 1.5f);
        int currentStacks = 0;

        FighterCountDRScript(ShipAPI ship, float amount, int maxStacks, float maxRange, String id) {
            this.ship = ship;
            amountPerFighter = amount;
            this.maxStacks = maxStacks;
            this.maxRange = maxRange;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            checkInterval.advance(amount);

            if (checkInterval.intervalElapsed()) {
                currentStacks = 0;
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() != null) {
                        for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                            if (MathUtils.dist(fighter.getLocation(), ship.getLocation()) <= maxRange + ship.getCollisionRadius() + fighter.getCollisionRadius()) {
                                currentStacks++;
                            }
                        }
                    }
                }
                currentStacks = Math.min(currentStacks, maxStacks);
                if (currentStacks > 0) {
                    float damageMult = 1f - currentStacks * amountPerFighter;
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, damageMult);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, damageMult);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, damageMult);
                }
                else {
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                }
            }

            if (currentStacks > 0) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        Strings.Descriptions.FighterCountDRTitle,
                        String.format(Strings.Descriptions.FighterCountDRDesc1, Utils.asPercentNoDecimal(currentStacks * amountPerFighter)),
                        false);
            }
        }
    }
}
