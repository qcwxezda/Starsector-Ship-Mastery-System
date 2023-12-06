package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BuiltInMissileRegen extends BaseMasteryEffect {
    public static final int NUM_SALVOS_REGEN = 1;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BuiltInMissileRegen)
                                 .params(NUM_SALVOS_REGEN, Utils.asFloatOneDecimal(1f / getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getUsableWeapons()) {
            if (weapon.getSlot().isBuiltIn() && WeaponAPI.WeaponType.MISSILE.equals(weapon.getType())) {
                if (weapon.getAmmoPerSecond() <= 0f) {
                    int numPerSalvo = weapon.getSpec().getBurstSize();
                    weapon.getAmmoTracker().setReloadSize(numPerSalvo);
                    weapon.getAmmoTracker().setAmmoPerSecond(numPerSalvo * getStrength(ship));
                }
            }
        }
    }
}
