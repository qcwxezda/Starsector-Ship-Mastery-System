package shipmastery.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import org.magiclib.achievements.MagicAchievementManager;
import shipmastery.ShipMastery;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class Enlightened extends BaseCharacterBackground implements CoreUITabListener {

    public static final float NPC_MASTERY_BOOST = 0.5f;
    public static final int PSEUDOCORE_EXTRA_LEVELS = 1;
    public static final String MODIFIER_ID = "sms_EnlightenedBackground";
    public static final String IS_ENLIGHTENED_START = "$sms_IsEnlightenedBackground";
    public static final String PROCESSED_HULLS_SPECS_KEY = "$sms_ProcessedHullSpecs";

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
        var achievement = MagicAchievementManager.getInstance().getAchievement("sms_MasteredMany");
        return achievement != null && achievement.isComplete();
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        var achievement = MagicAchievementManager.getInstance().getAchievement("sms_MasteredMany");
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
            tooltip.addPara(Strings.Backgrounds.enlightenedDesc1, -15f);
            tooltip.addPara(Strings.Backgrounds.enlightenedDesc2, 10f);
            tooltip.addPara(Strings.Backgrounds.enlightenedDesc3, 0f, Misc.getHighlightColor(), Utils.asPercent(NPC_MASTERY_BOOST));
            tooltip.addPara(Strings.Backgrounds.enlightenedDesc4, 0f, Misc.getHighlightColor(), Utils.asInt(PSEUDOCORE_EXTRA_LEVELS));
        }
    }

    @Override
    public void onNewGame(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        Global.getSector().getPersistentData().put(IS_ENLIGHTENED_START, true);
        Global.getSector().getPersistentData().put(PROCESSED_HULLS_SPECS_KEY, new HashSet<String>());
        Global.getSector().getListenerManager().addListener(this, false);
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (tab != CoreUITabId.FLEET && tab != CoreUITabId.REFIT) return;

        //noinspection unchecked
        var processed = (Set<String>) Global.getSector().getPersistentData().get(PROCESSED_HULLS_SPECS_KEY);
        var processing = new HashSet<String>();

        Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()
                .stream()
                .map(x -> Utils.getRestoredHullSpec(x.getHullSpec()))
                .distinct()
                .filter(x -> !processed.contains(x.getHullId()))
                .forEach(x -> {
                    processing.add(x.getHullId());
                    int maxLevel = ShipMastery.getMaxMasteryLevel(x);
                    float req = 0f;
                    for (int i = 0; i < maxLevel; i++) {
                        req += MasteryUtils.getUpgradeCost(i);
                    }
                    ShipMastery.addPlayerMasteryPoints(x,
                            req,
                            false,
                            false,
                            ShipMastery.MasteryGainSource.OTHER);
                });

        processed.addAll(processing);
    }
}
