package shipmastery.plugin;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import shipmastery.campaign.AutofitPluginSModOption;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;

public class SModAutofitCampaignPlugin extends BaseCampaignPlugin {
    private final RefitHandler refitHandler;
    public SModAutofitCampaignPlugin(RefitHandler refitHandler) {
        this.refitHandler = refitHandler;
    }

    @Override
    public PluginPick<AutofitPlugin> pickAutofitPlugin(FleetMemberAPI member) {
        if (isNotPlayerAndChangeAutofitEnabled(member)) return null;
        if (Settings.DISABLE_MAIN_FEATURES) return null;
        return new PluginPick<AutofitPlugin>(new AutofitPluginSModOption(refitHandler, false), PickPriority.MOD_GENERAL);
    }

    public static boolean isNotPlayerAndChangeAutofitEnabled(FleetMemberAPI member) {
        if (!Settings.ADD_SMOD_AUTOFIT_OPTION) return true;
        PersonAPI commander = null;
        if (member != null) {
            commander = member.getFleetCommanderForStats();
            if (commander == null) commander = member.getFleetCommander();
        }
        // Only affects player, as S-mod selection on NPC fleets is different
        return commander == null || !commander.isPlayer();
    }
}
