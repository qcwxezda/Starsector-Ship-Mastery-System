package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DriveFieldStabilizerBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.DriveFieldStabilizerBoost).params(
                Global.getSettings().getHullModSpec("drive_field_stabilizer").getDisplayName(),
                Utils.asInt(getStrength(selectedModule))
                                                                                                             );
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || !stats.getVariant().hasHullMod("drive_field_stabilizer")) return;
        stats.getDynamic().getMod(Stats.FLEET_BURN_BONUS).modifyFlat(id, (int) getStrength(stats));
    }
}
