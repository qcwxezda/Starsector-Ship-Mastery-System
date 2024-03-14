package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
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

public class MinimumReplacementRate extends BaseMasteryEffect {

    public static final float BASE_REPLACEMENT_RATE_MIN = Global.getSettings().getFloat("minFighterReplacementRate");

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.MinimumReplacementRate)
                                 .colors(Misc.getTextColor(),Settings.POSITIVE_HIGHLIGHT_COLOR)
                                 .params(Utils.asPercent(0), Utils.asPercent(BASE_REPLACEMENT_RATE_MIN + getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.MinimumReplacementRatePost, 0f, Misc.getTextColor(), Utils.asPercent(BASE_REPLACEMENT_RATE_MIN));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getNumFighterBays() <= 0) return;
        if (!ship.hasListenerOfClass(MinimumReplacementRateScript.class)) {
            ship.addListener(new MinimumReplacementRateScript(ship, getStrength(ship)));
        }
    }

    static class MinimumReplacementRateScript implements AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        final IntervalUtil checkInterval = new IntervalUtil(0.5f, 1f);

        MinimumReplacementRateScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
        }
        @Override
        public void advance(float amount) {
            if (ship.getCurrentCR() <= 0f || !ship.isAlive()) {
                ship.removeListener(this);
                return;
            }

            checkInterval.advance(amount);
            if (checkInterval.intervalElapsed()) {
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    bay.setCurrRate(MathUtils.clamp(bay.getCurrRate(), BASE_REPLACEMENT_RATE_MIN + strength, 1f));
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getFighterBays() <= 0) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getFighterBays(), 2f, false);
    }
}
