package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class LatentDR extends BaseMasteryEffect {
    public static final float MAX_DAMAGE_REDUCTION = 0.1f;
    public static final float MAX_PPT = 600f;
    public static final float BASE_PPT_FRAC = 0.8f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.LatentDR)
                .params(Utils.asPercent(MAX_DAMAGE_REDUCTION), Utils.asPercentNoDecimal(BASE_PPT_FRAC/getStrength(selectedVariant)))
                .colors(Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.LatentDRPost, 0f, Misc.getTextColor(), Utils.asInt(MAX_PPT), Utils.asInt(MAX_PPT));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new LatentDRScript(ship, BASE_PPT_FRAC/getStrength(ship)));
    }

    private class LatentDRScript implements AdvanceableListener {
        private final ShipAPI ship;
        private final float pptFrac;
        private float timeElapsed = 0f;
        private final float ppt;
        private final IntervalUtil updateInterval = new IntervalUtil(1f, 1f);

        private LatentDRScript(ShipAPI ship, float pptFrac) {
            this.ship = ship;
            this.pptFrac = pptFrac;
            float pptTemp = ship.getPeakTimeRemaining();
            if (pptTemp <= 0f || pptTemp > MAX_PPT) {
                pptTemp = MAX_PPT;
            }
            ppt = pptTemp;
        }

        @Override
        public void advance(float amount) {
            timeElapsed += amount;
            updateInterval.advance(amount);
            if (updateInterval.intervalElapsed()) {
                float effectStrength = Math.min(1f, timeElapsed / (pptFrac*ppt));
                float dr = effectStrength * MAX_DAMAGE_REDUCTION;
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f-dr);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f-dr);
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f-dr);
                ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 1f-dr);
            }
        }
    }
}
