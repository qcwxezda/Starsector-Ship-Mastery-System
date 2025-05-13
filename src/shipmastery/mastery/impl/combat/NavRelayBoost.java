package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class NavRelayBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.NavRelayBoost)
                                 .params(Global.getSettings().getHullModSpec(HullMods.NAV_RELAY).getDisplayName(), Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        MutableStat.StatMod mod = stats.getDynamic().getMod(Stats.COORDINATED_MANEUVERS_FLAT).getFlatBonus(
                HullMods.NAV_RELAY);
        if (mod != null) {
            stats.getDynamic().getMod(Stats.COORDINATED_MANEUVERS_FLAT).modifyFlat(id, mod.getValue() * getStrength(stats));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getOrdnancePoints(null) < Global.getSettings().getHullModSpec(HullMods.NAV_RELAY).getCostFor(spec.getHullSize())) return null;
        return 1f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        if (fm.getVariant().hasHullMod(HullMods.NAV_RELAY)) {
            return 2f*super.getNPCWeight(fm);
        }
        return 0f;
    }
}
