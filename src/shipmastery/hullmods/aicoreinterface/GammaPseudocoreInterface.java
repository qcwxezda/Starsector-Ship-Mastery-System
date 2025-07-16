package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class GammaPseudocoreInterface extends AICoreInterfaceHullmod {
    public static final float CR_PER_HULLMOD = 0.02f;
    public static final float CR_INITIAL = 0.12f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 50000f, 150000f);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.gammaPseudocoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(CR_INITIAL),
                Utils.asPercent(CR_PER_HULLMOD),
                Strings.Misc.doubled);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var variant = stats.getVariant();
        if (variant == null) return;
        long count = variant.getNonBuiltInHullmods().stream()
                .map(x -> Global.getSettings().getHullModSpec(x))
                .filter(x -> !x.isHiddenEverywhere() && x.getCostFor(hullSize) > 0)
                .count();
        float bonus = CR_INITIAL - CR_PER_HULLMOD * count;
        if (Misc.isAutomated(variant)) bonus *= 2f;
        stats.getMaxCombatReadiness().modifyFlat(id, bonus, Strings.Items.integratedDesc);
    }
}
