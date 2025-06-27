package shipmastery.campaign.items;

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public interface KCoreInterface extends AICoreOfficerPlugin {
    String getCommodityId();
    int getBaseLevel();
    void setPersonSkills(MutableCharacterStatsAPI stats, String factionId);
    float getBaseAIPointsMult();
    String getPortraitSpritePath();
}
