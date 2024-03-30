package shipmastery.config;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import shipmastery.campaign.FleetHandler;

public class LunaLibSettingsListener implements LunaSettingsListener {

    public static void init() {
        LunaLibSettingsListener settingsListener = new LunaLibSettingsListener();
        LunaSettings.addSettingsListener(settingsListener);
        settingsListener.settingsChanged("shipmasterysystem");
    }

    @Override
    public void settingsChanged(@NotNull String modId) {
        if (!"shipmasterysystem".equals(modId)) return;

        Settings.MASTERY_COLOR = LunaSettings.getColor("shipmasterysystem", "general_MasteryColor");
        Settings.POSITIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor("shipmasterysystem", "general_PositiveHighlightColor");
        Settings.NEGATIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor("shipmasterysystem", "general_NegativeHighlightColor");
        Settings.DOUBLE_CLICK_INTERVAL = LunaSettings.getFloat("shipmasterysystem", "general_DoubleClickInterval");
        Settings.SHOW_MP_AND_LEVEL_IN_REFIT =
                LunaSettings.getBoolean("shipmasterysystem", "general_RefitScreenDisplay");

        Settings.NPC_MASTERY_DENSITY = LunaSettings.getFloat("shipmasterysystem", "difficulty_Density");
        Settings.NPC_MASTERY_QUALITY = LunaSettings.getFloat("shipmasterysystem", "difficulty_Quality");
        Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER = LunaSettings.getInt("shipmasterysystem", "difficulty_MaxLevelMod");
        Settings.NPC_MASTERY_FLAGSHIP_BONUS = LunaSettings.getInt("shipmasterysystem", "difficulty_FlagshipBonus");
        Settings.NPC_SMOD_QUALITY_MOD = LunaSettings.getFloat("shipmasterysystem", "difficulty_SModMod");

        Settings.ENABLE_PLAYER_SHIP_GRAVEYARDS =
                LunaSettings.getBoolean("shipmasterysystem", "misc_PlayerShipGraveyards");
        Settings.ENABLE_RANDOM_MODE = LunaSettings.getBoolean("shipmasterysystem", "misc_RandomMode");
        Settings.ADD_SMOD_AUTOFIT_OPTION = LunaSettings.getBoolean("shipmasterysystem", "misc_SModAutofitOption");

        FleetHandler.NPC_MASTERY_CACHE.clear();
    }
}
