package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public class SkillEffectDescriptionWIthNegativeHighlight extends BaseSkillEffectDescription {

    protected Color nhc;
    protected Color dnhc;

    @Override
    public void init(MutableCharacterStatsAPI stats, SkillSpecAPI skill) {
        indent = BaseIntelPlugin.BULLET;
        tc = Misc.getTextColor();
        hc = Misc.getHighlightColor();
        dhc = Misc.setAlpha(hc, 155);
        nhc = Misc.getNegativeHighlightColor();
        dnhc = Misc.setAlpha(nhc, 155);
        alpha = 255;
        float level = stats.getSkillLevel(skill.getId());
        if (level <= 0) {
            tc = Misc.getGrayColor();
            hc = dhc;
            nhc = dnhc;
            alpha = 155;
        }
        dtc = Misc.getGrayColor();
    }
}
