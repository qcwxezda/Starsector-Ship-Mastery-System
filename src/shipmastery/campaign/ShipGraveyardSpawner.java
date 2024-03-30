package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.util.MathUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShipGraveyardSpawner extends BaseCampaignEventListener implements ShipRecoveryListener, EveryFrameScript {

    protected Set<FleetMemberAPI> recoveredShips = new HashSet<>();
    protected boolean weaponsRecovered = false;

    public ShipGraveyardSpawner() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI winner, BattleAPI battle) {
        if (!battle.isPlayerInvolved()) return;
        if (!Settings.ENABLE_PLAYER_SHIP_GRAVEYARDS) return;

        InteractionDialogAPI currentDialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        Set<FleetMemberAPI> members = new HashSet<>(Global.getSector().getPlayerFleet().getFleetData().getSnapshot());

        if (currentDialog != null && currentDialog.getPlugin() instanceof FleetInteractionDialogPluginImpl) {
            FleetInteractionDialogPluginImpl plugin = (FleetInteractionDialogPluginImpl) currentDialog.getPlugin();
            FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
            // Pretend the player is the winning side, otherwise no ships are recoverable
            List<FleetMemberAPI> recoverable = context.getRecoverableShips(battle, battle.getPlayerCombined(), battle.getNonPlayerCombined());
            List<FleetMemberAPI> storyRecoverable = context.getStoryRecoverableShips();

            if (recoverable == null) recoverable = new ArrayList<>();
            if (storyRecoverable == null) storyRecoverable = new ArrayList<>();

            Set<String> seenIds = new HashSet<>();

            for (FleetMemberAPI member : recoverable) {
                if (!recoveredShips.contains(member) &&
                        members.contains(member) &&
                        !seenIds.contains(member.getId())) {
                    addDerelict(member, false);
                    seenIds.add(member.getId());
                }
            }
            for (FleetMemberAPI member : storyRecoverable) {
                if (!recoveredShips.contains(member) &&
                        members.contains(member) &&
                        !seenIds.contains(member.getId())) {
                    addDerelict(member, true);
                    seenIds.add(member.getId());
                }
            }
        }
    }

    public void addDerelict(FleetMemberAPI fm, boolean storyPointRecovery) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        DerelictShipEntityPlugin.DerelictShipData params =
                DerelictShipEntityPlugin.createVariant(null, Misc.random, 0f);
        params.ship.shipName = fm.getShipName();
        params.ship.fleetMemberId = fm.getId();
        ShipVariantAPI variant = fm.getVariant();
        if (weaponsRecovered) {
            variant = fm.getVariant().clone();
            for (String slot : variant.getNonBuiltInWeaponSlots()) {
                ((HullVariantSpec) variant).getWeapons().remove(slot);
            }
            for (int i = variant.getHullSpec().getBuiltInWings().size(); i < variant.getWings().size(); i++) {
                variant.setWingId(i, null);
            }
        }
        params.ship.variant = variant;
        params.ship.nameAlwaysKnown = true;
        params.ship.pruneWeapons = false;
        SectorEntityToken entity =
                BaseThemeGenerator.addSalvageEntity(Misc.random, playerFleet.getContainingLocation(), "wreck",
                                                    "neutral", params);
        entity.setSensorProfile(null);
        entity.setDiscoverable(null);
        Vector2f loc = MathUtils.randomPointInCircle(playerFleet.getLocation(), 300f);
        entity.setFixedLocation(loc.x, loc.y);
        ShipRecoverySpecial.ShipRecoverySpecialData data =
                ShipRecoverySpecial.getSpecialData(entity, null, true, false);
        if (data != null) {
            data.ships = new ArrayList<>();
            data.ships.add(params.ship);
            if (storyPointRecovery) {
                data.storyPointRecovery = true;
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        weaponsRecovered = false;
        recoveredShips.clear();
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
        weaponsRecovered = true;
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> list, InteractionDialogAPI interactionDialogAPI) {
        recoveredShips.addAll(list);
    }
}
