package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class SModRemoval extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.SModRemoval);
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.SMOD_REMOVAL_ENABLED = true;
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.SMOD_REMOVAL_ENABLED = false;
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        if (Settings.CLEAR_SMODS_REFUND_FRACTION <= 0f) {
            tooltip.addPara(Strings.Descriptions.SModRemovalPost, 0f);
        }
        else {
            tooltip.addPara(
                    Strings.Descriptions.SModRemovalPostWithRefund,
                    0f,
                    Misc.getTextColor(),
                    Utils.asPercent(Settings.CLEAR_SMODS_REFUND_FRACTION));
        }
    }
}
