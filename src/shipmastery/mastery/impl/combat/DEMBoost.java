package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.dem.DEMEffect;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DEMBoost extends BaseMasteryEffect {

    public static final String PROCESSED_MISSILE_KEY = "sms_processed_" + DEMBoost.class.getSimpleName();

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.DEMBoost);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        tooltip.addPara(
                Strings.Descriptions.DEMBoostPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedModule, WeaponAPI.WeaponSize.SMALL)),
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedModule, WeaponAPI.WeaponSize.MEDIUM)),
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedModule, WeaponAPI.WeaponSize.LARGE)),
                Utils.asPercent(50f * strength),
                Utils.asPercent(25f * strength));
    }

    public float getRegenRate(ShipAPI ship, WeaponAPI.WeaponSize slotSize) {
        return switch (slotSize) {
            case SMALL -> getStrength(ship);
            case MEDIUM -> 1.5f * getStrength(ship);
            case LARGE -> 2f * getStrength(ship);
        };
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        boolean hasDEM = false;
        List<WeaponAPI> largeDemWeapons = new ArrayList<>();
        for (WeaponAPI weapon : ship.getUsableWeapons()) {
            if (weapon.getAmmoPerSecond() > 0f) continue;
            Object projSpec = weapon.getSpec().getProjectileSpec();
            if (projSpec instanceof MissileSpecAPI missileSpec) {
                boolean isDEM = missileSpec.getOnFireEffect() instanceof DEMEffect;
                // Absolutely no way to get projectile spec from spec id string (in this case the "hydra_warhead" in
                // MIRV behavior spec, so have to hard code this
                if ("hydra".equals(missileSpec.getHullSpec().getHullId())) {
                    isDEM = true;
                }
                if (isDEM) {
                    int burstSize = weapon.getSpec().getBurstSize();
                    weapon.getAmmoTracker().setReloadSize(burstSize);
                    weapon.getAmmoTracker().setAmmoPerSecond(burstSize * getRegenRate(ship, weapon.getSlot().getSlotSize()));
                    if (WeaponAPI.WeaponSize.LARGE.equals(weapon.getSlot().getSlotSize())) {
                        largeDemWeapons.add(weapon);
                    }
                    hasDEM = true;
                }
            }
        }
        if (hasDEM && !ship.hasListenerOfClass(DEMBoostScript.class)) {
            ship.addListener(new DEMBoostScript(ship, largeDemWeapons, 1f + 50f*getStrength(ship), 25f*getStrength(ship), id));
        }
    }

    static class DEMBoostScript implements AdvanceableListener {

        final ShipAPI ship;
        final List<WeaponAPI> largeDemWeapons;
        final float hpMult;
        final float speedIncrease;
        final String id;

        DEMBoostScript(ShipAPI ship, List<WeaponAPI> largeDemWeapons, float hpMult, float speedIncrease, String id) {
            this.ship = ship;
            this.largeDemWeapons = largeDemWeapons;
            this.hpMult = hpMult;
            this.speedIncrease = speedIncrease;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            for (WeaponAPI weapon : largeDemWeapons) {
                if (weapon.getChargeLevel() >= 0.999f) {
                    Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(weapon.getLocation(), 100f, 100f);
                    while (itr.hasNext()) {
                        Object o = itr.next();
                        if (!(o instanceof MissileAPI missile)) continue;
                        if (missile.getSource() != ship) continue;
                        if (missile.getCustomData() == null || !missile.getCustomData().containsKey(PROCESSED_MISSILE_KEY)) {
                            missile.setCustomData(PROCESSED_MISSILE_KEY, true);
                            missile.setHitpoints(missile.getHitpoints() * hpMult);
                            missile.getEngineStats().getMaxSpeed().modifyPercent(id, 100f * speedIncrease);
                            missile.getEngineStats().getAcceleration().modifyPercent(id, 100f * speedIncrease);
                            missile.getEngineStats().getDeceleration().modifyPercent(id, 100f * speedIncrease);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.sm + wsc.mm + wsc.lm;
        if (count <= 0f) return null;
        return 1.6f * Utils.getSelectionWeightScaledByValue(count, 4f, false);
    }
}
