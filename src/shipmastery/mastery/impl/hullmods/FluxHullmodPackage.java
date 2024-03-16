package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class FluxHullmodPackage extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.FluxHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.FLUX_COIL),
                Utils.getHullmodName(HullMods.FLUX_DISTRIBUTOR),
                Utils.getHullmodName(HullMods.FLUXBREAKERS),
                Utils.asPercent(getStrength(selectedModule))
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.FLUX_COIL, false),
                new HullmodData(HullMods.FLUX_DISTRIBUTOR, false),
                new HullmodData(HullMods.FLUXBREAKERS, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 3;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float strength = getStrength(stats);
        stats.getFluxCapacity().modifyPercent(id, 100f * strength);
        stats.getFluxDissipation().modifyPercent(id, 100f * strength);
    }
}
