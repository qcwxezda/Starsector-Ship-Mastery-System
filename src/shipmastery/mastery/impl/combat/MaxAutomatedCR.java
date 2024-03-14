package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MaxAutomatedCR extends BaseMasteryEffect {
    public static final String automatedShipsName = Global.getSettings().getSkillSpec(Skills.AUTOMATED_SHIPS).getName();
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MaxAutomatedCR).params(
                automatedShipsName,
                Utils.asPercent(1f));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (Misc.isAutomated(stats) && !Automated.isAutomatedNoPenalty(stats)) {
            for (MutableStat.StatMod mod : stats.getMaxCombatReadiness().getFlatMods().values()) {
                if (mod.getSource().startsWith(Skills.AUTOMATED_SHIPS)) {
                    stats.getMaxCombatReadiness().modifyFlat(mod.getSource(), 1f, mod.getDesc());
                    break;
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isBuiltInMod(HullMods.AUTOMATED)) return null;
        return 3.5f - Utils.hullSizeToInt(spec.getHullSize());
    }
}
