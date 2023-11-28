package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;

/** Known issue: extra bay(s) don't show up in the fleet screen */
public class ConvertedHangarBays extends AdditiveMasteryEffect {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant().hasHullMod(HullMods.CONVERTED_HANGAR)) {
            stats.getNumFighterBays().modifyFlat(id, getIncrease(stats));
        }
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        // Remove fighter wings that are no longer allowed due to decreased limit
        for (FleetMemberAPI fm : Utils.getMembersNoSync(Global.getSector().getPlayerFleet())) {
            if (!getHullSpec().equals(fm.getHullSpec())) continue;
            ShipVariantAPI variant = fm.getVariant();
            List<String> wingIds = variant.getWings();
            if (wingIds != null && !wingIds.isEmpty()) {
                for (int i = fm.getStats().getNumFighterBays().getModifiedInt(); i < wingIds.size(); i++) {
                    variant.setWingId(i, null);
                }
            }
        }
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ConvertedHangarBays).params(Strings.Descriptions.ConvertedHangarName, getIncreasePlayer());
    }
}
