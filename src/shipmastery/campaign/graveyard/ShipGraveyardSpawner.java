package shipmastery.campaign.graveyard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.util.MathUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShipGraveyardSpawner extends BaseCampaignEventListener implements ShipRecoveryListener, EveryFrameScript {

    protected final Set<FleetMemberAPI> recoveredShips = new HashSet<>();
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

        if (currentDialog != null && currentDialog.getPlugin() instanceof FleetInteractionDialogPluginImpl plugin) {
            FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
            // Pretend the player is the winning side, otherwise no ships are recoverable
            List<FleetMemberAPI> recoverable = context.getRecoverableShips(battle, battle.getPlayerCombined(), battle.getNonPlayerCombined());
            List<FleetMemberAPI> storyRecoverable = context.getStoryRecoverableShips();

            if (recoverable == null) recoverable = new ArrayList<>();
            if (storyRecoverable == null) storyRecoverable = new ArrayList<>();

            int lastNoStoryPoint = recoverable.size();
            recoverable.addAll(storyRecoverable);

            Set<String> seenIds = new HashSet<>();
            List<Pair<FleetMemberAPI, SectorEntityToken>> lostInfo = new ArrayList<>();

            for (int i = 0; i < recoverable.size(); i++) {
                FleetMemberAPI member = recoverable.get(i);
                if (!recoveredShips.contains(member) &&
                        members.contains(member) &&
                        !seenIds.contains(member.getId())) {
                    lostInfo.add(new Pair<>(member, addDerelict(member, i >= lastNoStoryPoint)));
                    seenIds.add(member.getId());
                }
            }

            if (!lostInfo.isEmpty()) {
                Global.getSector().getIntelManager().addIntel(new ShipGraveyardIntel(lostInfo));
            }
        }
    }

    public SectorEntityToken addDerelict(FleetMemberAPI fm, boolean storyPointRecovery) {
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
        params.ship.addDmods = false;
        params.ship.nameAlwaysKnown = true;
        params.ship.pruneWeapons = false;
        params.canHaveExtraCargo = false;
        SectorEntityToken entity =
                BaseThemeGenerator.addSalvageEntity(Misc.random, playerFleet.getContainingLocation(), Entities.WRECK,
                                                    Global.getSector().getPlayerFaction().getId(), params);
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
        return entity;
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
