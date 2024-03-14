package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
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

public class DamageChargesDamperField extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.DamageChargesDamperField).params(getSystemName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.DamageChargesDamperFieldPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(minChargePerHit(selectedModule)),
                Utils.asPercent(maxChargePerHit(selectedModule)));
        tooltip.addPara(
                Strings.Descriptions.DamageChargesDamperFieldPost2,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(minDRPerHit(selectedModule)),
                Utils.asPercent(maxDRPerHit(selectedModule)),
                Utils.asPercent(maxTotalDR(selectedModule)));
    }

    public float minChargePerHit(ShipAPI ship) {
        return 0.003f * getStrength(ship) / 0.4f;
    }

    public float maxChargePerHit(ShipAPI ship) {
        return 0.015f * getStrength(ship) / 0.4f;
    }

    public float minDRPerHit(ShipAPI ship) {
        return 0.0015f * getStrength(ship) / 0.4f;
    }

    public float maxDRPerHit(ShipAPI ship) {
        return 0.0075f * getStrength(ship) / 0.4f;
    }

    public float maxTotalDR(ShipAPI ship) {
        return getStrength(ship);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(DamageChargesDamperFieldScript.class)) {
            ship.addListener(new DamageChargesDamperFieldScript(
                    ship,
                    minChargePerHit(ship),
                    maxChargePerHit(ship),
                    minDRPerHit(ship),
                    maxDRPerHit(ship),
                    maxTotalDR(ship),
                    id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "damper";
    }

    static class DamageChargesDamperFieldScript extends BaseShipSystemListener implements AdvanceableListener, DamageListener {

        final ShipAPI ship;
        final float minChargePerHit, maxChargePerHit;
        final float minDRPerHit, maxDRPerHit, maxTotalDR;
        final String id;
        final EntityBurstEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.4f, 0.4f);
        float nextActivationExtraDR = 0f;

        DamageChargesDamperFieldScript(ShipAPI ship, float minC, float maxC, float minD, float maxD, float maxT, String id) {
            this.ship = ship;
            minChargePerHit = minC;
            maxChargePerHit = maxC;
            minDRPerHit = minD;
            maxDRPerHit = maxD;
            maxTotalDR = maxT;
            this.id = id;
            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), new Color(255, 222, 200, 255), 6, 0f, 1f);
            emitter.layer = CombatEngineLayers.BELOW_SHIPS_LAYER;
            emitter.fadeInFrac = 0.5f;
            emitter.fadeOutFrac = 0.5f;
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void advanceWhileOn(float amount) {
            if (nextActivationExtraDR > 0f) {
                float effectLevel = ship.getSystem().getEffectLevel();
                MutableShipStatsAPI stats = ship.getMutableStats();
                stats.getHullDamageTakenMult().modifyMult(id, 1f - nextActivationExtraDR * effectLevel);
                stats.getArmorDamageTakenMult().modifyMult(id, 1f - nextActivationExtraDR * effectLevel);
                stats.getEmpDamageTakenMult().modifyMult(id, 1f - nextActivationExtraDR * effectLevel);

                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/damper_field.png",
                        Strings.Descriptions.DamageChargesDamperFieldTitle,
                        String.format(Strings.Descriptions.DamageChargesDamperFieldDesc1, Utils.asPercentNoDecimal(nextActivationExtraDR * effectLevel)),
                        false);
            }
        }

        @Override
        public void onFullyDeactivate() {
            nextActivationExtraDR = 0f;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getEmpDamageTakenMult().unmodify(id);
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (target != ship) return;
            if (ship.getSystem().isActive()) return;
            if (ship.getParamAboutToApplyDamage() instanceof BeamAPI) return;

            float totalDamage = result.getDamageToShields() + result.getTotalDamageToArmor() + result.getDamageToHull();
            if (totalDamage <= 0f) return;

            if (ship.getSystem().getAmmo() < ship.getSystem().getMaxAmmo()) {
                float chargeFrac = (float) Math.log(totalDamage / 100f + 1f) / 60f;
                chargeFrac = Math.min(maxChargePerHit, Math.max(minChargePerHit, chargeFrac));

                float reloadProgress = ship.getSystem().getAmmoReloadProgress();
                float newReloadProgress = Math.min(1f, reloadProgress + chargeFrac);
                ship.getSystem().setAmmoReloadProgress(newReloadProgress);
            }
            else {
                float drFrac = (float) Math.log(totalDamage / 1000f + 1f) / 60f;
                drFrac = Math.min(maxDRPerHit, Math.max(minDRPerHit, drFrac));
                nextActivationExtraDR += drFrac;
                nextActivationExtraDR = Math.min(maxTotalDR, nextActivationExtraDR);
            }
        }

        @Override
        public void advance(float amount) {
            if (nextActivationExtraDR > 0f) {
                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    emitter.alphaMult = nextActivationExtraDR;
                    emitter.width = nextActivationExtraDR * 20f;
                    emitter.widthGrowth = -emitter.width;
                    Particles.burst(emitter, 6);
                }
                if (!ship.getSystem().isActive()) {
                    Utils.maintainStatusForPlayerShip(
                            ship,
                            id,
                            "graphics/icons/hullsys/damper_field.png",
                            Strings.Descriptions.DamageChargesDamperFieldTitle,
                            String.format(
                                    Strings.Descriptions.DamageChargesDamperFieldDesc2,
                                    Utils.asPercentNoDecimal(nextActivationExtraDR)),
                            false);
                }
            }
        }
    }
}
