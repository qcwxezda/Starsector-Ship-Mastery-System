package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class EnableSModRemoval extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.init(Strings.ENABLE_SMOD_REMOVAL);
    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_REMOVAL_ENABLED = true;
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_REMOVAL_ENABLED = false;
    }

    @Override
    public boolean isUniqueEffect() {
        return true;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.ENABLE_SMOD_REMOVAL_POST, 5f).setAlignment(Alignment.MID);
    }
}
