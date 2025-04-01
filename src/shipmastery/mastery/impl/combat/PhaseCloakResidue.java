package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.PhaseCloakStats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.combat.entities.Ship;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class PhaseCloakResidue extends BaseMasteryEffect {

    static final float CLOAK_COOLDOWN_MULT = 4f;
    static final float CLOAK_COST_MULT = 5f;
    static final float MAX_DAMAGE_REDUCTION = 0.75f;

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.PhaseCloakResiduePost,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(MAX_DAMAGE_REDUCTION),
                Utils.asFloatOneDecimal(CLOAK_COST_MULT),
                Utils.asFloatOneDecimal(CLOAK_COOLDOWN_MULT));
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.PhaseCloakResidue).params(Utils.asFloatOneDecimal(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float mult = CLOAK_COOLDOWN_MULT;
        if (stats.getVariant().hasHullMod("ex_phase_coils")) {
            mult *= 5f;
        }
        stats.getPhaseCloakCooldownBonus().modifyMult(id, mult);
        stats.getPhaseCloakActivationCostBonus().modifyMult(id, CLOAK_COST_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(TimeMultUnphasingScript.class)) {
            ship.addListener(new TimeMultUnphasingScript(ship, getStrength(ship), id));
        }
    }

    public static class TimeMultUnphasingScript implements AdvanceableListener {
        final float maxTime;
        boolean isAcceleratedUnphased = false;
        float timeAcceleratedUnphased = 0f;
        final ShipAPI ship;
        final MutableShipStatsAPI stats;
        final String id;
        final float maxTimeMult;
        final JitterEmitter emitter;
        final int particleCount;
        static final int particlesPerSecond = 50;

        TimeMultUnphasingScript(ShipAPI ship, float maxTime, String id) {
            this.ship = ship;
            stats = ship.getMutableStats();
            this.maxTime = maxTime;
            this.id = id;
            maxTimeMult = PhaseCloakStats.getMaxTimeMult(ship.getMutableStats());
            particleCount = (int) (maxTime * particlesPerSecond);
            emitter = new JitterEmitter(
                    ship,
                    ship.getSpriteAPI(),
                    ship.getSpriteAPI().getAverageColor(),
                    150f,
                    40f,
                    0.8f,
                    true,
                    0.2f,
                    particleCount);
            emitter.setSaturationShift(1.5f);
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void advance(float amount) {
            boolean isUnphasing = ((Ship) ship).isUnphasing();
            if (isUnphasing && !isAcceleratedUnphased) {
                isAcceleratedUnphased = true;
                Particles.stream(emitter, 1, particlesPerSecond, maxTime, emitter -> isAcceleratedUnphased);
                timeAcceleratedUnphased = 0f;
            }

            if (ship.isPhased() && !isUnphasing) {
                resetTimeMults();
            }

            if (ship.getFluxTracker().isVenting()) {
                resetTimeMults();
            }

            if (isAcceleratedUnphased) {
                MutableStat timeMult = stats.getTimeMult();
                timeMult.unmodify(id);
                float currentTimeMult = timeMult.getModifiedValue();
                float targetTimeMult = maxTimeMult + (1f - maxTimeMult) * timeAcceleratedUnphased / maxTime;
                float ratio = targetTimeMult / currentTimeMult;
                timeAcceleratedUnphased += amount / timeMult.getModifiedValue();
                if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / ratio);
                }
                timeMult.modifyMult(id, ratio);
                Utils.maintainStatusForPlayerShip(ship,
                        id + "1",
                        ship.getPhaseCloak().getSpecAPI().getIconSpriteName(),
                        Strings.Descriptions.PhaseCloakResidueStatusTitle,
                        String.format(Strings.Descriptions.PhaseCloakResidueStatusDesc1, Utils.asPercentNoDecimal(timeMult.getModifiedValue())),
                        false);
                float damageMult = 1f - ((timeMult.getModifiedValue() - 1f) / (maxTimeMult - 1f)) * MAX_DAMAGE_REDUCTION;
                stats.getHullDamageTakenMult().modifyMult(id, damageMult);
                stats.getArmorDamageTakenMult().modifyMult(id, damageMult);
                stats.getEmpDamageTakenMult().modifyMult(id, damageMult);
                Utils.maintainStatusForPlayerShip(ship,
                        id + "2",
                        ship.getPhaseCloak().getSpecAPI().getIconSpriteName(),
                        Strings.Descriptions.PhaseCloakResidueStatusTitle,
                        String.format(Strings.Descriptions.PhaseCloakResidueStatusDesc2, Utils.asPercentNoDecimal(1f - damageMult)),
                        false);
                if (timeAcceleratedUnphased >= maxTime) {
                    resetTimeMults();
                }
            }
        }

        void resetTimeMults() {
            isAcceleratedUnphased = false;
            stats.getTimeMult().unmodify(id);
            Global.getCombatEngine().getTimeMult().unmodify(id);
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getEmpDamageTakenMult().unmodify(id);
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isPhase()) return null;
        return 1.5f;
    }
}
