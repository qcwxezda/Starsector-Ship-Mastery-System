package shipmastery.config;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.io.IOException;

public class Settings {
    public static Color MASTERY_COLOR = new Color(96, 192, 255);
    public static Color POSITIVE_HIGHLIGHT_COLOR = Misc.getHighlightColor();
    public static Color NEGATIVE_HIGHLIGHT_COLOR = Misc.getNegativeHighlightColor();
    public static Float DOUBLE_CLICK_INTERVAL;

    /** From 0-1, roughly the percentage of ships in NPC fleets that will have masteries */
    public static Float NPC_MASTERY_DENSITY;

    /** From 0-1, defines the mastery level distribution among hulls with masteries (0 = all level 1, 1 = all max level) */
    public static Float NPC_MASTERY_QUALITY;

    /** By default, the maximum mastery level any ship hull can have for an NPC fleet is the officer level of the commander */
    public static Integer NPC_MASTERY_MAX_LEVEL_MODIFIER;

    /** Modifies the max possible mastery level of the NPC flagship's hull spec by this much */
    public static Integer NPC_MASTERY_FLAGSHIP_BONUS ;

    /** Chance for each available s-mod slot to be filled is equal to the fleet's quality plus this bonus. */
    public static Float NPC_SMOD_QUALITY_MOD;
    public static Boolean SHOW_MP_AND_LEVEL_IN_REFIT;

    /** If the player loses a battle, ships that would otherwise be recoverable are spawned as derelicts */
    public static Boolean ENABLE_PLAYER_SHIP_GRAVEYARDS;

    public static void loadSettingsFromJson() throws JSONException, IOException {
        JSONObject json = Global.getSettings().loadJSON("shipmastery_settings.json", "shipmasterysystem");
        DOUBLE_CLICK_INTERVAL = (float) json.getDouble("doubleClickInterval");
        NPC_MASTERY_DENSITY = (float) json.getDouble("npcMasteryDensity");
        NPC_MASTERY_QUALITY = (float) json.getDouble("npcMasteryQuality");
        NPC_MASTERY_MAX_LEVEL_MODIFIER = json.getInt("npcMasteryMaxLevelModifier");
        NPC_MASTERY_FLAGSHIP_BONUS = json.getInt("npcMasteryFlagshipBonus");
        NPC_SMOD_QUALITY_MOD = (float) json.getDouble("npcSmodQualityMod");
        SHOW_MP_AND_LEVEL_IN_REFIT = json.getBoolean("showMpAndLevelInRefit");
        ENABLE_PLAYER_SHIP_GRAVEYARDS = json.getBoolean("enablePlayerShipGraveyards");
    }
}
