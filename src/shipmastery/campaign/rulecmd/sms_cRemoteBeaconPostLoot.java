package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.GateTransitListener;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class sms_cRemoteBeaconPostLoot extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var globalMem = memoryMap.get(MemKeys.GLOBAL);
        if (!globalMem.getBoolean(Strings.Campaign.TRIGGERED_REMOTE_BEACON_DEFENDERS)) {
            globalMem.set(Strings.Campaign.TRIGGERED_REMOTE_BEACON_DEFENDERS, true);
            DeferredActionPlugin.performOnUnpause(() -> DeferredActionPlugin.performLater(new InitiateDefenderFleet(), 1f));
        }
        return true;
    }

    public static class InitiateDefenderFleet implements Action {
        @Override
        public void perform() {
            String commanderId = Strings.Campaign.COMMANDER_PREFIX + Misc.genUID();
            FleetParamsV3 fParams = new FleetParamsV3(
                    null,
                    null,
                    "sms_curator",
                    2f,
                    Strings.Campaign.REMOTE_BEACON_DEFENDER_FLEET_TYPE,
                    1000f, 0f, 0f, 0f, 0f, 0f, 0f);
            fParams.withOfficers = true;
            fParams.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA;
            fParams.maxNumShips = 60;
            fParams.doNotPrune = true;
            fParams.random = new Random(commanderId.hashCode());
            fParams.modeOverride = FactionAPI.ShipPickMode.PRIORITY_ONLY;
            fParams.addShips = new ArrayList<>();
            fParams.addShips.add("sms_tesseract_RemoteBeacon");

            var fleet = FleetFactoryV3.createFleet(fParams);
            fleet.getInflater().setRemoveAfterInflating(false);
            fleet.setName(Strings.Campaign.singularity);
            fleet.clearAbilities();
            fleet.getFleetData().sort();
            var commander = fleet.getCommander();
            if (commander != null) {
                commander.setId(commanderId);
            }

            var playerFleet = Global.getSector().getPlayerFleet();
            fleet.setLocation(playerFleet.getLocation().x, playerFleet.getLocation().y);
            fleet.setNoAutoDespawn(true);
            playerFleet.getContainingLocation().addEntity(fleet);

            var mem = fleet.getMemoryWithoutUpdate();
            mem.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            mem.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            mem.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
            mem.set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
            mem.set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new FIDConfigGen());
            Global.getSector().getMemoryWithoutUpdate().set(Strings.Campaign.REMOTE_BEACON_DEFENDER_FLEET, fleet);

            var script = new DefendersDefeatedListener(fleet);
            Global.getSector().getListenerManager().addListener(script, false);
            Global.getSector().addListener(script);
            Global.getSector().addScript(script);
        }
    }

    public static class FIDConfigGen implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        @Override
        public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
            FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
            config.alwaysAttackVsAttack = true;
            config.pullInStations = false;
            config.delegate = new FIDDelegate();
            return config;
        }

        // Not sure how xstream will handle the anonymous classes, just make an inner class
        public static class FIDDelegate extends FleetInteractionDialogPluginImpl.BaseFIDDelegate {
            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.objectivesAllowed = true;
                bcc.aiRetreatAllowed = false;
                bcc.fightToTheLast = true;
            }
        }
    }

    public static class DefendersDefeatedListener extends BaseCampaignEventListener implements EveryFrameScript, GateTransitListener {
        final CampaignFleetAPI toTrack;
        float removeTimer = 10f;

        public DefendersDefeatedListener(CampaignFleetAPI toTrack) {
            super(true);
            this.toTrack = toTrack;
        }

        @Override
        public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
            // We don't despawn this fleet, but some other mod might
            if (fleet == toTrack) {
                DeferredActionPlugin.performLater(() -> Global.getSector().removeListener(this), 0f);
            }
        }

        @Override
        public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
            var battle = plugin.getBattle();
            if (battle == null) return;
            var nonPlayer = battle.getNonPlayerSide();
            if (nonPlayer == null) return;
            if (!nonPlayer.contains(toTrack)) return;
            if (toTrack.isEmpty()) {
                loot.addCommodity("sms_amorphous_core", 1);
                DeferredActionPlugin.performLater(() -> Global.getSector().removeListener(this), 0f);
            }
        }

        @Override
        public boolean isDone() {
            return toTrack.isEmpty();
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            var playerLoc = Global.getSector().getPlayerFleet().getContainingLocation();
            if (!toTrack.isInCurrentLocation() && toTrack.getContainingLocation() != null) {
                removeTimer -= amount;
                if (removeTimer <= 0f) {
                    toTrack.getContainingLocation().removeEntity(toTrack);
                    toTrack.setContainingLocation(null);
                }
            }
        }

        @Override
        public void reportFleetTransitingGate(CampaignFleetAPI fleet, SectorEntityToken gateFrom, SectorEntityToken gateTo) {
            if (toTrack.isEmpty()) {
                DeferredActionPlugin.performLater(() -> Global.getSector().getListenerManager().removeListener(this), 0f);
                return;
            }
            if (!fleet.isPlayerFleet()) return;
            // Mods might add gates in hyperspace
            if (gateTo == null || gateTo.isInHyperspace()) return;
            if (gateTo.getContainingLocation().hasTag(Tags.THEME_CORE)) return;
            if (gateTo.getContainingLocation().hasTag(Tags.THEME_HIDDEN)) return;
            if (fleet.getContainingLocation() == toTrack.getContainingLocation()) return;
            if (Misc.random.nextFloat() <= 0.85f) return;

            Vector2f loc = MathUtils.randomPointInRing(gateTo.getLocation(), 1000f, 2000f);
            removeTimer = 10f;
            toTrack.setLocation(loc.x, loc.y);
            if (toTrack.getContainingLocation() != null) {
                toTrack.getContainingLocation().removeEntity(toTrack);
            }
            gateTo.getContainingLocation().addEntity(toTrack);
            toTrack.getAI().addAssignment(FleetAssignment.INTERCEPT, fleet, 1000f, null);
        }
    }
}
