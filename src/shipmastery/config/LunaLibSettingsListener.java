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

        Settings.MASTERY_COLOR = LunaSettings.getColor(id, "sms_generalMasteryColor");
        Settings.POSITIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_generalPositiveHighlightColor");
        Settings.NEGATIVE_HIGHLIGHT_COLOR =
                LunaSettings.getColor(id, "sms_generalNegativeHighlightColor");
        Settings.DOUBLE_CLICK_INTERVAL = LunaSettings.getFloat(id, "sms_generalDoubleClickInterval");
        Settings.SHOW_MP_AND_LEVEL_IN_REFIT =
                LunaSettings.getBoolean(id, "sms_generalRefitScreenDisplay");

        Settings.NPC_MASTERY_DENSITY = LunaSettings.getFloat(id, "sms_difficultyDensity");
        Settings.NPC_MASTERY_QUALITY = LunaSettings.getFloat(id, "sms_difficultyQuality");
        Settings.NPC_MASTERY_MAX_LEVEL_MODIFIER = LunaSettings.getInt(id, "sms_difficultyMaxLevelMod");
        Settings.NPC_MASTERY_FLAGSHIP_BONUS = LunaSettings.getInt(id, "sms_difficultyFlagshipBonus");
        Settings.NPC_SMOD_QUALITY_MOD = LunaSettings.getFloat(id, "sms_difficultySModMod");

        Settings.ENABLE_PLAYER_SHIP_GRAVEYARDS =
                LunaSettings.getBoolean(id, "sms_miscPlayerShipGraveyards");
        Settings.ENABLE_RANDOM_MODE = LunaSettings.getBoolean(id, "sms_miscRandomMode");
        Settings.ADD_SMOD_AUTOFIT_OPTION = LunaSettings.getBoolean(id, "sms_miscSModAutofitOption");

        FleetHandler.NPC_MASTERY_CACHE.clear();
    }
}
