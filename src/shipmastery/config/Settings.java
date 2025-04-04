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
    public static Float COMBAT_MP_GAIN_MULTIPLIER;
    public static Float CIVILIAN_MP_GAIN_MULTIPLIER;
    public static Float BUILD_IN_CREDITS_COST_MULTIPLIER;
    public static Float DOUBLE_CLICK_INTERVAL;
    public static Float CYBER_AUG_MAX_BONUS;
    public static Float CYBER_AUG_BONUS_PER_GROUP;
    public static Float CYBER_AUG_BASE_BONUS;
    public static Boolean ENABLE_COPY_SEED_BUTTON;
    /** By default, the average mastery level any ship hull will have for an NPC fleet is (1/3 commander level + modifier in difficulty csv) */
    public static Integer NPC_MASTERY_LEVEL_MODIFIER;
    /** Hard cap for progression purposes */
    public static Integer NPC_MASTERY_LEVEL_MODIFIER_CAP;
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
        NPC_MASTERY_LEVEL_MODIFIER = json.getInt("npcMasteryLevelModifier");
        NPC_MASTERY_LEVEL_MODIFIER_CAP = json.getInt("npcMasteryLevelModifierCap");

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
        COMBAT_MP_GAIN_MULTIPLIER = Math.max(0f, (float) json.getDouble("combatMPGainMultiplier"));
        CIVILIAN_MP_GAIN_MULTIPLIER = Math.max(0f, (float) json.getDouble("civilianMPGainMultiplier"));
        BUILD_IN_CREDITS_COST_MULTIPLIER = Math.max(0f, (float) json.getDouble("buildInCreditsCostMultiplier"));
        RANDOM_GENERATION_SEED = json.getString("randomMasterySeed");
        ENABLE_COPY_SEED_BUTTON = json.getBoolean("enableCopySeedButton");

        CYBER_AUG_BASE_BONUS = Math.max(0f, (float) json.getDouble("cyberAugBaseBonus"));
        CYBER_AUG_MAX_BONUS = Math.max(0f, (float) json.getDouble("cyberAugMaxBonus"));
        CYBER_AUG_BONUS_PER_GROUP = Math.max(0f, (float) json.getDouble("cyberAugBonusPerGroup"));
    }
}
