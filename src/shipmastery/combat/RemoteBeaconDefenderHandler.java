package shipmastery.combat;

import com.fs.starfarer.api.combat.AdmiralAIPlugin;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.CombatFleetManager;

import java.util.List;

public class RemoteBeaconDefenderHandler {
    public static void modifyAdmiralAI(CombatFleetManagerAPI manager, FleetMemberAPI mustDeployIfAble) {
        manager.getAdmiralAI().setDelegate(new AdmiralAIPlugin.AdmiralPluginDelegate() {
            @Override
            public void doAdditionalInitialDeployment() {

            }

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
}
