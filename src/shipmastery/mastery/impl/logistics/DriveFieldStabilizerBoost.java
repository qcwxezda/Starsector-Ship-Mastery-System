package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DriveFieldStabilizerBoost extends BaseMasteryEffect {

    public int getBoost(float strength) {
        return AdditiveMasteryEffect.truncateExceptNearZero(strength);
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.DriveFieldStabilizerBoost).params(
                Global.getSettings().getHullModSpec("drive_field_stabilizer").getDisplayName(),
                Utils.asInt(getBoost(getStrength(selectedModule))));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || !stats.getVariant().hasHullMod("drive_field_stabilizer")) return;
        stats.getDynamic().getMod(Stats.FLEET_BURN_BONUS).modifyFlat(id, getBoost(getStrength(stats)));
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return spec.isBuiltInMod("drive_field_stabilizer") ? 1f : null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}

