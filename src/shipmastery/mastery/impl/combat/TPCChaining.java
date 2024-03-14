package shipmastery.mastery.impl.combat;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.TargetChecker;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Collection;

public class TPCChaining extends BaseMasteryEffect {

    static final float CHAIN_DAMAGE_FRAC = 0.5f;
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.TPCChaining).params(Strings.Descriptions.TPCName);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        tooltip.addPara(Strings.Descriptions.TPCChainingPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(strength), Utils.asInt((strength * 20f)), Utils.asInt((strength * 2500f)));
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(TPCChainingScript.class)) {
            float strength = getStrengthForPlayer();
            ship.addListener(new TPCChainingScript(ship, strength, strength * 20f, strength * 2500f, id));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(TPCChainingScript.class);
    }

    static class TPCChainingScript implements DamageDealtModifier {

        final ShipAPI ship;
        final float damageBonus;
        final float chainCount;
        final float chainRange;
        final String id;
        final Color color = new Color(1f, 0.5f, 0.5f, 1f);

        TPCChainingScript(ShipAPI ship, float damageBonus, float chainCount, float chainRange, String id) {
            this.ship = ship;
            this.chainCount = chainCount;
            this.damageBonus = damageBonus;
            this.chainRange = chainRange;
            this.id = id;
        }

        @Override
        public String modifyDamageDealt(Object param, final CombatEntityAPI target, final DamageAPI damage, final Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI)) return null;
            final DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            if (!"tpc_shot".equals(proj.getProjectileSpecId())) return null;

            damage.getModifier().modifyPercent(id, 100f * damageBonus);

            final Collection<CombatEntityAPI> targets = EngineUtils.getKNearestEntities(
                    (int) chainCount,
                    pt,
                    null,
                    true,
                    chainRange,
                    true,
                    new TargetChecker() {
                        @Override
                        public boolean check(CombatEntityAPI entity) {
                            return entity != null
                                    && entity != target
                                    && Global.getCombatEngine().isEntityInPlay(entity)
                                    && entity.getHitpoints() > 0
                                    && entity.getOwner() != proj.getOwner()
                                    && entity.getOwner() != 100;
                        }
                    });

            for (CombatEntityAPI entity : targets) {
                Global.getCombatEngine().spawnEmpArc(proj.getSource(), pt, target, entity,
                    DamageType.ENERGY,
                    damage.getDamage() * CHAIN_DAMAGE_FRAC, // damage
                    0, // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    30f, // thickness
                    color,
                    Color.WHITE);
            }

            return null;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getBuiltInWeapons() == null) return null;
        for (String id : spec.getBuiltInWeapons().values()) {
            if ("tpc".equals(id)) {
                return 1f;
            }
        }
        return null;
    }
}
