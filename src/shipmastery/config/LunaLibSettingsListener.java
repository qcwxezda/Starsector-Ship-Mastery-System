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
        Settings.RANDOM_GENERATION_SEED = LunaSettings.getString(id, "sms_RandomMasterySeed");

        Settings.MASTERY_COLOR = LunaSettings.getColor(id, "sms_MasteryColor");
        Settings.POSITIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_PositiveHighlightColor");
        Settings.NEGATIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_NegativeHighlightColor");
        Settings.DOUBLE_CLICK_INTERVAL = LunaSettings.getFloat(id, "sms_DoubleClickInterval");
        Settings.SHOW_MP_AND_LEVEL_IN_REFIT =
                LunaSettings.getBoolean(id, "sms_RefitScreenDisplay");
        Settings.ENABLE_COPY_SEED_BUTTON = LunaSettings.getBoolean(id, "sms_EnableCopySeedButton");

        Settings.NPC_MASTERY_DENSITY = LunaSettings.getFloat(id, "sms_DifficultyDensity");
        Settings.NPC_MASTERY_QUALITY = LunaSettings.getFloat(id, "sms_DifficultyQuality");
        Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER = LunaSettings.getInt(id, "sms_DifficultyMaxLevelMod");
        Settings.NPC_MASTERY_FLAGSHIP_BONUS = LunaSettings.getInt(id, "sms_DifficultyFlagshipBonus");
        Settings.NPC_SMOD_QUALITY_MOD = LunaSettings.getFloat(id, "sms_DifficultySModMod");

        Settings.NPC_MASTERY_DENSITY_CAP = LunaSettings.getFloat(id, "sms_DifficultyDensityCap");
        Settings.NPC_MASTERY_QUALITY_CAP = LunaSettings.getFloat(id, "sms_DifficultyQualityCap");
        Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER_CAP = LunaSettings.getInt(id, "sms_DifficultyMaxLevelModCap");
        Settings.NPC_MASTERY_FLAGSHIP_BONUS_CAP = LunaSettings.getInt(id, "sms_DifficultyFlagshipBonusCap");
        Settings.NPC_SMOD_QUALITY_MOD_CAP = LunaSettings.getFloat(id, "sms_DifficultySModModCap");

        Settings.NPC_PROGRESSION_ENABLED = LunaSettings.getBoolean(id, "sms_ProgressionEnabled");
        Settings.NPC_TOTAL_PROGRESSION_MP = LunaSettings.getInt(id, "sms_TotalProgressionMP");

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

        Settings.CYBER_AUG_BASE_BONUS = LunaSettings.getFloat(id, "sms_CyberAugBaseBonus");
        Settings.CYBER_AUG_MAX_BONUS = LunaSettings.getFloat(id, "sms_CyberAugMaxBonus");
        Settings.CYBER_AUG_BONUS_PER_GROUP = LunaSettings.getFloat(id, "sms_CyberAugBonusPerGroup");

        FleetHandler.NPC_MASTERY_CACHE.clear();
    }
}
