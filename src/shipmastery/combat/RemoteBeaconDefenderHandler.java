package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AdmiralAIPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.CombatFleetManager;
import particleengine.Particles;
import shipmastery.campaign.skills.HiddenEffectScript;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.fx.OverlayEmitter;
import shipmastery.util.MathUtils;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.List;
import java.util.Set;

public class RemoteBeaconDefenderHandler extends BaseEveryFrameCombatPlugin implements ShipDestroyedListener, FleetMemberDeploymentListener {

    private final FleetMemberAPI enemyFlagship;
    private float strengthDurationMult = 1f;
    private static final float MAX_STRENGTH_DURATION_MULT = 2f;
    public static final String MODIFY_KEY_ENEMIES_KILLED = "sms_RemoteBeaconDefender_Destroyed";
    public static final String MODIFY_KEY_CR = "sms_RemoteBeaconDefender_CR";

    private void modifyAdmiralAI(CombatFleetManagerAPI manager, FleetMemberAPI mustDeployIfAble) {
        manager.getAdmiralAI().setDelegate(new AdmiralAIPlugin.AdmiralPluginDelegate() {
            @Override
            public void doAdditionalInitialDeployment() {}

            @Override
            public boolean allowedToDeploy(List<FleetMemberAPI> chosenSoFar, FleetMemberAPI member) {
                if (member == mustDeployIfAble) return true;
                if (chosenSoFar.contains(mustDeployIfAble)) return true;

                var reserves = ((CombatFleetManager) manager).getReserves();
                FleetMember fm = (FleetMember) mustDeployIfAble;
                return !reserves.contains(fm);
            }
        });
    }

    public RemoteBeaconDefenderHandler(FleetMemberAPI enemyFlagship) {
        this.enemyFlagship = enemyFlagship;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        modifyAdmiralAI(engine.getFleetManager(FleetSide.ENEMY), enemyFlagship);
    }

    @Override
    public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
        if (target.isFighter()) return;
        var ship = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getShipFor(enemyFlagship);
        if (ship == null || ship == target) return;

        if (target.getOriginalOwner() == ship.getOwner()) {
            float dist = MathUtils.dist(target.getLocation(), ship.getLocation());
            if (dist > 8000f) return;

            float increase = Utils.hullSizeToInt(target.getHullSize()) * 0.03f;
            strengthDurationMult = Math.min(MAX_STRENGTH_DURATION_MULT, Math.max(0f, strengthDurationMult + increase));
            var dynamicStats = ship.getMutableStats().getDynamic();
            dynamicStats.getMod(HiddenEffectScript.Provider.STRENGTH_MOD)
                    .modifyMult(MODIFY_KEY_ENEMIES_KILLED, strengthDurationMult);
            dynamicStats.getMod(HiddenEffectScript.Provider.DURATION_MOD)
                    .modifyMult(MODIFY_KEY_ENEMIES_KILLED, strengthDurationMult);

            var darkColor = new Color(50, 150, 100, 100);
            var brightColor = new Color(150, 250, 200);
            for (int i = 0; i < 5; i++) {
                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 4f;
                params.zigZagReductionFactor = 0.05f;
                params.minFadeOutMult = 1f;
                params.flickerRateMult = MathUtils.randBetween(0.1f, 0.12f);
                params.nonBrightSpotMinBrightness = 0f;

                float arcSize = 50f + MathUtils.randBetween(0f, 200f);
                var arc = Global.getCombatEngine().spawnEmpArcVisual(
                        target.getLocation(),
                        target,
                        ship.getLocation(),
                        ship, arcSize,
                        darkColor,
                        brightColor,
                        params);
                arc.setCoreWidthOverride(arcSize / 2f);
                arc.setSingleFlickerMode(true);
            }

            OverlayEmitter emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1.5f);
            emitter.color = brightColor;
            emitter.enableDynamicAnchoring();
            Particles.burst(emitter, 1);
        }
    }

    @Override
    public void reportFleetMemberDeployed(DeployedFleetMemberAPI member) {
        if (member.getMember() != enemyFlagship) return;
        var ship = member.getShip();
        var stats = ship.getMutableStats();
        stats.getCriticalMalfunctionChance().modifyMult(MODIFY_KEY_CR, 0f);
        stats.getWeaponMalfunctionChance().modifyMult(MODIFY_KEY_CR, 0f);
        stats.getEngineMalfunctionChance().modifyMult(MODIFY_KEY_CR, 0f);
        stats.getShieldMalfunctionChance().modifyMult(MODIFY_KEY_CR, 0f);
        stats.getDynamic().getMod(HiddenEffectScript.Provider.COOLDOWN_MOD).modifyMult(MODIFY_KEY_CR, ship.getCurrentCR());
        // Reset strength multiplier whenever the ship "dies"
        strengthDurationMult = 1f;
    }
}

