package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
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

public class StartingRange extends BaseMasteryEffect {

    public static final float[] MAX_RANGE_BONUS = {0.1f, 0.15f, 0.2f, 0.25f};
    public static final float MAX_PPT = 600f;
    public static final float BASE_PPT_FRAC = 0.4f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.StartingRange)
                .params(Utils.asPercent(MAX_RANGE_BONUS[Utils.hullSizeToInt(selectedVariant.getHullSize())]), Utils.asPercentNoDecimal(BASE_PPT_FRAC*getStrength(selectedVariant)))
                .colors(Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.StartingRangePost, 0f, Misc.getTextColor(), Utils.asInt(MAX_PPT), Utils.asInt(MAX_PPT));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new StartingRangeScript(ship, BASE_PPT_FRAC*getStrength(ship)));
    }

    private class StartingRangeScript implements AdvanceableListener {
        private final ShipAPI ship;
        private final float pptFrac;
        private float timeElapsed = 0f;
        private float rangeBoost = 0f;
        private final float ppt;
        private final IntervalUtil updateInterval = new IntervalUtil(1f, 1f);

        private StartingRangeScript(ShipAPI ship, float pptFrac) {
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
                float effectStrength = Math.max(0f, 1f - timeElapsed / (pptFrac*ppt));
                rangeBoost = effectStrength * MAX_RANGE_BONUS[Utils.hullSizeToInt(ship.getHullSize())];
                if (rangeBoost <= 0f) {
                    ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
                    ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
                }
                else {
                    ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, 100f*rangeBoost);
                    ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, 100f*rangeBoost);
                }
            }
            if (rangeBoost > 0f) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/sensor_array.png",
                        Strings.Descriptions.StartingRangeTitle,
                        String.format(Strings.Descriptions.StartingRangeDesc1, Utils.asPercent(rangeBoost)),
                        false
                );
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        if (wsc.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f) >= 0.8f) return null;
        return 1f;
    }
}
