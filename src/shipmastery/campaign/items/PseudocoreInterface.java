package shipmastery.campaign.items;

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public interface PseudocoreInterface extends AICoreOfficerPlugin {
    String getCommodityId();
    int getBaseLevel();
    void setPersonSkills(MutableCharacterStatsAPI stats, String factionId);
    float getBaseAIPointsMult();
    String getPortraitSpritePath();

    static PseudocoreInterface getPluginForPseudocore(String commodityId) {
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
}
