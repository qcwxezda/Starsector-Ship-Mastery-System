package shipmastery.aicoreinterface;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BetaCoreInterface implements AICoreInterfacePlugin {

    public static final float ECM_PER = 0.01f;
    public static final float[] ECM_CAP = new float[] {0.01f, 0.01f, 0.02f, 0.03f};
    public static final float[] EFFECTIVE_ARMOR_PER = new float[] {10f, 10f, 15f, 20f};
    public static final float INCREASED_DMOD_PROB = 9f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return AICoreInterfacePlugin.getDefaultIntegrationCost(member, 60000f, 120000f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() == null) return;
        int sCount = Misc.getCurrSpecialMods(stats.getVariant());
        int dCount = DModManager.getNumDMods(stats.getVariant());
        int size = Utils.hullSizeToInt(hullSize);
        stats.getEffectiveArmorBonus().modifyFlat(id, EFFECTIVE_ARMOR_PER[size] * dCount);
        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, 100f * Math.min(ECM_PER*sCount, ECM_CAP[size]));
        stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD).modifyPercent(id, 100f * INCREASED_DMOD_PROB);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.betaCoreIntegrationEffect,
                0f,
                new Color[] {
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asInt(EFFECTIVE_ARMOR_PER[0]),
                Utils.asInt(EFFECTIVE_ARMOR_PER[1]),
                Utils.asInt(EFFECTIVE_ARMOR_PER[2]),
                Utils.asInt(EFFECTIVE_ARMOR_PER[3]),
                Utils.asPercent(ECM_PER),
                Utils.asPercent(ECM_CAP[0]),
                Utils.asPercent(ECM_CAP[1]),
                Utils.asPercent(ECM_CAP[2]),
                Utils.asPercent(ECM_CAP[3]),
                Utils.asPercent(INCREASED_DMOD_PROB));
    }
}
