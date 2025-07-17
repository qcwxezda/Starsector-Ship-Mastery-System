package shipmastery.backgrounds;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import org.magiclib.achievements.MagicAchievementManager;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class RejectHumanity extends BaseCharacterBackground {

    public static final float OFFICER_REDUCTION = 1f;
    public static final float CREWED_CR_REDUCTION = 0.5f;

    @Override
    public boolean canBeSelected(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return isUnlocked();
    }

    public boolean isUnlocked() {
        var achievement = MagicAchievementManager.getInstance().getAchievement("sms_PseudocoreCrewedShip");
        return achievement != null && achievement.isComplete();
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        tooltip.addPara(Strings.Backgrounds.rejectHumanityCannotSelect, Misc.getNegativeHighlightColor(), 0f);
    }

    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        addToTooltip(tooltip, true);
    }

    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        addToTooltip(tooltip, false);
    }

    public void addToTooltip(TooltipMakerAPI tooltip, boolean forIntel) {
        tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc1, -15f);
        if (!isUnlocked() && !forIntel) return;
        tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc2, 10f);
        tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc3, 10f, Misc.getNegativeHighlightColor(), Utils.asPercent(OFFICER_REDUCTION));
        tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc4, 0f, Misc.getNegativeHighlightColor(), Utils.asPercent(CREWED_CR_REDUCTION));
        tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc5, 0f, Misc.getHighlightColor(), "Pseudocore Uplink Mk. 2");
    }
}
