package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.OperationsCenter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class OperationsCenterBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.OperationsCenterBoost)
                                 .params(Global.getSettings().getHullModSpec(HullMods.OPERATIONS_CENTER).getDisplayName(), Utils.asPercent(getStrength(selectedModule)));    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null) return;
        StatBonus cpRate = ship.getMutableStats().getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT);
        MutableStat.StatMod mod = cpRate.getFlatBonus(
                OperationsCenter.MOD_ID);
        if (mod != null) {
            cpRate.modifyFlat(id, mod.getValue() * getStrength(ship));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.getMutableStats().getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).unmodify(id);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getOrdnancePoints(null) < Global.getSettings().getHullModSpec(HullMods.OPERATIONS_CENTER).getCostFor(spec.getHullSize())) return null;
        return 1f;
    }
}
