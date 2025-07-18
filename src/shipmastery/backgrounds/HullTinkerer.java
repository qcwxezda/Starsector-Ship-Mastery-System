package shipmastery.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import shipmastery.plugin.ModPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class HullTinkerer extends BaseCharacterBackground {
    public static final float COST_REDUCTION = 0.5f;
    public static final String IS_TINKERER_START = "$sms_IsTinkererBackground";

    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        addToTooltip(tooltip);
    }

    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        addToTooltip(tooltip);
    }

    public void addToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Backgrounds.tinkererDesc1, -15f);
        tooltip.addPara(Strings.Backgrounds.tinkererDesc2, 10f, Misc.getHighlightColor(),
                Utils.asInt(ModPlugin.originalMaxPermaMods));
        tooltip.addPara(Strings.Backgrounds.tinkererDesc3, 0f, Misc.getHighlightColor(), Utils.asPercent(COST_REDUCTION));
        tooltip.addPara(Strings.Backgrounds.tinkererDesc4, 0f);
        tooltip.addPara(Strings.Backgrounds.tinkererDesc5, 0f, Misc.getHighlightColor(),
                Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).getDisplayName());
    }

    @Override
    public void onNewGame(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        Global.getSector().getPersistentData().put(IS_TINKERER_START, true);
    }
}
