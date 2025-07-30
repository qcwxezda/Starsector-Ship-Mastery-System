package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.CargoScreenListener;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import shipmastery.backgrounds.BackgroundUtils;
import shipmastery.campaign.items.PseudocorePlugin;
import shipmastery.util.Strings;
import shipmastery.util.VariantLookup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerFleetHandler implements ColonyInteractionListener, ShipRecoveryListener, CoreUITabListener,
                                           EconomyTickListener, CargoScreenListener {

    public PlayerFleetHandler() {
        Global.getSector().getListenerManager().addListener(this, true);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {}
    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {}
    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {}

    @Override
    public void reportEconomyMonthEnd() {}

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        addMasteryHandlerToPlayerFleet();
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object o) {
        if (id == CoreUITabId.FLEET || id == CoreUITabId.REFIT) {
            addMasteryHandlerToPlayerFleet();
            if (BackgroundUtils.isRejectHumanityStart()) {
                BackgroundUtils.setOfficerNumberToZero();
            }
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> fms, InteractionDialogAPI dialog) {
        addMasteryHandlerToPlayerFleet();
    }


    @Override
    public void reportEconomyTick(int tick) {
        // Also check periodically in case none of the above catch a ship being added to the player's fleet
        addMasteryHandlerToPlayerFleet();
    }

    public static void addMasteryHandlerToPlayerFleet() {
        for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            ShipVariantAPI variant = fm.getVariant();
            VariantLookup.VariantInfo variantInfo = VariantLookup.getVariantInfo(variant);
            if (!variant.hasHullMod(Strings.Hullmods.MASTERY_HANDLER)
                    || variantInfo == null
                    || variantInfo.fleet != Global.getSector().getPlayerFleet()
                    || variant.isStockVariant() || variant.isGoalVariant()) {
                fm.setVariant(FleetHandler.addHandlerMod(variant, variant, fm), false, true);
            }
            // Remove NPC mastery indicators if they exist
            for (int i = 1; i <= 15; i++) {
                fm.getVariant().removeMod("sms_npc_indicator" + i);
            }
        }
    }

    @Override
    public void reportCargoScreenOpened() {
        // Unlock codex stuff
        var unlockData = SharedUnlockData.get();

        Set<String> pseudocoreIdsInCargo = new HashSet<>();
        for (var stack : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
            if (!stack.isCommodityStack()) continue;
            var id = stack.getCommodityId();
            var spec = Global.getSettings().getCommoditySpec(id);
            // Only handle pseudocores, which have the scale auto pts tag
            if (spec == null || !spec.hasTag(PseudocorePlugin.SCALE_AUTOMATED_POINTS_TAG)) continue;
            pseudocoreIdsInCargo.add(id);
        }

        boolean save = false;
        for (var id : pseudocoreIdsInCargo) {
            var plugin = Misc.getAICoreOfficerPlugin(id);
            if (plugin == null) return;
            var person = plugin.createPerson(id, "player", Misc.random);
            for (var skill : person.getStats().getSkillsCopy()) {
                if (skill.getLevel() > 0f && skill.getSkill().hasTag(Tags.CODEX_UNLOCKABLE)) {
                    save |= unlockData.reportPlayerAwareOfSkill(skill.getSkill().getId(), false);
                }
            }
        }

        if (save) {
            SharedUnlockData.get().saveIfNeeded();
        }
    }

    @Override
    public void reportPlayerLeftCargoPods(SectorEntityToken entity) {}

    @Override
    public void reportPlayerNonMarketTransaction(PlayerMarketTransaction transaction, InteractionDialogAPI dialog) {}

    @Override
    public void reportSubmarketOpened(SubmarketAPI submarket) {}
}
