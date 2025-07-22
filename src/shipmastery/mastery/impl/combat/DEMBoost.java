package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.dem.DEMEffect;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DEMBoost extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.DEMBoost);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedVariant);
        tooltip.addPara(
                Strings.Descriptions.DEMBoostPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedVariant, WeaponAPI.WeaponSize.SMALL)),
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedVariant, WeaponAPI.WeaponSize.MEDIUM)),
                Utils.asFloatOneDecimal(1f / getRegenRate(selectedVariant, WeaponAPI.WeaponSize.LARGE)),
                Utils.asPercent(50f * strength),
                Utils.asPercent(25f * strength));
    }

    public float getRegenRate(ShipVariantAPI variant, WeaponAPI.WeaponSize slotSize) {
        return switch (slotSize) {
            case SMALL -> getStrength(variant);
            case MEDIUM -> 1.5f * getStrength(variant);
            case LARGE -> 2f * getStrength(variant);
        };
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(DEMBoostScript.class)) {
            ship.addListener(new DEMBoostScript(ship, 1f + 50f*getStrength(ship), 25f*getStrength(ship), id));
        }
    }

    class DEMBoostScript implements ProjectileCreatedListener {

        final ShipAPI ship;
        float hpMult;
        final float speedIncrease;
        final String id;

        DEMBoostScript(ShipAPI ship, float hpMult, float speedIncrease, String id) {
            this.ship = ship;
            this.hpMult = hpMult;
            this.speedIncrease = speedIncrease;
            this.id = id;
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!(proj instanceof MissileAPI missile)) return;
            if (!(missile.getSpec().getOnFireEffect() instanceof DEMEffect)) return;
            WeaponAPI weapon = missile.getWeapon();
            if (weapon != null) {
                if (weapon.getAmmoPerSecond() <= 0f) {
                    int burstSize = weapon.getSpec().getBurstSize();
                    weapon.getAmmoTracker().setReloadSize(burstSize);
                    weapon.getAmmoTracker().setAmmoPerSecond(burstSize * getRegenRate(ship.getVariant(), weapon.getSlot().getSlotSize()));
                }
                if (weapon.getSlot() != null && weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.LARGE) {
                    missile.setHitpoints(missile.getHitpoints() * hpMult);
                    missile.getEngineStats().getMaxSpeed().modifyPercent(id, 100f * speedIncrease);
                    missile.getEngineStats().getAcceleration().modifyPercent(id, 100f * speedIncrease);
                    missile.getEngineStats().getDeceleration().modifyPercent(id, 100f * speedIncrease);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f);
        if (weight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 0.4f, 1f);
    }
}
