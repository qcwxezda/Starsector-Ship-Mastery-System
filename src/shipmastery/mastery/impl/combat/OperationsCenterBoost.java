package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
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
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.OperationsCenterBoost)
                                 .params(Global.getSettings().getHullModSpec(HullMods.OPERATIONS_CENTER).getDisplayName(), Utils.asPercent(getStrength(selectedVariant)));    }

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
        return 0.75f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        // I think NPCs can make use of command point recovery, maybe...?
        if (fm.getVariant().hasHullMod(HullMods.OPERATIONS_CENTER)) {
            return super.getNPCWeight(fm);
        }
        return 0f;
    }
}
