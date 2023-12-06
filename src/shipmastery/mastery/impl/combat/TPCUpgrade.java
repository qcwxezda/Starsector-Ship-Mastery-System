package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
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
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.TPCUpgrade).params(Strings.Descriptions.TPCName,
                                                                                               Utils.asPercent(strength),
                                                                                               Utils.asPercent(10f * strength));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
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
            if (!(param instanceof DamagingProjectileAPI)) return null;
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
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
}
