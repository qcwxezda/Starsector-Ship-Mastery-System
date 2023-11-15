package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class EnableSModRemoval extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return MasteryDescription.init(Strings.ENABLE_SMOD_REMOVAL);
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
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.ENABLE_SMOD_REMOVAL_POST, 5f);
    }
}
