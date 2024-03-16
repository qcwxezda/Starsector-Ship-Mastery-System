package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.BeamParticleEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomBeamIntensity extends BaseMasteryEffect {

    public static final float BEAM_WIDTH_MULT = 1.3f;

    public float getDamageIncrease(ShipAPI ship) {
        return getStrength(ship);
    }

    public float getDuration(ShipAPI ship) {
        return getStrength(ship) * 6f;
    }

    public float getChancePerSecond(ShipAPI ship) {
        return getStrength(ship) * 0.2f;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RandomBeamIntensity).params(
                Utils.asInt(getStrength(selectedModule) * 200f),
                Utils.asPercent(getDamageIncrease(selectedModule)),
                Utils.asFloatOneDecimal(getDuration(selectedModule)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getBeamWeaponRangeBonus().modifyFlat(id, getStrength(stats) * 200f);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.RandomBeamIntensityPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(getChancePerSecond(selectedModule)));
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(RandomBeamIntensityScript.class)) {
            ship.addListener(new RandomBeamIntensityScript(ship, getChancePerSecond(ship), getStrength(ship), getDuration(ship), id));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(RandomBeamIntensityScript.class);
    }

    static class RandomBeamIntensityScript implements AdvanceableListener {
        final ShipAPI ship;
        final float chancePerSecond;
        final float damageIncrease;
        final float activationLength;
        final String id;
        boolean active = false;
        float activeDuration = 0f;
        final List<WeaponAPI> beamWeapons = new ArrayList<>();
        final Map<BeamAPI, Float> originalWidths = new HashMap<>();
        final IntervalUtil checkInterval = new IntervalUtil(0.25f, 0.25f);

        RandomBeamIntensityScript(ShipAPI ship, float chancePerSecond, float damageIncrease, float activationLength, String id) {
            this.ship = ship;
            this.chancePerSecond = chancePerSecond;
            this.damageIncrease = damageIncrease;
            this.id = id;
            this.activationLength = activationLength;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (weapon.isBeam()) {
                    beamWeapons.add(weapon);
                }
            }
        }

        @Override
        public void advance(float amount) {
            if (!active) {
                checkInterval.advance(amount);
                if (checkInterval.intervalElapsed()) {
                    boolean beamIsFiring = false;
                    for (WeaponAPI weapon : beamWeapons) {
                        if (weapon.isFiring()) {
                            beamIsFiring = true;
                            break;
                        }
                    }
                    if (beamIsFiring && Math.random() <= chancePerSecond * checkInterval.getIntervalDuration()) {
                        active = true;
                        activeDuration = 0f;
                    }
                }
            }

            if (active) {
                // 0.25 second chargeup / chargedown time
                float effectLevel = Math.min(1f, Math.min(activeDuration * 4f, (activationLength - activeDuration) * 4f));
                for (WeaponAPI weapon : beamWeapons) {
                    for (BeamAPI beam : weapon.getBeams()) {
                        if (!originalWidths.containsKey(beam)) {
                            originalWidths.put(beam, beam.getWidth());
                        }
                        else {
                            beam.setWidth(originalWidths.get(beam) * (1f + (BEAM_WIDTH_MULT - 1f) * effectLevel));
                        }

                        // Creating this many emitter instances is fine for non-dynamically-tracked emitters
                        BeamParticleEmitter emitter = new BeamParticleEmitter(beam);
                        Particles.burst(emitter, (int) (beam.getLength() / 300f));
                    }
                }
                ship.getMutableStats().getBeamWeaponDamageMult().modifyPercent(id, 100f * damageIncrease * effectLevel);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/high_energy_focus.png",
                        Strings.Descriptions.RandomBeamIntensityTitle,
                        String.format(Strings.Descriptions.RandomBeamIntensityDesc1, Utils.asPercentNoDecimal(damageIncrease * effectLevel)),
                        false);

                activeDuration += amount;
                if (activeDuration > activationLength) {
                    active = false;
                    for (Map.Entry<BeamAPI, Float> entry : originalWidths.entrySet()) {
                        entry.getKey().setWidth(entry.getValue());
                    }
                    originalWidths.clear();
                    ship.getMutableStats().getBeamWeaponDamageMult().unmodify(id);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.getDominantWeaponTypes(spec).contains(WeaponAPI.WeaponType.ENERGY)) return null;
        return 1f;
    }
}
