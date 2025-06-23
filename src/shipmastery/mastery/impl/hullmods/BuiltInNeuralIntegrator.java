package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BuiltInNeuralIntegrator extends HullmodPackage {

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.BuiltInNeuralIntegrator;
    }

    @Override
    protected String[] getDescriptionParams(ShipVariantAPI selectedVariant) {
        return new String[] {
                Utils.getHullmodName(HullMods.NEURAL_INTEGRATOR),
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.NEURAL_INTEGRATOR, true)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 1;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(HullMods.NEURAL_INTEGRATOR);
        stats.getSuppliesToRecover().unmodify(HullMods.NEURAL_INTEGRATOR);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!spec.isBuiltInMod(HullMods.AUTOMATED)) return null;
        return super.getSelectionWeight(spec);
    }
}
