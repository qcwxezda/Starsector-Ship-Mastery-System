package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ECMPackageBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ECMPackageBoost)
                                 .params(Global.getSettings().getHullModSpec(HullMods.ECM).getDisplayName(), Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        MutableStat.StatMod mod = stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).getFlatBonus(HullMods.ECM);
        if (mod != null) {
            stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, mod.getValue() * getStrength(stats));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getOrdnancePoints(null) < Global.getSettings().getHullModSpec(HullMods.ECM).getCostFor(spec.getHullSize())) return null;
        return 0.75f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return fm.getVariant().hasHullMod(HullMods.ECM) ? super.getNPCWeight(fm) : 0f;
    }
}
