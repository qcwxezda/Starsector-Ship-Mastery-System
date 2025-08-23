package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class FluxHullmodPackage extends HullmodPackage {

    public static final float REQ_NOT_MET_MULT = 0.3f;

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.FluxHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipVariantAPI selectedVariant) {
        return new String[] {
                Utils.getHullmodName(HullMods.FLUX_COIL),
                Utils.getHullmodName(HullMods.FLUX_DISTRIBUTOR),
                Utils.getHullmodName(HullMods.FLUXBREAKERS),
                Utils.asPercentNoDecimal(getStrength(selectedVariant))
        };
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FluxHullmodPackagePost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(getStrength(selectedVariant)*REQ_NOT_MET_MULT));
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
        stats.getFluxCapacity().modifyPercent(id, 100f*strength);
        stats.getFluxDissipation().modifyPercent(id, 100f*strength);
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float strength = getStrength(stats);
        stats.getFluxCapacity().modifyPercent(id, 100f*strength*REQ_NOT_MET_MULT);
        stats.getFluxDissipation().modifyPercent(id, 100f*strength*REQ_NOT_MET_MULT);
    }
}
