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
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class ShipGraveyardSpawner extends BaseCampaignEventListener implements ShipRecoveryListener, EveryFrameScript {

    protected final Set<FleetMemberAPI> recoveredShips = new HashSet<>();
    protected boolean weaponsRecovered = false;
    // Recovering *any* ship prevents *every* AI core on your disabled ships from dropping as cargo
    // Possibly/probably a vanilla bug?
    protected boolean shipsRecovered = false;

    protected final Map<FleetMemberAPI, PersonAPI> origAICaptains = new HashMap<>();
    public static final String AI_CORE_MEM_KEY = "$sms_RecoverableWreckAICoreID";


    public ShipGraveyardSpawner() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getCaptain() != null && member.getCaptain().isAICore()) {
                origAICaptains.put(member, member.getCaptain());
            }
        }
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI winner, BattleAPI battle) {
        if (!battle.isPlayerInvolved()) return;
        if (!Settings.ENABLE_PLAYER_SHIP_GRAVEYARDS) return;

        InteractionDialogAPI currentDialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        Set<FleetMemberAPI> members = new HashSet<>(Global.getSector().getPlayerFleet().getFleetData().getSnapshot());

        if (currentDialog != null && currentDialog.getPlugin() instanceof FleetInteractionDialogPluginImpl plugin) {
            FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
            // Have to restore AI core captains because they get removed, but only if the player chooses not to recover any ships????
            origAICaptains.forEach(FleetMemberAPI::setCaptain);

            // Pretend the player is the winning side, otherwise no ships are recoverable
            List<FleetMemberAPI> recoverable = context.getRecoverableShips(battle, battle.getPlayerCombined(), battle.getNonPlayerCombined());
            List<FleetMemberAPI> storyRecoverable = context.getStoryRecoverableShips();

            // Have to restore AI core captains *AGAIN* because getRecoverableShips removes them...
            origAICaptains.forEach(FleetMemberAPI::setCaptain);

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

        origAICaptains.clear();
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
        String aiCoreId = null;
        if (fm.getCaptain() != null && fm.getCaptain().isAICore() && (Misc.isUnremovable(fm.getCaptain()) || !weaponsRecovered || shipsRecovered)) {
            fm.getCaptain().getMemoryWithoutUpdate().set(Misc.KEEP_CAPTAIN_ON_SHIP_RECOVERY, true, 0f);
            params.ship.captain = fm.getCaptain();
            aiCoreId = fm.getCaptain().getAICoreId();
        } else {
            fm.setCaptain(null);
        }
        SectorEntityToken entity =
                BaseThemeGenerator.addSalvageEntity(Misc.random, playerFleet.getContainingLocation(), Entities.WRECK,
                                                    Global.getSector().getPlayerFaction().getId(), params);
        entity.setSensorProfile(null);
        entity.setDiscoverable(null);
        Vector2f loc = MathUtils.randomPointInCircle(playerFleet.getLocation(), 300f);
        entity.setFixedLocation(loc.x, loc.y);
        ShipRecoverySpecial.ShipRecoverySpecialData data =
                ShipRecoverySpecial.getSpecialData(entity, null, true, false);
        if (aiCoreId != null) {
            entity.getMemoryWithoutUpdate().set(AI_CORE_MEM_KEY, aiCoreId);
        }
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
        shipsRecovered = false;
        recoveredShips.clear();
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
        weaponsRecovered = true;
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> list, InteractionDialogAPI interactionDialogAPI) {
        recoveredShips.addAll(list);
        shipsRecovered = true;
    }
}
