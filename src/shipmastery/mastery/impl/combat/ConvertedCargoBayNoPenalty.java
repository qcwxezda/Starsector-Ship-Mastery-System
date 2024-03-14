package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

/** Note: don't just remove the hullmod from the variant or suppress it,
 * as there's no way to properly add it back / un-suppress it should the mastery be deactivated. */
public class ConvertedCargoBayNoPenalty extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ConvertedCargoBayNoPenalty).params(
                Global.getSettings().getHullModSpec(HullMods.CONVERTED_BAY).getDisplayName(),
                Global.getSettings().getHullModSpec(HullMods.DEFECTIVE_MANUFACTORY).getDisplayName());
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship) {
        MutableShipStatsAPI stats = fighter.getMutableStats();
        stats.getMaxSpeed().unmodify(HullMods.DEFECTIVE_MANUFACTORY);
        stats.getArmorDamageTakenMult().unmodify(HullMods.DEFECTIVE_MANUFACTORY);
        stats.getShieldDamageTakenMult().unmodify(HullMods.DEFECTIVE_MANUFACTORY);
        stats.getHullDamageTakenMult().unmodify(HullMods.DEFECTIVE_MANUFACTORY);
        stats.getMaxSpeed().unmodify(HullMods.CONVERTED_BAY);
        stats.getArmorDamageTakenMult().unmodify(HullMods.CONVERTED_BAY);
        stats.getShieldDamageTakenMult().unmodify(HullMods.CONVERTED_BAY);
        stats.getHullDamageTakenMult().unmodify(HullMods.CONVERTED_BAY);
        fighter.setDHullOverlay(null);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isBuiltInMod(HullMods.CONVERTED_BAY) && !spec.isBuiltInMod(HullMods.DEFECTIVE_MANUFACTORY)) return null;
        return 3f;
    }
}
