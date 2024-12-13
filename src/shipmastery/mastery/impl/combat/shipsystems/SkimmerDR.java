package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class SkimmerDR extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.SkimmerDR).params(
                getSystemName(),
                Utils.asPercent(strength),
                Utils.asFloatOneDecimal(15f * strength));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SkimmerDRPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(SkimmerDRScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new SkimmerDRScript(ship, 1f - strength, strength * 15f, id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "displacer";
    }

    static class SkimmerDRScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final float damageMult;
        final float duration;
        final String id;
        boolean active = false;
        float activeDur = 0f;
        final EntityBurstEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.1f, 0.1f);

        SkimmerDRScript(ShipAPI ship, float damageMult, float duration, String id) {
            this.ship = ship;
            this.damageMult = damageMult;
            this.duration = duration;
            this.id = id;

            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), new Color(100, 200, 255, 255), 8, 25f, 1f);
            emitter.widthGrowth = -20f;
            emitter.alphaMult = 0.1f;
            emitter.jitterRadius = 10f;
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void onFullyActivate() {
            active = true;
            activeDur = 0f;
        }

        @Override
        public void advance(float amount) {
            if (active) {
                float effectLevel = 1f - activeDur / duration;

                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - (1f - damageMult) * effectLevel);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - (1f - damageMult) * effectLevel);

                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/damper_field.png",
                        Strings.Descriptions.SkimmerDRTitle,
                        String.format(Strings.Descriptions.SkimmerDRDesc1, (int) (100f * (1f - damageMult) * effectLevel) + "%"),
                        false);

                emitter.width = 25f * effectLevel;
                emitter.widthGrowth = -20f * effectLevel;
                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    Particles.burst(emitter, 8);
                }

                activeDur += amount;
                if (activeDur >= duration) {
                    active = false;
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                }
            }
        }
    }
}
