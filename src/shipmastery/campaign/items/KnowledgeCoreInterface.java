package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public interface KnowledgeCoreInterface {
    String getCommodityId();
    int getBaseLevel();
    void setPersonSkills(MutableCharacterStatsAPI stats, String factionId);
    float getBaseAIPointsMult();
    String getPortraitSpritePath();
}
