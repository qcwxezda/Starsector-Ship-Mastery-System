package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FasterActiveFlares extends BaseMasteryEffect {

    public static final String PROCESSED_KEY = "sms_processed_" + FasterActiveFlares.class.getSimpleName();

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FasterActiveFlares).params(
                Utils.asPercentNoDecimal(getStrength(selectedModule)));
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

    static class FasterActiveFlaresScript implements AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        final String id;
        final IntervalUtil checkInterval = new IntervalUtil(0.5f, 0.5f);

        FasterActiveFlaresScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            checkInterval.advance(amount);

            if (checkInterval.intervalElapsed()) {
                Iterator<Object> search = Global.getCombatEngine().getAllObjectGrid()
                                                .getCheckIterator(ship.getLocation(),
                                                                  2f * ship.getCollisionRadius() + 200f,
                                                                  2f * ship.getCollisionRadius() + 200f);
                while (search.hasNext()) {
                    Object o = search.next();
                    if (!(o instanceof MissileAPI missile)) continue;
                    if (missile.getSource() != ship) continue;
                    if (missile.getCustomData().containsKey(PROCESSED_KEY)) continue;
                    if (!"FLARE_SEEKER".equals(missile.getSpec().getTypeString())) continue;

                    // Seeker flare AI needs 0.5 seconds elapsed to start working and starts anywhere from -0.5 to 0
                    // seconds elapsed
                    missile.getMissileAI().advance(1f);
                    missile.getEngineStats().getAcceleration().modifyPercent(id, 100f * strength);
                    missile.getEngineStats().getTurnAcceleration().modifyPercent(id, 100f * strength);
                    missile.getEngineStats().getMaxTurnRate().modifyPercent(id, 100f * strength);
                    missile.getEngineStats().getDeceleration().modifyPercent(id, 100f * strength);
                    missile.setHitpoints(missile.getMaxHitpoints() * (1f + strength));
                    missile.setCustomData(PROCESSED_KEY, true);
                }
            }
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
