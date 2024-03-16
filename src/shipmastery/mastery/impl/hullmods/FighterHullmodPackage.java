package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class FighterHullmodPackage extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.FighterHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.EXPANDED_DECK_CREW),
                Utils.getHullmodName(HullMods.RECOVERY_SHUTTLES),
                Utils.asPercent(getStrength(selectedModule))
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.EXPANDED_DECK_CREW, false),
                new HullmodData(HullMods.RECOVERY_SHUTTLES, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 2;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getFighterRefitTimeMult().modifyMult(id, 1f - getStrength(stats));
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getFighterBays() <= 0) return null;
        return super.getSelectionWeight(spec);
    }
}
