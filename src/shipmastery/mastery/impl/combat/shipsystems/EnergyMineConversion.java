package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.ai.EnergyMineAI;
import shipmastery.combat.listeners.ShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EnergyMineConversion extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init("placeholder text...");
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"mine_strike".equals(ship.getSystem().getId())) {
            return;
        }
        ship.addListener(new EnergyMineConversionScript(ship, id));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {

    }

    public static class EnergyMineConversionScript implements ShipSystemListener, AdvanceableListener {
        final ShipAPI ship;
        final String id;
        final CombatEngineAPI engine;
        final Set<MissileAPI> trackedMines = new HashSet<>();

        EnergyMineConversionScript(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
            engine = Global.getCombatEngine();
        }

        @Override
        public void onActivate(ShipAPI ship) {

        }

        @Override
        public void onDeactivate(ShipAPI ship) {}

        @Override
        public void onFullyActivate(ShipAPI ship) {
            // Start from the end of the projectile list -- the newly made mine is very likely to be at the
            // end of this list, so this for loop is actually O(1)-ish
            ArrayList<DamagingProjectileAPI> projectiles = (ArrayList<DamagingProjectileAPI>) engine.getProjectiles();
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                DamagingProjectileAPI proj = projectiles.get(i);
                if (ship == proj.getSource() && "minelayer_mine_heavy".equals(proj.getProjectileSpecId())) {
                    MissileAPI mine = (MissileAPI) engine.spawnProjectile(
                            ship,
                            null,
                            "sms_energyminelayer",
                            proj.getLocation(),
                            proj.getFacing(),
                            null);
                    mine.setMissileAI(new EnergyMineAI(mine));
                    // Copied from MineStrikeStats
                    Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(ship, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
                    float fadeInTime = 0.5F;
                    mine.fadeOutThenIn(fadeInTime);
                    float liveTime = 5.0F;
                    mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
                    Global.getCombatEngine().removeEntity(proj);
                    trackedMines.add(mine);
                    break;
                }
            }
        }

        @Override
        public void onFullyDeactivate(ShipAPI ship) {}

        @Override
        public void onGainedAmmo(ShipAPI ship) {}

        @Override
        public void onFullyCharged(ShipAPI ship) {}

        @Override
        public void advanceWhileOn(ShipAPI ship, float amount) {

        }

        @Override
        public void advance(float amount) {
            for (Iterator<MissileAPI> iterator = trackedMines.iterator(); iterator.hasNext(); ) {
                MissileAPI mine = iterator.next();
                if (!engine.isEntityInPlay(mine)) {
                    iterator.remove();
                    if (mine.getHitpoints() > 0f) {

                    }
                }
            }
        }
    }
}
