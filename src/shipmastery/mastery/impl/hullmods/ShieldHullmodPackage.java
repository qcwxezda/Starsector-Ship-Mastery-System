package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ShieldHullmodPackage extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.ShieldHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.ACCELERATED_SHIELDS),
                Utils.getHullmodName(HullMods.EXTENDED_SHIELDS),
                Utils.getHullmodName(HullMods.HARDENED_SHIELDS),
                Utils.getHullmodName(HullMods.STABILIZEDSHIELDEMITTER),
                Utils.asPercent(getStrength(selectedModule))
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.ACCELERATED_SHIELDS, false),
                new HullmodData(HullMods.EXTENDED_SHIELDS, false),
                new HullmodData(HullMods.HARDENED_SHIELDS, false),
                new HullmodData(HullMods.STABILIZEDSHIELDEMITTER, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 3;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getShieldUpkeepMult().modifyMult(id, 0f);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - getStrength(stats));
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!Utils.hasShield(spec)) return null;
        return super.getSelectionWeight(spec);
    }
}
