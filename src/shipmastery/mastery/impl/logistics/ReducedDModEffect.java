package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class ReducedDModEffect extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.ReducedDModEffectPos,
                Strings.Descriptions.ReducedDModEffectNeg,
                true,
                true, getStrength(selectedVariant));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT).modifyMult(id, getMult(stats));
        // Need to reapply d-mods due to ordering issues
        if (stats.getVariant() != null) {
            stats.getVariant().getHullMods().forEach(id -> {
                var spec = Global.getSettings().getHullModSpec(id);
                if (spec.hasTag(Tags.HULLMOD_DMOD)) {
                    spec.getEffect().applyEffectsBeforeShipCreation(hullSize, stats, id);
                }
            });
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return 0.5f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        int count = (int) fm.getVariant().getHullMods()
                .stream()
                .filter(x -> Global.getSettings().getHullModSpec(x).hasTag(Tags.HULLMOD_DMOD))
                .count();
        return super.getNPCWeight(fm)*0.5f*count;
    }
}
