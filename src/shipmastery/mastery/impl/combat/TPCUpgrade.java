package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.ParticleBurstEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class TPCUpgrade extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.TPCUpgrade).params(Strings.Descriptions.TPCName,
                                                                                               Utils.asPercent(strength),
                                                                                               Utils.asPercent(10f * strength));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.TPCUpgradePost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asInt((getStrengthForPlayer() * 15f)));
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(TPCUpgradeScript.class)) {
            float strength = getStrengthForPlayer();
            ship.addListener(new TPCUpgradeScript(ship, strength, strength * 10f, strength * 15f, id));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(TPCUpgradeScript.class);
    }

    static class TPCUpgradeScript implements DamageDealtModifier {

        final ShipAPI ship;
        final float chance;
        final float damageBonus;
        final int regenAmount;
        final String id;
        final Color color = new Color(1f, 0.5f, 0.5f, 1f);

        TPCUpgradeScript(ShipAPI ship, float chance, float damageBonus, float regenAmount, String id) {
            this.ship = ship;
            this.chance = chance;
            this.damageBonus = damageBonus;
            this.id = id;
            this.regenAmount = (int) regenAmount;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI proj)) return null;
            if (!"tpc_shot".equals(proj.getProjectileSpecId())) return null;

            if (Math.random() <= chance) {
                damage.getModifier().modifyPercent(id, 100f * damageBonus);
                if (proj.getWeapon() != null) {
                    for (int i = 0; i < regenAmount; i++) {
                        proj.getWeapon().getAmmoTracker().addOneAmmo();
                    }
                }

                ParticleBurstEmitter burst = new ParticleBurstEmitter(pt);
                burst.size = 6f;
                burst.sizeJitter = 0.1f;
                burst.lifeJitter = 0.1f;
                burst.radiusJitter = 0.5f;
                burst.radius = 75f;
                burst.lengthMultiplierOverTime = 3f;
                burst.alpha = 0.6f;
                burst.alphaJitter = 0.4f;
                burst.color = color;
                burst.life = 0.6f;
                Particles.burst(burst, 100);
            }
            return null;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return getTPCMasterySelectionWeight(spec);
    }

    public static Float getTPCMasterySelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getBuiltInWeapons() == null) return null;
        for (String id : spec.getBuiltInWeapons().values()) {
            var weaponSpec = Global.getSettings().getWeaponSpec(id);
            if (weaponSpec != null && weaponSpec.getProjectileSpec() instanceof ProjectileSpecAPI pSpec) {
                if ("tpc_shot".equals(pSpec.getId())) {
                    return 1f;
                }
            }
        }
        return null;
    }
}
