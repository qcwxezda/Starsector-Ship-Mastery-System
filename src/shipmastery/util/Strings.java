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
    String ICON_HEADER = Utils.getString("sms_masteryPanel", "iconHeader");
    String HULLMOD_HEADER = Utils.getString("sms_masteryPanel", "hullmodHeader");
    String DESIGN_TYPE_HEADER = Utils.getString("sms_masteryPanel", "designTypeHeader");
    String ORDNANCE_POINTS_HEADER = Utils.getString("sms_masteryPanel", "ordnancePointsHeader");
    String MASTERY_POINTS_HEADER = Utils.getString("sms_masteryPanel", "masteryPointsHeader");
    String CREDITS_HEADER = Utils.getString("sms_masteryPanel", "creditsHeader");
    String MODULAR_HEADER = Utils.getString("sms_masteryPanel", "modularHeader");
    String EFFECT_CANT_DEACTIVATE = Utils.getString("sms_misc", "effectCantBeDeactivated");
    String EFFECT_CANT_DEACTIVATE_WARNING = Utils.getString("sms_misc", "effectCantBeDeactivatedWarning");
    String SHIP_MASTERY_EFFECT = Utils.getString("sms_misc", "shipMasteryEffect");
    String AND_STR = Utils.getString("sms_misc", "and");
    String DOESNT_AFFECT_MODULES = Utils.getString("sms_misc", "doesntAffectModules");
    String FAILED_TO_GENERATE_MASTERIES = Utils.getString("sms_misc", "failedToGenerateMasteries");
    String FLAGSHIP_ONLY = Utils.getString("sms_misc", "flagshipOnly");
    String BEST_OF_THE_BEST_DESC = Utils.getString("sms_misc", "bestOfTheBestDesc");

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
        String ShieldDeflection = Utils.getString("sms_descriptions", "ShieldDeflection");
        String ShieldDeflectionPost = Utils.getString("sms_descriptions", "ShieldDeflectionPost");
        String ShieldDeflectionStatusTitle = Utils.getString("sms_descriptions", "ShieldDeflectionStatusTitle");
        String ShieldDeflectionStatusDesc = Utils.getString("sms_descriptions", "ShieldDeflectionStatusDesc");
        String HighFrequencyMotes = Utils.getString("sms_descriptions", "HighFrequencyMotes");
        String HighFrequencyMotesPost = Utils.getString("sms_descriptions", "HighFrequencyMotesPost");
        String PhaseCloakResidue = Utils.getString("sms_descriptions", "PhaseCloakResidue");
        String PhaseCloakResiduePost = Utils.getString("sms_descriptions", "PhaseCloakResiduePost");
        String PhaseCloakResidueStatusTitle = Utils.getString("sms_descriptions", "PhaseCloakResidueStatusTitle");
        String PhaseCloakResidueStatusDesc1 = Utils.getString("sms_descriptions", "PhaseCloakResidueStatusDesc1");
        String PhaseCloakResidueStatusDesc2 = Utils.getString("sms_descriptions", "PhaseCloakResidueStatusDesc2");
        String EnergyMineConversion = Utils.getString("sms_descriptions", "EnergyMineConversion");
        String EnergyMineConversionPost = Utils.getString("sms_descriptions", "EnergyMineConversionPost");
        String RecallDeviceRegeneration = Utils.getString("sms_descriptions", "RecallDeviceRegeneration");
        String RecallDeviceDestruction = Utils.getString("sms_descriptions", "RecallDeviceDestruction");
        String RecallDeviceDestructionPost = Utils.getString( "sms_descriptions", "RecallDeviceDestructionPost");
        String BallisticFireRateHullLevel = Utils.getString("sms_descriptions", "BallisticFireRateHullLevel");
        String BallisticFireRateHullLevelPost = Utils.getString("sms_descriptions", "BallisticFireRateHullLevelPost");
        String BallisticFireRateHullLevelTitle = Utils.getString("sms_descriptions", "BallisticFireRateHullLevelTitle");
        String BallisticFireRateHullLevelDesc1 = Utils.getString("sms_descriptions", "BallisticFireRateHullLevelDesc1");
        String BallisticFireRateHullLevelDesc2 = Utils.getString("sms_descriptions", "BallisticFireRateHullLevelDesc2");
        String AAFRangeDamage = Utils.getString("sms_descriptions", "AAFRangeDamage");
        String AAFRangeDamagePost = Utils.getString("sms_descriptions", "AAFRangeDamagePost");
        String AAFRangeDamageTitle = Utils.getString("sms_descriptions", "AAFRangeDamageTitle");
        String AAFRangeDamageDesc1 = Utils.getString("sms_descriptions", "AAFRangeDamageDesc1");
        String ManeuveringJetsMobility = Utils.getString("sms_descriptions", "ManeuveringJetsMobility");
        String ManeuveringJetsMobilityPost = Utils.getString("sms_descriptions", "ManeuveringJetsMobilityPost");
        String ManeuveringJetsMobilityTitle = Utils.getString("sms_descriptions", "ManeuveringJetsMobilityTitle");
        String ManeuveringJetsMobilityDesc1 = Utils.getString("sms_descriptions", "ManeuveringJetsMobilityDesc1");
        String SystemRegenOnKill = Utils.getString("sms_descriptions", "SystemRegenOnKill");
        String SystemRegenOnKillPost = Utils.getString("sms_descriptions", "SystemRegenOnKillPost");
        String SystemRegenOnKillPost2 = Utils.getString("sms_descriptions", "SystemRegenOnKillPost2");
        String SystemRegenOnKillPost3 = Utils.getString("sms_descriptions", "SystemRegenOnKillPost3");
        String HEFExplosion = Utils.getString("sms_descriptions", "HEFExplosion");
        String HEFExplosionPost = Utils.getString("sms_descriptions", "HEFExplosionPost");
        String HEFExplosionPost2 = Utils.getString("sms_descriptions", "HEFExplosionPost2");
        String HEFExplosionDesc1 = Utils.getString("sms_descriptions", "HEFExplosionDesc1");
        String HEFExplosionTitle = Utils.getString("sms_descriptions", "HEFExplosionTitle");
        String ConvertedHangarBays = Utils.getString("sms_descriptions", "ConvertedHangarBays");
        String LidarArrayRange = Utils.getString("sms_descriptions", "LidarArrayRange");
        String LidarArrayRangeTitle = Utils.getString("sms_descriptions", "LidarArrayRangeTitle");
        String LidarArrayRangeDesc1 = Utils.getString("sms_descriptions", "LidarArrayRangeDesc1");
        String LidarArrayFlux = Utils.getString("sms_descriptions", "LidarArrayFlux");
        String LidarArrayFluxTitle = Utils.getString("sms_descriptions", "LidarArrayFluxTitle");
        String LidarArrayFluxDesc1 = Utils.getString("sms_descriptions", "LidarArrayFluxDesc1");
        String BurnDriveDR = Utils.getString("sms_descriptions", "BurnDriveDR");
        String BurnDriveDRPost = Utils.getString("sms_descriptions", "BurnDriveDRPost");
        String BurnDriveDRTitle = Utils.getString("sms_descriptions", "BurnDriveDRTitle");
        String BurnDriveDRDesc1 = Utils.getString("sms_descriptions", "BurnDriveDRDesc1");
        String WhileActiveMissileBoost = Utils.getString("sms_descriptions", "WhileActiveMissileBoost");
        String WhileActiveMissileBoostTitle = Utils.getString("sms_descriptions", "WhileActiveMissileBoostTitle");
        String WhileActiveMissileBoostDesc1 = Utils.getString("sms_descriptions", "WhileActiveMissileBoostDesc1");
        String PlasmaBurnEngineRepair = Utils.getString( "sms_descriptions", "PlasmaBurnEngineRepair");
        String PlasmaBurnEngineRepairPost = Utils.getString( "sms_descriptions", "PlasmaBurnEngineRepairPost");
        String PlasmaBurnEngineRepairTitle = Utils.getString( "sms_descriptions", "PlasmaBurnEngineRepairTitle");
        String PlasmaBurnEngineRepairDesc1 = Utils.getString( "sms_descriptions", "PlasmaBurnEngineRepairDesc1");
        String PlasmaBurnEngineRepairDesc2 = Utils.getString( "sms_descriptions", "PlasmaBurnEngineRepairDesc2");
        String ZeroFluxResidue = Utils.getString( "sms_descriptions", "ZeroFluxResidue");
        String ZeroFluxResidueTitle = Utils.getString( "sms_descriptions", "ZeroFluxResidueTitle");
        String ZeroFluxResidueDesc1 = Utils.getString( "sms_descriptions", "ZeroFluxResidueDesc1");
        String HitAngleDR = Utils.getString( "sms_descriptions", "HitAngleDR");
        String HitAngleDRNeg = Utils.getString( "sms_descriptions", "HitAngleDRNeg");
        String HitAngleDRPost = Utils.getString( "sms_descriptions", "HitAngleDRPost");
        String HitAngleDRPost2 = Utils.getString( "sms_descriptions", "HitAngleDRPost2");
    }
}
