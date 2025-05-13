package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class GroundSupportBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.GroundSupportBoost).params(
                Global.getSettings().getHullModSpec("ground_support").getDisplayName(),
                Global.getSettings().getHullModSpec("advanced_ground_support").getDisplayName(),
                Utils.asInt(getStrength(selectedModule))
                                                                                                             );
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null ||
                (!stats.getVariant().hasHullMod("ground_support") &&
                        !stats.getVariant().hasHullMod("advanced_ground_support"))) return;
        stats.getDynamic().getMod(Stats.FLEET_GROUND_SUPPORT).modifyFlat(id, getStrength(stats));
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isBuiltInMod("ground_support") || spec.isBuiltInMod("advanced_ground_support")) return 1f;
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}
