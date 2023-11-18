package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class SModRemoval extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.init(Strings.Descriptions.SModRemoval);
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_REMOVAL_ENABLED = true;
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_REMOVAL_ENABLED = false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Descriptions.SModRemovalPost, 5f);
    }
}
