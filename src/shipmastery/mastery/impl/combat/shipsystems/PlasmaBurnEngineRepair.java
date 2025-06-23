package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class PlasmaBurnEngineRepair extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.PlasmaBurnEngineRepair).params(getSystemName(),
                                                                                                           Utils.asFloatOneDecimal(2f * getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        tooltip.addPara(Strings.Descriptions.PlasmaBurnEngineRepairPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asFloatOneDecimal(strength), Utils.asPercent(strength));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        float strength = getStrength(ship);
        ship.addListener(new PlasmaBurnEngineRepairScript(ship, 2f * strength, strength, strength));
    }

    @Override
    public String getSystemSpecId() {
        return "microburn";
    }

    class PlasmaBurnEngineRepairScript extends BaseShipSystemListener implements AdvanceableListener {

        final ShipAPI ship;
        final float repairSpeedMult, turnRateMult, turnRateTime;
        float postEffectLevel = 0f;
        static final Color color = new Color(89, 255, 89);
        final EntityBurstEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.2f, 0.2f);

        PlasmaBurnEngineRepairScript(ShipAPI ship, float repairSpeedMult, float turnRateMult, float turnRateTime) {
            this.ship = ship;
            this.repairSpeedMult = repairSpeedMult;
            this.turnRateMult = turnRateMult;
            this.turnRateTime = turnRateTime;
            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), color, 4, 10f, 1f);
            emitter.alphaMult = 0.5f;
            emitter.fadeInFrac = 0.5f;
            emitter.fadeOutFrac = 0.5f;
            emitter.widthGrowth = -10f;
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void onFullyDeactivate() {
            postEffectLevel = 1f;
        }

        @Override
        public void advanceWhileOn(float amount) {
            ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, 1f / repairSpeedMult);
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    getSystemSpec().getIconSpriteName(),
                    Strings.Descriptions.PlasmaBurnEngineRepairTitle,
                    String.format(Strings.Descriptions.PlasmaBurnEngineRepairDesc1, Utils.asFloatOneDecimal(repairSpeedMult)),
                    false);
        }

        @Override
        public void advance(float amount) {
            if (postEffectLevel > 0f) {
                ship.getMutableStats().getMaxTurnRate().modifyPercent(id, 100f * turnRateMult * postEffectLevel);
                ship.getMutableStats().getTurnAcceleration().modifyPercent(id, 100f * turnRateMult * postEffectLevel);
                ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, 1f / repairSpeedMult);
                Utils.maintainStatusForPlayerShip(ship,
                        id,
                        getSystemSpec().getIconSpriteName(),
                        Strings.Descriptions.PlasmaBurnEngineRepairTitle,
                        String.format(Strings.Descriptions.PlasmaBurnEngineRepairDesc1, Utils.asFloatOneDecimal(repairSpeedMult)),
                        false);
                Utils.maintainStatusForPlayerShip(ship,
                        id + "2",
                        getSystemSpec().getIconSpriteName(),
                        Strings.Descriptions.PlasmaBurnEngineRepairTitle,
                        String.format(Strings.Descriptions.PlasmaBurnEngineRepairDesc2, Utils.asPercentNoDecimal(turnRateMult * postEffectLevel)),
                        false);
                postEffectLevel -= amount / turnRateTime;
            } else {
                ship.getMutableStats().getMaxTurnRate().unmodify(id);
                ship.getMutableStats().getTurnAcceleration().unmodify(id);
                ship.getMutableStats().getCombatEngineRepairTimeMult().unmodify(id);
            }

            float effectAlpha = postEffectLevel;
            if (ship.getSystem().isActive()) {
                effectAlpha = 1f;
            }
            if (effectAlpha > 0f) {
                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    emitter.alphaMult = effectAlpha;
                    Particles.burst(emitter, 4);
                }
            }
        }
    }
}
