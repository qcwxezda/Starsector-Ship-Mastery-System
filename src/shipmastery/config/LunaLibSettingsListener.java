package shipmastery.config;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import shipmastery.campaign.FleetHandler;

public class LunaLibSettingsListener implements LunaSettingsListener {

    public static String id = "shipmasterysystem";

    public static void init() {
        LunaLibSettingsListener settingsListener = new LunaLibSettingsListener();
        LunaSettings.addSettingsListener(settingsListener);
        settingsListener.settingsChanged(id);
    }

    @Override
    public void settingsChanged(@NotNull String modId) {
        if (!id.equals(modId)) return;

        Settings.CLEAR_SMODS_ALWAYS_ENABLED = LunaSettings.getBoolean(id, "sms_ClearSModsAlwaysEnabled");
        Settings.CLEAR_SMODS_REFUND_FRACTION = LunaSettings.getFloat(id, "sms_ClearSModsRefundFraction");
        Settings.MP_GAIN_MULTIPLIER = LunaSettings.getFloat(id, "sms_MPGainMultiplier");
        Settings.BUILD_IN_CREDITS_COST_MULTIPLIER = LunaSettings.getFloat(id, "sms_SModCreditCostMultiplier");
        Settings.MASTERY_COLOR = LunaSettings.getColor(id, "sms_MasteryColor");
        Settings.POSITIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_PositiveHighlightColor");
        Settings.NEGATIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_NegativeHighlightColor");
        Settings.DOUBLE_CLICK_INTERVAL = LunaSettings.getFloat(id, "sms_DoubleClickInterval");
        Settings.SHOW_MP_AND_LEVEL_IN_REFIT =
                LunaSettings.getBoolean(id, "sms_RefitScreenDisplay");

        Settings.NPC_MASTERY_DENSITY = LunaSettings.getFloat(id, "sms_DifficultyDensity");
        Settings.NPC_MASTERY_QUALITY = LunaSettings.getFloat(id, "sms_DifficultyQuality");
        Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER = LunaSettings.getInt(id, "sms_DifficultyMaxLevelMod");
        Settings.NPC_MASTERY_FLAGSHIP_BONUS = LunaSettings.getInt(id, "sms_DifficultyFlagshipBonus");
        Settings.NPC_SMOD_QUALITY_MOD = LunaSettings.getFloat(id, "sms_DifficultySModMod");

        Settings.ENABLE_PLAYER_SHIP_GRAVEYARDS =
                LunaSettings.getBoolean(id, "sms_PlayerShipGraveyards");
        Settings.ENABLE_RANDOM_MODE = LunaSettings.getBoolean(id, "sms_RandomMode");
        Settings.ADD_SMOD_AUTOFIT_OPTION = LunaSettings.getBoolean(id, "sms_SModAutofitOption");
        String recentBattlesParam = LunaSettings.getString(id, "sms_RecentBattles");
        if ("Off".equals(recentBattlesParam)) {
            Settings.ENABLE_RECENT_BATTLES = false;
        }
        else {
            Settings.ENABLE_RECENT_BATTLES = true;
            Settings.RECENT_BATTLES_PRECISE_MODE = "Precise".equals(recentBattlesParam);
        }
        Settings.DISABLE_MAIN_FEATURES = LunaSettings.getBoolean(id, "sms_DisableMainFeatures");

        FleetHandler.NPC_MASTERY_CACHE.clear();
    }
}
