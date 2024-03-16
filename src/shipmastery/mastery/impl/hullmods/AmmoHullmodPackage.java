package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AmmoHullmodPackage extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.AmmoHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.MISSLERACKS),
                Utils.getHullmodName(HullMods.MAGAZINES),
                Utils.asPercent(getStrength(selectedModule))
        };
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.MISSLERACKS, false),
                new HullmodData(HullMods.MAGAZINES, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 2;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float strength = getStrength(stats);
        stats.getBallisticAmmoBonus().modifyPercent(id, 100f * strength);
        stats.getEnergyAmmoBonus().modifyPercent(id, 100f * strength);
        stats.getMissileAmmoBonus().modifyPercent(id, 100f * strength);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        if (wsc.mm + wsc.me + wsc.mb + wsc.le + wsc.lm + wsc.lb <= 1) return null;
        return super.getSelectionWeight(spec);
    }
}
