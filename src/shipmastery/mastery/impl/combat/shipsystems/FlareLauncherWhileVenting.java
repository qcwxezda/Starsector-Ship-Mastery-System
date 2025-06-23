package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FlareLauncherWhileVenting extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FlareLauncherWhileVenting).params(
                Utils.asFloatOneDecimal(getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FlareLauncherWhileVentingPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (getFlareWeapons(ship).isEmpty()) return;
        if (!ship.hasListenerOfClass(FlareLauncherWhileVentingScript.class)) {
            ship.addListener(new FlareLauncherWhileVentingScript(ship, getStrength(ship)));
        }
    }

    static List<WeaponAPI> getFlareWeapons(ShipAPI ship) {
        List<WeaponAPI> flareWeapons = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            Object projSpec = weapon.getSpec().getProjectileSpec();
            if (!(projSpec instanceof MissileSpecAPI)) continue;
            String type = ((MissileSpecAPI) projSpec).getTypeString();
            if ("FLARE_JAMMER".equals(type) || "FLARE_SEEKER".equals(type) || "FLARE".equals(type)) {
                flareWeapons.add(weapon);
            }
        }
        return flareWeapons;
    }

    /** Obfuscated, no way to get sound string */
    static String getFireSoundString(WeaponAPI weapon) {
        return "flarelauncher3".equals(weapon.getId()) ? "system_flare_launcher_active" : "launch_flare_1";
    }

    static class FlareLauncherWhileVentingScript implements AdvanceableListener {

        final ShipAPI ship;
        final IntervalUtil flareInterval;
        final List<WeaponAPI> flareWeapons;

        FlareLauncherWhileVentingScript(ShipAPI ship, float rate) {
            this.ship = ship;
            flareInterval = new IntervalUtil(1f / rate, 1f / rate);
            flareWeapons = getFlareWeapons(ship);
        }

        @Override
        public void advance(float amount) {
            if (!ship.getFluxTracker().isVenting()) {
                flareInterval.setElapsed(0f);
            }
            else {
                flareInterval.advance(amount);
                if (flareInterval.intervalElapsed()) {
                    for (WeaponAPI weapon : flareWeapons) {
                        Global.getCombatEngine().spawnProjectile(
                                ship,
                                weapon,
                                weapon.getId(),
                                weapon.getFirePoint(0),
                                weapon.getCurrAngle(),
                                ship.getVelocity());
                        Global.getCombatEngine().spawnMuzzleFlashOrSmoke(ship, weapon.getFirePoint(0), weapon.getSpec(), weapon.getCurrAngle());
                        Global.getSoundPlayer().playSound(getFireSoundString(weapon), 1f, 1f, weapon.getFirePoint(0), ship.getVelocity());
                    }
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isBuiltInMod(HullMods.SAFETYOVERRIDES)) return null;
        for (String id : spec.getBuiltInWeapons().values()) {
            WeaponSpecAPI wSpec = Global.getSettings().getWeaponSpec(id);
            Object pSpec = wSpec.getProjectileSpec();
            if (!(pSpec instanceof MissileSpecAPI)) continue;
            String type = ((MissileSpecAPI) pSpec).getTypeString();
            if ("FLARE_JAMMER".equals(type) || "FLARE_SEEKER".equals(type) || "FLARE".equals(type)) {
                return 3f;
            }
        }
        return null;
    }
}
