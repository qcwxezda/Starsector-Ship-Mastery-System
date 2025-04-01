package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class HullHullmodPackage extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.HullHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.REINFORCEDHULL),
                Utils.getHullmodName(HullMods.BLAST_DOORS),
                Utils.asPercent(getStrength(selectedModule))
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.REINFORCEDHULL, false),
                new HullmodData(HullMods.BLAST_DOORS, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 2;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float strength = getStrength(stats);
        stats.getHullDamageTakenMult().modifyMult(id, 1f - strength);
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - strength);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
