package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class SafetyOverridesBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.SafetyOverridesBoost)
                .params(Global.getSettings().getHullModSpec(HullMods.SAFETYOVERRIDES).getDisplayName(), Utils.asFloatOneDecimal(strength), (int) (5f * strength));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null) return;
        if (stats.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) {
            stats.getMaxSpeed().modifyFlat(id, getStrength(stats));
            stats.getWeaponRangeThreshold().modifyFlat(id, 5f *getStrength(stats));
        }
    }
}
