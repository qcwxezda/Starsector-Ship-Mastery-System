package shipmastery.mastery.impl;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

@SuppressWarnings("unused")
public class EnhanceMasteryDescOnly extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        int enhances = MasteryUtils.getEnhanceCount(selectedFleetMember.getHullSpec());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EnhanceMasteryDescOnly).params(Utils.asPercent(MasteryUtils.ENHANCE_MASTERY_AMOUNT[enhances]));
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}
