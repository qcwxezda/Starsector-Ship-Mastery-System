package shipmastery.plugin;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import shipmastery.campaign.CoreAutofitPluginExt;
import shipmastery.campaign.RefitHandler;

public class SModAutofitCampaignPlugin extends BaseCampaignPlugin {
    private final RefitHandler refitHandler;
    public SModAutofitCampaignPlugin(RefitHandler refitHandler) {
        this.refitHandler = refitHandler;
    }

    @Override
    public PluginPick<AutofitPlugin> pickAutofitPlugin(FleetMemberAPI member) {
        PersonAPI commander = null;
        if (member != null) {
            commander = member.getFleetCommanderForStats();
            if (commander == null) commander = member.getFleetCommander();
        }
        // Only affects player, as S-mod selection on NPC fleets is different
        if (commander == null || !commander.isPlayer()) return null;
        return new PluginPick<AutofitPlugin>(new CoreAutofitPluginExt(commander, refitHandler), PickPriority.MOD_GENERAL);
    }
}
