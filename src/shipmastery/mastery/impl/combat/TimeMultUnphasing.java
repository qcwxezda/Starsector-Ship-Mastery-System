package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.PhaseCloakStats;
import com.fs.starfarer.combat.entities.Ship;
import particleengine.Particles;
import shipmastery.graphics.JitterEmitter;
import shipmastery.graphics.OutlineEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

public class TimeMultUnphasing extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init("placeholder...");
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new TimeMultUnphasingScript(ship, getStrength(ship), id));
    }

    public static class TimeMultUnphasingScript implements AdvanceableListener {
        final float maxTime;
        boolean isAcceleratedUnphased = false;
        float timeAcceleratedUnphased = 0f;
        final ShipAPI ship;
        final String id;
        final float maxTimeMult;
        final JitterEmitter emitter;
        final OutlineEmitter outlineEmitter;

        TimeMultUnphasingScript(ShipAPI ship, float maxTime, String id) {
            this.ship = ship;
            this.maxTime = maxTime;
            this.id = id;
            maxTimeMult = PhaseCloakStats.getMaxTimeMult(ship.getMutableStats());
            emitter = new JitterEmitter(ship, false);
            emitter.enableDynamicAnchoring();
            outlineEmitter = new OutlineEmitter(ship);
            outlineEmitter.enableDynamicAnchoring();
        }

        @Override
        public void advance(float amount) {
            boolean isUnphasing = ((Ship) ship).isUnphasing();
            if (isUnphasing && !isAcceleratedUnphased) {
                isAcceleratedUnphased = true;
                Particles.stream(emitter, 2, 60, maxTime, new Particles.StreamAction<JitterEmitter>() {
                    @Override
                    public boolean apply(JitterEmitter emitter) {
                        return isAcceleratedUnphased;
                    }
                });

                Particles.stream(outlineEmitter, 200, 2000, maxTime, new Particles.StreamAction<OutlineEmitter>() {
                    @Override
                    public boolean apply(OutlineEmitter emitter) {
                        return isAcceleratedUnphased;
                    }
                });

                timeAcceleratedUnphased = 0f;
            }

            if (ship.isPhased() && !isUnphasing) {
                isAcceleratedUnphased = false;
                ship.getMutableStats().getTimeMult().unmodify(id);
            }

            if (isAcceleratedUnphased) {
                MutableStat timeMult = ship.getMutableStats().getTimeMult();
                timeMult.unmodify(id);
                float currentTimeMult = timeMult.getModifiedValue();
                float targetTimeMult = maxTimeMult + (1f - maxTimeMult) * timeAcceleratedUnphased / maxTime;
                float ratio = targetTimeMult / currentTimeMult;
                timeMult.modifyMult(id, ratio);
                timeAcceleratedUnphased += amount / timeMult.getModifiedValue();
                if (timeAcceleratedUnphased >= maxTime) {
                    isAcceleratedUnphased = false;
                    timeMult.unmodify(id);
                }
            }
        }
    }
}
