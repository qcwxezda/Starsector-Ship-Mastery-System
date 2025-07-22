package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FasterActiveFlares extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FasterActiveFlares).params(
                Utils.asPercentNoDecimal(getStrength(selectedVariant)));
    }

    static List<WeaponAPI> getActiveFlareWeapons(ShipAPI ship) {
        List<WeaponAPI> flareWeapons = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            Object projSpec = weapon.getSpec().getProjectileSpec();
            if (!(projSpec instanceof MissileSpecAPI)) continue;
            String type = ((MissileSpecAPI) projSpec).getTypeString();
            if ("FLARE_SEEKER".equals(type)) {
                flareWeapons.add(weapon);
            }
        }
        return flareWeapons;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (getActiveFlareWeapons(ship).isEmpty()) return;
        if (!ship.hasListenerOfClass(FasterActiveFlaresScript.class)) {
            ship.addListener(new FasterActiveFlaresScript(ship, getStrength(ship), id));
        }
    }

    record FasterActiveFlaresScript(ShipAPI ship, float strength, String id) implements ProjectileCreatedListener {
        @Override
            public void reportProjectileCreated(DamagingProjectileAPI proj) {
                if (!(proj instanceof MissileAPI missile)) return;
                if (!"FLARE_SEEKER".equals(missile.getSpec().getTypeString())) return;

                // Seeker flare AI needs 0.5 seconds elapsed to start working and starts anywhere from -0.5 to 0
                // seconds elapsed
                missile.getMissileAI().advance(1f);
                missile.getEngineStats().getAcceleration().modifyPercent(id, 100f * strength);
                missile.getEngineStats().getTurnAcceleration().modifyPercent(id, 100f * strength);
                missile.getEngineStats().getMaxTurnRate().modifyPercent(id, 100f * strength);
                missile.getEngineStats().getDeceleration().modifyPercent(id, 100f * strength);
                missile.setHitpoints(missile.getMaxHitpoints() * (1f + strength));
            }
        }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        for (String id : spec.getBuiltInWeapons().values()) {
            WeaponSpecAPI wSpec = Global.getSettings().getWeaponSpec(id);
            Object pSpec = wSpec.getProjectileSpec();
            if (!(pSpec instanceof MissileSpecAPI)) continue;
            String type = ((MissileSpecAPI) pSpec).getTypeString();
            if ("FLARE_SEEKER".equals(type)) {
                return 3f;
            }
        }
        return null;
    }
}
