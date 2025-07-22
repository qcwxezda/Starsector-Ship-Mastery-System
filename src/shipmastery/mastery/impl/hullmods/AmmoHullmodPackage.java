package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AmmoHullmodPackage extends HullmodPackage {

    public static final float REGEN_MULTIPLIER = 0.6f;

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.AmmoHullmodPackage;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.AmmoHullmodPackagePost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercentNoDecimal(getStrength(selectedVariant)));
    }

    @Override
    protected String[] getDescriptionParams(ShipVariantAPI selectedVariant) {
        return new String[] {
                Utils.getHullmodName(HullMods.MISSLERACKS),
                Utils.getHullmodName(HullMods.MAGAZINES),
                Utils.asPercentNoDecimal(getStrength(selectedVariant)),
                Utils.asPercentNoDecimal(getStrength(selectedVariant)*REGEN_MULTIPLIER)
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
        stats.getBallisticAmmoBonus().modifyMult(id, 1f + strength);
        stats.getEnergyAmmoBonus().modifyMult(id, 1f + strength);
        stats.getMissileAmmoBonus().modifyMult(id, 1f + strength);

        float regenStrength = strength*REGEN_MULTIPLIER;
        stats.getBallisticAmmoRegenMult().modifyPercent(id, 100f*regenStrength);
        stats.getEnergyAmmoRegenMult().modifyPercent(id, 100f*regenStrength);
        stats.getMissileAmmoRegenMult().modifyPercent(id, 100f*regenStrength);
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        float strength = getStrength(stats);
        stats.getBallisticAmmoBonus().modifyPercent(id, 100f*strength);
        stats.getEnergyAmmoBonus().modifyPercent(id, 100f*strength);
        stats.getMissileAmmoBonus().modifyPercent(id, 100f*strength);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        if (wsc.ltotal + wsc.mtotal + wsc.stotal == 0) return null;
        return super.getSelectionWeight(spec);
    }
}
