package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class FMRRegen extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FMRRegen).params(getSystemName(),
                                                                                             Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(FMRRegenScript.class)) {
            ship.addListener(new FMRRegenScript(ship, getStrength(ship)));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FMRRegenPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, "" + 1);
    }

    @Override
    public String getSystemSpecId() {
        return "fastmissileracks";
    }

    static class FMRRegenScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float regenFrac;
        final Set<WeaponAPI> missileWeapons = new HashSet<>();


        FMRRegenScript(ShipAPI ship, float regenFrac) {
            this.ship = ship;
            this.regenFrac = regenFrac;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (WeaponAPI.WeaponType.MISSILE.equals(weapon.getType()) && weapon.getSpec().getMaxAmmo() >= 1) {
                    missileWeapons.add(weapon);
                }
            }

            ship.getSystem().setAmmo(Math.min(ship.getSystem().getAmmo(), 1));
        }

        @Override
        public void onActivate() {
            for (WeaponAPI weapon : missileWeapons) {
                int maxAmmo = weapon.getSpec().getMaxAmmo();
                float regen = maxAmmo * regenFrac;
                int regenAmount = (int) regen;
                float chanceRegenAdditional = regen - regenAmount;

                if (Math.random() <= chanceRegenAdditional) {
                    regenAmount++;
                }

                weapon.setAmmo(Math.min(weapon.getMaxAmmo(), weapon.getAmmo() + regenAmount));
            }
        }

        @Override
        public void onGainedAmmo() {
            ship.getSystem().setAmmo(1);
        }
    }
}
