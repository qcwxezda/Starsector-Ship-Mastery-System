package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PseudocorePlugin extends AICoreOfficerPlugin {
    String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    String DEFAULT_PERSONALITY_ID = "aggressive";
    String SHARED_KNOWLEDGE_ID = "sms_shared_knowledge";
    String CRYSTALLINE_KNOWLEDGE_ID = "sms_crystalline_knowledge";
    String WARPED_KNOWLEDGE_ID = "sms_warped_knowledge";
    String AMORPHOUS_KNOWLEDGE_ID = "sms_amorphous_knowledge";
    String SCALE_AUTOMATED_POINTS_TAG = "sms_scale_auto_pts";

    String getCommodityId();
    int getLevel();
    List<String> getPrioritySkills();
    float getAIPointsMult();
    String getPortraitSpritePath();

    static PseudocorePlugin getPluginForPseudocore(String commodityId) {
        if (commodityId == null) return null;
        return switch (commodityId) {
            case "sms_alpha_pseudocore" -> new AlphaPseudocorePlugin();
            case "sms_warped_pseudocore" -> new WarpedPseudocorePlugin();
            case "sms_crystalline_pseudocore" -> new CrystallinePseudocorePlugin();
            case "sms_beta_pseudocore" -> new BetaPseudocorePlugin();
            case "sms_gamma_pseudocore" -> new GammaPseudocorePlugin();
            case "sms_fractured_gamma_core" -> new FracturedGammaCorePlugin();
            case "sms_amorphous_pseudocore" -> new AmorphousPseudocorePlugin();
            default -> null;
        };
    }

    class Handler implements PlayerFleetSyncListener, CoreTabListener {
        @Override
        public void onCoreTabOpened(CoreUITabId id) {}

        @Override
        public void onCoreUIDismissed() {
            onPlayerFleetSync();
        }

        @Override
        public void onPlayerFleetSync() {
            var fleet = Global.getSector().getPlayerFleet();
            Map<String, AICoreOfficerPlugin> plugins = new HashMap<>();
            for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                PersonAPI captain = fm.getCaptain();
                String id = captain == null ? null : captain.getAICoreId();
                if (id == null) continue;
                var spec = Global.getSettings().getCommoditySpec(id);
                if (spec.hasTag(COPY_PERSONALITY_TAG)) {
                    setPersonalityToPlayerDoctrine(captain);
                }
                if (spec.hasTag(SCALE_AUTOMATED_POINTS_TAG)) {
                    float ratio = fm.getUnmodifiedDeploymentPointsCost() / fm.getDeploymentPointsCost();
                    var plugin = plugins.computeIfAbsent(id, Misc::getAICoreOfficerPlugin);
                    var memory = captain.getMemoryWithoutUpdate();
                    if (memory != null && plugin instanceof PseudocorePlugin kPlugin) {
                        float baseMult = kPlugin.getAIPointsMult();
                        // Special behavior for amorphous cores
                        if ("sms_amorphous_pseudocore".equals(id)) {
                            int points = (int) ShipMastery.getPlayerMasteryPoints(fm.getHullSpec());
                            int groups = (int) (points / AmorphousPseudocorePlugin.MP_PER_GROUP);
                            baseMult = Math.max(1f, baseMult - groups*AmorphousPseudocorePlugin.DP_MULT_PER_MP_GROUP);
                        }
                        memory.set("$autoPointsMult", baseMult * ratio);
                    }
                }
            }
        }
    }

    static String getPlayerPersonalityId() {
        var personalityPicker = Global.getSector().getPlayerFaction().getPersonalityPicker();
        if (personalityPicker != null && !personalityPicker.isEmpty()) {
            return personalityPicker.getItems().get(0);
        } else {
            return DEFAULT_PERSONALITY_ID;
        }
    }

    static void setPersonalityToPlayerDoctrine(PersonAPI person) {
        person.setPersonality(getPlayerPersonalityId());
    }

}
