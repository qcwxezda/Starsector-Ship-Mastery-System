package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;

public class AllowOverCapacitySMods extends BaseMasteryEffect {
    static float DP_PENALTY_PER_SMOD = 0.05f;
    @Override
    public MasteryDescription getDescription() {
        int count = getCount();
        return MasteryDescription.initDefaultHighlight(count == 1 ? Strings.ALLOW_OVER_CAPACITY_SMOD_SINGLE : Strings.ALLOW_OVER_CAPACITY_SMOD_PLURAL).params(count);
    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.modifyFlat(id, getCount());
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.unmodify(id);
    }

    @Override
    public boolean canBeDeactivated() {
        return false;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats == null || stats.getVariant() == null || stats.getFleetMember() == null) return;

        int overMax = stats.getVariant().getSMods().size() - SModUtils.getMaxSMods(stats);
        if (overMax > 0) {
            // Should only be applied once per ship, not once per mastery effect
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyPercent("_sms_UNIQUE", 100f * DP_PENALTY_PER_SMOD * overMax);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip) {
        tooltip.addPara(String.format(Strings.ALLOW_OVER_CAPACITY_SMOD_POST, (int) (100f * DP_PENALTY_PER_SMOD)), 5f);
    }

    int getCount() {
        return Math.max(1, (int) (2f * getStrength()));
    }
}