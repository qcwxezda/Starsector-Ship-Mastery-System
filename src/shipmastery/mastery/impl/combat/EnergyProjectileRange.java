package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EnergyProjectileRange extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EnergyProjectileRange).params(Utils.asInt(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new EnergyProjectileRangeScript(getStrength(ship)));
    }

    record EnergyProjectileRangeScript(float bonus) implements WeaponBaseRangeModifier {
        @Override
            public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
                return 0f;
            }

            @Override
            public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
                return 1f;
            }

            @Override
            public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
                return WeaponAPI.WeaponType.ENERGY.equals(weapon.getType()) && !weapon.isBeam() ? bonus : 0f;
            }
        }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.getDominantWeaponTypes(spec).contains(WeaponAPI.WeaponType.ENERGY)) return null;
        return 1f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        float score = 0f;
        for (String slot : fm.getVariant().getFittedWeaponSlots()) {
            var weapon = fm.getVariant().getWeaponSpec(slot);
            if (weapon != null && WeaponAPI.WeaponType.ENERGY.equals(weapon.getType()) && !weapon.isBeam()) {
                score += switch (weapon.getSize()) {
                    case SMALL -> 1f;
                    case MEDIUM -> 2f;
                    case LARGE -> 4f;
                };
            }
        }
        return Math.min(2f, score / 3f);
    }
}
