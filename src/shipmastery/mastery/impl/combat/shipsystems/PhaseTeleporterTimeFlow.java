package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PhaseTeleporterTimeFlow extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.PhaseTeleporterTimeFlow)
                                 .params(getSystemName(), Utils.asPercent(strength), Utils.asFloatOneDecimal(5f*strength));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        tooltip.addPara(
                Strings.Descriptions.PhaseTeleporterTimeFlowPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asFloatOneDecimal(15f*strength),
                Utils.asFloatOneDecimal(30f*strength),
                Utils.asFloatOneDecimal(45f*strength),
                Utils.asFloatOneDecimal(60f*strength));
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null || ship.getSystem() == null | !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(PhaseTeleporterTimeFlowScript.class)) {
            ship.addListener(new PhaseTeleporterTimeFlowScript(ship, getStrength(ship), 5f*getStrength(ship), 15f*getStrength(ship), id));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.getMutableStats().getTimeMult().unmodify(id);
        ship.removeListenerOfClass(PhaseTeleporterTimeFlowScript.class);
    }

    @Override
    public String getSystemSpecId() {
        return "phaseteleporter";
    }

    static class PhaseTeleporterTimeFlowScript extends BaseShipSystemListener implements AdvanceableListener,
                                                                                         ShipDestroyedListener {
        final ShipAPI ship;
        final float maxTimeMult;
        final float maxTime;
        final float basePPTAmount;
        final String id;
        float timeLeft = 0f;
        final float particlesPerSecond = 50;
        final int particleCount;
        final JitterEmitter emitter;

        PhaseTeleporterTimeFlowScript(ShipAPI ship, float strength, float maxTime, float basePPTAmount, String id) {
            this.ship = ship;
            this.maxTimeMult = 1f + strength;
            this.maxTime = maxTime;
            this.basePPTAmount = basePPTAmount;
            this.id = id;
            particleCount = (int) (maxTime * particlesPerSecond);
            emitter = new JitterEmitter(
                    ship,
                    ship.getSpriteAPI(),
                    ship.getSpriteAPI().getAverageColor(),
                    150f,
                    25f,
                    0.7f,
                    true,
                    0.2f,
                    particleCount);
            emitter.setSaturationShift(1.5f);
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void onFullyActivate() {
            timeLeft = maxTime;
            Particles.stream(emitter, 1, particlesPerSecond, maxTime, new Particles.StreamAction<JitterEmitter>() {
                @Override
                public boolean apply(JitterEmitter emitter) {
                    return timeLeft > 0f;
                }
            });
        }

        @Override
        public void advance(float amount) {
            if (timeLeft <= 0f) {
                ship.getMutableStats().getTimeMult().unmodify(id);
                return;
            }

            MutableStat timeMult = ship.getMutableStats().getTimeMult();
            timeMult.unmodify(id);
            float currentTimeMult = timeMult.getModifiedValue();
            float targetTimeMult = maxTimeMult + (1f - maxTimeMult) * (maxTime - timeLeft) / maxTime;
            float ratio = targetTimeMult / currentTimeMult;
            timeLeft -= amount / timeMult.getModifiedValue();
            timeMult.modifyMult(id, ratio);
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / ratio);
            }
            Utils.maintainStatusForPlayerShip(ship,
                                              id,
                                              ship.getSystem().getSpecAPI().getIconSpriteName(),
                                              Strings.Descriptions.PhaseTeleporterTimeFlowTitle,
                                              String.format(Strings.Descriptions.PhaseTeleporterTimeFlowDesc1, Utils.asPercentNoDecimal(timeMult.getModifiedValue())),
                                              false);
        }

        @Override
        public void reportShipDestroyed(ShipAPI source, ShipAPI target) {
            if (timeLeft <= 0f) return;
            if (EngineUtils.shipIsOwnedBy(source, ship) && !target.isFighter()) {
                int size = Utils.hullSizeToInt(target.getHullSize());
                float amount = (size + 1f) * basePPTAmount;
                MutableStat.StatMod current = ship.getMutableStats().getPeakCRDuration().getFlatBonus(id);
                StatBonus peakCR = ship.getMutableStats().getPeakCRDuration();
                if (ship.getPeakTimeRemaining() > 0f) {
                    peakCR.modifyFlat(id, current == null ? amount : current.getValue() + amount);
                }
                else {
                    peakCR.unmodify(id);
                    float peakTime = peakCR.computeEffective(ship.getHullSpec().getNoCRLossTime());
                    float diff = (ship.getTimeDeployedForCRReduction() - peakTime) / peakCR.getMult();
                    if (diff > 0f) {
                        ship.getMutableStats().getPeakCRDuration().modifyFlat(id, diff + amount);
                    }
                }
            }
        }
    }
}
