package shipmastery.util;

public interface Strings {
    String MASTERY_BUTTON_STR = Utils.getString("sms_refitScreen", "masteryButton");
    String MASTERY_LABEL_STR = Utils.getString("sms_refitScreen", "masteryLabel");

    String MUST_BE_DOCKED_HULLMODS = Utils.getString("sms_masteryPanel", "mustBeDockedHullmods");
    String MUST_BE_DOCKED_MASTERIES = Utils.getString("sms_masteryPanel", "mustBeDockedMasteries");
    String MASTERY_TAB_STR = Utils.getString("sms_masteryPanel", "masteryTab");
    String CREDITS_DISPLAY_STR = Utils.getString("sms_masteryPanel", "creditsDisplay");
    String MASTERY_POINTS_DISPLAY_STR = Utils.getString("sms_masteryPanel", "masteryPointsDisplay");
    String HULLMODS_EMPTY_STR = Utils.getString("sms_masteryPanel", "hullmodListEmptyHint");
    String CLEAR_BUTTON_STR = Utils.getString("sms_masteryPanel", "clearButton");
    String BUILTIN_DISPLAY_STR = Utils.getString("sms_masteryPanel", "builtInDisplay");
    String DOUBLE_CLICK_HINT_STR = Utils.getString("sms_masteryPanel", "doubleClickHint");
    String YES_STR = Utils.getString("sms_masteryPanel", "yes");
    String NO_STR = Utils.getString("sms_masteryPanel", "no");
    String CANT_BUILD_IN_STR = Utils.getString("sms_masteryPanel", "cantBuildIn");
    String LIMIT_REACHED_STR = Utils.getString("sms_masteryPanel", "limitReached");
    String CREDITS_SHORTFALL_STR = Utils.getString("sms_masteryPanel", "notEnoughCredits");
    String MASTERY_POINTS_SHORTFALL_STR = Utils.getString("sms_masteryPanel", "notEnoughMasteryPoints");
    String DISMISS_WINDOW_STR = Utils.getString("sms_masteryPanel", "dismissWindow");
    String HULLMODS_TAB_STR = Utils.getString("sms_masteryPanel", "hullmodsTab");
    String UNKNOWN_EFFECT_STR = Utils.getString("sms_masteryPanel", "unknownMastery");
    String ADVANCE_MASTERY_STR = Utils.getString("sms_masteryPanel", "levelUpMastery");
    String CONFIRM_STR = Utils.getString("sms_masteryPanel", "confirmText2");
    String CANCEL_STR = Utils.getString("sms_masteryPanel", "cancelText");
    String UPGRADE_CONFIRMED_STR = Utils.getString("sms_masteryPanel", "upgradeConfirm");
    String UPGRADE_ASK_STR = Utils.getString("sms_masteryPanel", "confirmText");
    String ENHANCE_STR = Utils.getString("sms_masteryPanel", "enhanceConfirm");
    String BUILD_IN_STR = Utils.getString("sms_masteryPanel", "builtInConfirm");
    String CLEAR_CONFIRMED_STR = Utils.getString("sms_masteryPanel", "clearConfirm");
    String CLEAR_ASK_STR = Utils.getString("sms_masteryPanel", "confirmText");
    String CHANGES_PENDING = Utils.getString("sms_masteryPanel", "changesPending");
    String BUILD_IN_OVER_MAX_WARNING = Utils.getString("sms_masteryPanel", "buildInOverMaxWarning");
    String CANT_OPEN_PANEL = Utils.getString("sms_masteryPanel", "cantOpenPanel");
    String EFFECT_CANT_DEACTIVATE = Utils.getString("sms_misc", "effectCantBeDeactivated");
    String EFFECT_CANT_DEACTIVATE_WARNING = Utils.getString("sms_misc", "effectCantBeDeactivatedWarning");
    String SHIP_MASTERY_EFFECT = Utils.getString("sms_misc", "shipMasteryEffect");
    String AND_STR = Utils.getString("sms_misc", "and");
    String DOESNT_AFFECT_MODULES = Utils.getString("sms_misc", "doesntAffectModules");
    interface Descriptions {
        String SModCapacity = Utils.getString("sms_descriptions", "SModCapacity");
        String SModCreditsCost = Utils.getString("sms_descriptions", "SModCreditsCost");
        String SModCreditsCostNeg = Utils.getString("sms_descriptions", "SModCreditsCostNeg");
        String SModMPCost = Utils.getString("sms_descriptions", "SModMPCost");
        String SModMPCostNeg = Utils.getString("sms_descriptions", "SModMPCostNeg");
        String RestorationCost = Utils.getString("sms_descriptions", "RestorationCost");
        String RestorationCostNeg = Utils.getString("sms_descriptions", "RestorationCostNeg");
        String SModRemoval = Utils.getString("sms_descriptions", "SModRemoval");
        String SModRemovalPost = Utils.getString("sms_descriptions", "SModRemovalPost");
        String SModsOverCapacitySingle = Utils.getString("sms_descriptions", "SModsOverCapacitySingle");
        String SModsOverCapacityPlural = Utils.getString("sms_descriptions", "SModsOverCapacityPlural");
        String SModsOverCapacityPost = Utils.getString("sms_descriptions", "SModsOverCapacityPost");
        String IgnoreNoBuildIn = Utils.getString("sms_descriptions", "IgnoreNoBuildIn");
        String StatIncrease = Utils.getString("sms_descriptions", "StatIncrease");
        String StatDecrease = Utils.getString("sms_descriptions", "StatDecrease");
        String StatListItem = Utils.getString("sms_descriptions", "StatListItem");
        String ScaleOtherMasteries = Utils.getString("sms_descriptions", "ScaleOtherMasteries");
        String ScaleOtherMasteriesNeg = Utils.getString("sms_descriptions", "ScaleOtherMasteriesNeg");
        String ScaleOtherMasteriesPost = Utils.getString("sms_descriptions", "ScaleOtherMasteriesPost");
        String FluxByShieldUpkeep = Utils.getString("sms_descriptions", "FluxByShieldUpkeep");
        String FluxByShieldUpkeepNeg = Utils.getString("sms_descriptions", "FluxByShieldUpkeepNeg");
        String FluxByShieldUpkeepPost = Utils.getString("sms_descriptions", "FluxByShieldUpkeepPost");
        String RangeIfNoBonuses = Utils.getString("sms_descriptions", "RangeIfNoBonuses");
        String RangeIfNoBonusesNeg = Utils.getString("sms_descriptions", "RangeIfNoBonusesNeg");
        String RangeIfNoBonusesPost = Utils.getString("sms_descriptions", "RangeIfNoBonusesPost");
        String MaxOPForHull = Utils.getString("sms_descriptions", "MaxOPForHull");
        String MaxOPForHullPost = Utils.getString("sms_descriptions", "MaxOPForHullPost");
        String DPIfOnlyShip = Utils.getString("sms_descriptions", "DPIfOnlyShip");
        String DPIfOnlyShipNeg = Utils.getString("sms_descriptions", "DPIfOnlyShipNeg");
        String PeakCRMultipleShips = Utils.getString("sms_descriptions", "PeakCRMultipleShips");
        String PeakCRMultipleShipsNeg = Utils.getString("sms_descriptions", "PeakCRMultipleShipsNeg");
        String PeakCRMultipleShipsPost = Utils.getString("sms_descriptions", "PeakCRMultipleShipsPost");
    }
}
