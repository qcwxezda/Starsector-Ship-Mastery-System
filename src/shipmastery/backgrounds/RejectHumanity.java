package shipmastery.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import org.magiclib.achievements.MagicAchievementManager;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class RejectHumanity extends BaseCharacterBackground {

    public static final float OFFICER_REDUCTION = 1f;
    public static final float CREWED_CR_REDUCTION = 1f;
    public static final float MAX_DP_BONUS = 0.1f;
    public static final String MODIFIER_ID = "sms_RejectHumanityBackground";
    public static final String IS_REJECT_HUMANITY_START = "$sms_IsRejectHumanityBackground";

    @Override
    public boolean canBeSelected(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return isUnlocked();
    }

    @Override
    public String getShortDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (isUnlocked()) {
            return super.getShortDescription(factionSpec, factionConfig);
        }
        return Strings.Campaign.questionMarks;
    }

    public boolean isUnlocked() {
        var achievement = MagicAchievementManager.getInstance().getAchievement("sms_PseudocoreCrewedShip");
        return achievement != null && achievement.isComplete();
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        var achievement = MagicAchievementManager.getInstance().getAchievement("sms_PseudocoreCrewedShip");
        String name = achievement == null ? Strings.Campaign.unknown : achievement.getName();
        tooltip.addPara(Strings.Backgrounds.cannotSelectBackground, -20f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), name);
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
        if (forIntel || isUnlocked()) {
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc1, -15f);
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc2, 10f);
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc3, 10f, Misc.getNegativeHighlightColor(), Utils.asPercent(OFFICER_REDUCTION));
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc4, 0f, Misc.getNegativeHighlightColor(), Utils.asPercent(CREWED_CR_REDUCTION));
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc5, 0f, Misc.getHighlightColor(), Global.getSettings().getSpecialItemSpec("sms_pseudocore_uplink_mk2").getName());
            tooltip.addPara(Strings.Backgrounds.rejectHumanityDesc6, 10f, Misc.getHighlightColor(), Utils.asPercent(MAX_DP_BONUS), Global.getSettings().getSkillSpec("best_of_the_best").getName());
        }
    }

    @Override
    public void onNewGame(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        Global.getSector().getPersistentData().put(IS_REJECT_HUMANITY_START, true);
    }

    @Override
    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        BackgroundUtils.setOfficerNumberToZero();
        var cargo = Global.getSector().getPlayerFleet().getCargo();
        cargo.addSpecial(new SpecialItemData("sms_pseudocore_uplink_mk2", null), 1f);
        cargo.addCommodity("sms_beta_pseudocore", 1f);
        Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().forEach(
                member -> {
                    if (member.getCaptain() != null && !member.getCaptain().isPlayer() && !member.getCaptain().isDefault() && !member.getCaptain().isAICore()) {
                        member.setCaptain(null);
                    }
                }
        );
    }
}
