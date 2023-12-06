package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

import java.util.*;

public class RandomBeamIntensity extends BaseMasteryEffect {

    public static final float BEAM_WIDTH_MULT = 1.5f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return null;
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(RandomBeamIntensityScript.class)) {
            ship.addListener(new RandomBeamIntensityScript(ship, 0.1f, getStrength(ship), 5f, id));
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
            if (!active && Math.random() <= amount * chancePerSecond) {
                active = true;
                activeDuration = 0f;
            }

            if (active) {
                float effectLevel = Math.min(1f, Math.min(activeDuration, activationLength - activeDuration));
                for (WeaponAPI weapon : beamWeapons) {
                    for (BeamAPI beam : weapon.getBeams()) {
                        if (!originalWidths.containsKey(beam)) {
                            originalWidths.put(beam, beam.getWidth());
                        }
                        else {
                            beam.setWidth(originalWidths.get(beam) * (1f + (BEAM_WIDTH_MULT - 1f) * effectLevel));
                        }
                    }
                }
                ship.getMutableStats().getBeamWeaponDamageMult().modifyPercent(id, 100f * damageIncrease * effectLevel);

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
}
