package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import particleengine.Particles;
import shipmastery.combat.ai.LOSMissileAI;
import shipmastery.config.Settings;
import shipmastery.fx.TrailEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TorpedoTracking extends BaseMasteryEffect {

    public static final float SPEED_MULT = 0.8f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.TorpedoTracking)
                                 .params(Utils.asPercent(getStrengthForPlayer() / 16f), Utils.asPercent(1f - SPEED_MULT))
                                 .colors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.TorpedoTrackingPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.oneDecimalPlaceFormat.format(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(TorpedoTrackingScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new TorpedoTrackingScript(ship, strength, strength / 16f, id));
        }
    }

    static class TorpedoTrackingScript implements AdvanceableListener {

        final ShipAPI ship;
        final float turnRate;
        final float hpBoost;
        final String id;
        final Set<WeaponAPI> reaperWeapons = new HashSet<>();

        TorpedoTrackingScript(ShipAPI ship, float turnRate, float hpBoost, String id) {
            this.ship = ship;
            this.turnRate = turnRate;
            this.hpBoost = hpBoost;
            this.id = id;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                Object spec = weapon.getSpec().getProjectileSpec();
                if (spec instanceof MissileSpecAPI) {
                    String name = ((MissileSpecAPI) spec).getHullSpec().getHullId();
                    if ("reaper_torp".equals(name) || "hammer_torp".equals(name)) {
                        reaperWeapons.add(weapon);
                    }
                }
            }
        }

        @Override
        public void advance(float amount) {
            boolean isFiring = false;
            for (WeaponAPI weapon : reaperWeapons) {
                if (weapon.getChargeLevel() >= 0.999f) {
                    isFiring = true;
                    break;
                }
            }

            if (isFiring) {
                float r = 2f*ship.getCollisionRadius();
                Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), r, r);
                while (itr.hasNext()) {
                    Object o = itr.next();
                    if (!(o instanceof MissileAPI)) continue;
                    final MissileAPI missile = (MissileAPI) o;
                    if (!reaperWeapons.contains(missile.getWeapon())) continue;
                    if (missile.getCustomData() == null || !missile.getCustomData().containsKey(id)) {
                        missile.setCustomData(id, true);
                        TrailEmitter trail = new TrailEmitter(missile);
                        Color explosionColor = missile.getSpec().getExplosionColor();
                        trail.color = new Color(explosionColor.getRed() / 255f, explosionColor.getGreen() / 255f, explosionColor.getBlue() / 255f, 0.3f);
                        trail.length = 40f;
                        trail.width = 20f;
                        trail.lifeJitter = 0.5f;
                        trail.sizeJitter = 0.2f;
                        trail.saturationChangeOverLife = -0.5f;
                        trail.life = 0.5f;
                        trail.randomXOffset = 5f;
                        trail.randomAngleDegrees = 20f;
                        trail.yOffset = -trail.length * 0.25f;
                        Particles.stream(trail, 1, 60f, -1f, new Particles.StreamAction<TrailEmitter>() {
                            @Override
                            public boolean apply(TrailEmitter emitter) {
                                return !Utils.wasProjectileRemoved(missile) && !missile.isFizzling();
                            }
                        });

                        missile.getEngineStats().getMaxTurnRate().setBaseValue(0f);
                        missile.getEngineStats().getTurnAcceleration().setBaseValue(0f);
                        missile.getEngineStats().getMaxTurnRate().modifyFlat(id, turnRate);
                        missile.getEngineStats().getTurnAcceleration().modifyFlat(id, turnRate * 10f);
                        missile.getEngineStats().getMaxSpeed().modifyMult(id, SPEED_MULT);
                        missile.setHitpoints(missile.getHitpoints() * (1f + hpBoost));
                        missile.setMass(missile.getMass() * 2f);
                        missile.setMaxFlightTime(missile.getMaxFlightTime() / SPEED_MULT);

                        // Could just use MissileAI, but then would have to set the missile's spec to alwaysAccelerate,
                        // which is a global permanent change
                        missile.setMissileAI(new LOSMissileAI(missile, 3f));
                    }
                }
            }
        }
    }
}
