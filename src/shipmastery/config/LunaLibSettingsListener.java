package shipmastery.config;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import shipmastery.campaign.FleetHandler;

public class LunaLibSettingsListener implements LunaSettingsListener {

    public static final String id = "shipmasterysystem";

    public static void init() {
        LunaLibSettingsListener settingsListener = new LunaLibSettingsListener();
        LunaSettings.addSettingsListener(settingsListener);
        settingsListener.settingsChanged(id);
    }

    @Override
    public void settingsChanged(@NotNull String modId) {
        if (!id.equals(modId)) return;

        Settings.COMBAT_MP_GAIN_MULTIPLIER = LunaSettings.getFloat(id, "sms_CombatMPGainMultiplier");
        Settings.CIVILIAN_MP_GAIN_MULTIPLIER = LunaSettings.getFloat(id, "sms_CivilianMPGainMultiplier");
        Settings.BUILD_IN_CREDITS_COST_MULTIPLIER = LunaSettings.getFloat(id, "sms_SModCreditCostMultiplier");
        Settings.RANDOM_GENERATION_SEED = LunaSettings.getString(id, "sms_RandomMasterySeed");
        Settings.DISPLAY_LOCKED_MASTERIES = LunaSettings.getBoolean(id, "sms_DisplayLockedMasteries");

        Settings.MASTERY_COLOR = LunaSettings.getColor(id, "sms_MasteryColor");
        Settings.DOUBLE_CLICK_INTERVAL = LunaSettings.getFloat(id, "sms_DoubleClickInterval");
        Settings.SHOW_MP_AND_LEVEL_IN_REFIT =
                LunaSettings.getBoolean(id, "sms_RefitScreenDisplay");
        Settings.ENABLE_COPY_SEED_BUTTON = LunaSettings.getBoolean(id, "sms_EnableCopySeedButton");
        Settings.CR_PENALTY_PER_EXCESS_OP_PERCENT = LunaSettings.getFloat(id, "sms_CrPenaltyPerExcessOPPercent");

        Settings.NPC_MASTERY_LEVEL_MODIFIER = LunaSettings.getInt(id, "sms_DifficultyLevelMod");
        Settings.NPC_MASTERY_LEVEL_MODIFIER_CAP = LunaSettings.getInt(id, "sms_DifficultyLevelModCap");

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
        FleetHandler.NPC_MASTERY_CACHE.clear();
    }
}
