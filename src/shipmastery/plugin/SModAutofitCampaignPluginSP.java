package shipmastery.plugin;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import shipmastery.campaign.AutofitPluginSModOption;
import shipmastery.config.Settings;

public class SModAutofitCampaignPluginSP extends BaseCampaignPlugin {
    @Override
    public PluginPick<AutofitPlugin> pickAutofitPlugin(FleetMemberAPI member) {
        if (SModAutofitCampaignPlugin.isNotPlayerAndChangeAutofitEnabled(member)) return null;
        if (!Settings.DISABLE_MAIN_FEATURES) return null;
        return new PluginPick<>(new AutofitPluginSModOption(null, true), PickPriority.MOD_GENERAL);
    }
}
