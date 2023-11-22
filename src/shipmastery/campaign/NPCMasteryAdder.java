package shipmastery.campaign;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.ShipMasteryNPC;

import java.util.Map;
import java.util.NavigableMap;

public class NPCMasteryAdder extends BaseCampaignEventListener {
    public NPCMasteryAdder(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
//        SectorEntityToken target = dialog.getInteractionTarget();
//        if (!(target instanceof CampaignFleetAPI)) {
//            return;
//        }
//
//        CampaignFleetAPI fleet = (CampaignFleetAPI) target;
//
//        if (ShipMasteryNPC.FLEET_MASTERY_CACHE.containsKey(fleet.getId())) {
//            return;
//        }
//
//        ShipMasteryNPC.generateMasteryLevelsForNPCFleet(fleet);
//        FleetDataAPI fleetData = fleet.getFleetData();
//        if (fleetData == null) return;
//
//        Map<ShipHullSpecAPI, NavigableMap<Integer, Boolean>> masteries = ShipMasteryNPC.FLEET_MASTERY_CACHE.get(fleet.getId());
//        if (masteries == null) return;
//
//        for (FleetMemberAPI fm : fleetData.getMembersListCopy()) {
//            NavigableMap<Integer, Boolean> levels = masteries.get(fm.getHullSpec());
//            if (levels == null || levels.isEmpty()) continue;
//            int maxLevel = levels.lastEntry().getKey();
//            if (maxLevel < 1) continue;
//            maxLevel = Math.min(maxLevel, 9);
////            if (fm.getVariant().isStockVariant()) {
////                fm.setVariant(fm.getVariant().clone(), false, false);
////                fm.getVariant().setSource(VariantSource.REFIT);
////            }
//            fm.getVariant().addPermaMod("sms_npcIndicator" + maxLevel, false);
 //       }
    }
}
