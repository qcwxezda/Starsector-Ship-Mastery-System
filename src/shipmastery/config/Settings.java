package shipmastery.config;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.util.MathUtils;

import java.awt.Color;
import java.io.IOException;

public class Settings {
    public static final float ENHANCE_AMOUNT = 0.1f;
    public static final int MAX_ENHANCES = 5;

    public static Color MASTERY_COLOR = new Color(96, 192, 255);
    public static Color POSITIVE_HIGHLIGHT_COLOR = Misc.getHighlightColor();
    public static Color NEGATIVE_HIGHLIGHT_COLOR = Misc.getNegativeHighlightColor();
    public static Boolean CLEAR_SMODS_ALWAYS_ENABLED;
    public static Float CLEAR_SMODS_REFUND_FRACTION;
    public static String RANDOM_GENERATION_SEED;
    public static Float MP_GAIN_MULTIPLIER;
    public static Float BUILD_IN_CREDITS_COST_MULTIPLIER;
    public static Float DOUBLE_CLICK_INTERVAL;
    public static Boolean ENABLE_COPY_SEED_BUTTON;

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

    /** Hard caps for progression purposes */
    public static Float NPC_MASTERY_DENSITY_CAP;
    public static Float NPC_MASTERY_QUALITY_CAP;
    public static Integer  NPC_MASTERY_MAX_LEVEL_MODIFIER_CAP;
    public static Integer NPC_MASTERY_FLAGSHIP_BONUS_CAP;
    public static Float NPC_SMOD_QUALITY_MOD_CAP;

    public static Boolean NPC_PROGRESSION_ENABLED;
    /** How long it takes for NPC mastery stats to reach cap, measured in total player MP gain across all combat ships*/
    public static Integer NPC_TOTAL_PROGRESSION_MP;

    public static Boolean SHOW_MP_AND_LEVEL_IN_REFIT;

    /** If the player loses a battle, ships that would otherwise be recoverable are spawned as derelicts */
    public static Boolean ENABLE_PLAYER_SHIP_GRAVEYARDS;
    public static Boolean DISABLE_MAIN_FEATURES;
    public static Boolean ENABLE_RANDOM_MODE;
    public static Boolean ENABLE_RECENT_BATTLES;
    public static Boolean RECENT_BATTLES_PRECISE_MODE;
    public static Boolean ADD_SMOD_AUTOFIT_OPTION;


    public static void loadSettingsFromJson() throws JSONException, IOException {
        JSONObject json = Global.getSettings().loadJSON("shipmastery_settings.json", "shipmasterysystem");
        DOUBLE_CLICK_INTERVAL = (float) json.getDouble("doubleClickInterval");
        NPC_MASTERY_DENSITY = (float) json.getDouble("npcMasteryDensity");
        NPC_MASTERY_QUALITY = (float) json.getDouble("npcMasteryQuality");
        NPC_MASTERY_MAX_LEVEL_MODIFIER = json.getInt("npcMasteryMaxLevelModifier");
        NPC_MASTERY_FLAGSHIP_BONUS = json.getInt("npcMasteryFlagshipBonus");
        NPC_SMOD_QUALITY_MOD = (float) json.getDouble("npcSmodQualityMod");
        NPC_MASTERY_DENSITY_CAP = (float) json.getDouble("npcMasteryDensityCap");
        NPC_MASTERY_QUALITY_CAP = (float) json.getDouble("npcMasteryQualityCap");
        NPC_MASTERY_MAX_LEVEL_MODIFIER_CAP = json.getInt("npcMasteryMaxLevelModifierCap");
        NPC_MASTERY_FLAGSHIP_BONUS_CAP = json.getInt("npcMasteryFlagshipBonusCap");
        NPC_SMOD_QUALITY_MOD_CAP = (float) json.getDouble("npcSmodQualityModCap");

        NPC_PROGRESSION_ENABLED = json.getBoolean("progressionEnabled");
        NPC_TOTAL_PROGRESSION_MP = json.getInt("npcTotalProgressionMP");

        SHOW_MP_AND_LEVEL_IN_REFIT = json.getBoolean("showMpAndLevelInRefit");
        ENABLE_PLAYER_SHIP_GRAVEYARDS = json.getBoolean("enablePlayerShipGraveyards");
        ENABLE_RANDOM_MODE = json.getBoolean("enableRandomMode");
        ENABLE_RECENT_BATTLES = json.getBoolean("enableRecentBattles");
        RECENT_BATTLES_PRECISE_MODE = json.getBoolean("recentBattlesPreciseMode");
        ADD_SMOD_AUTOFIT_OPTION = json.getBoolean("addSModAutofitOption");
        DISABLE_MAIN_FEATURES = json.getBoolean("disableMainFeatures");
        CLEAR_SMODS_ALWAYS_ENABLED = json.getBoolean("clearSModsAlwaysEnabled");
        CLEAR_SMODS_REFUND_FRACTION = MathUtils.clamp((float) json.getDouble("clearSModsRefundFraction"), 0f, 1f);
        MP_GAIN_MULTIPLIER = Math.max(0f, (float) json.getDouble("mpGainMultiplier"));
        BUILD_IN_CREDITS_COST_MULTIPLIER = Math.max(0f, (float) json.getDouble("buildInCreditsCostMultiplier"));
        RANDOM_GENERATION_SEED = json.getString("randomMasterySeed");
        ENABLE_COPY_SEED_BUTTON = json.getBoolean("enableCopySeedButton");
    }
}
