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
    String BEST_OF_THE_BEST_DESC2 = Utils.getString("sms_misc", "bestOfTheBestDesc2");
    String BEST_OF_THE_BEST_DESC3 = Utils.getString("sms_misc", "bestOfTheBestDesc3");
    String BEST_OF_THE_BEST_SCOPE = Utils.getString("sms_misc", "bestOfTheBestScope");
    String BEST_OF_THE_BEST_SCOPE2 = Utils.getString("sms_misc", "bestOfTheBestScope2");
    String EXCESS_OP_WARNING = Utils.getString("sms_misc", "excessOPWarning");
    String gainedMPSingle = Utils.getString("sms_messages", "gainedMPSingle");
    String gainedMPMultiple = Utils.getString("sms_messages", "gainedMPMultiple");

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
        String SpeedHullLevel = Utils.getString("sms_descriptions", "SpeedHullLevel");
        String SpeedHullLevelPost = Utils.getString("sms_descriptions", "SpeedHullLevelPost");
        String SpeedHullLevelTitle = Utils.getString("sms_descriptions", "SpeedHullLevelTitle");
        String SpeedHullLevelDesc1 = Utils.getString("sms_descriptions", "SpeedHullLevelDesc1");
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
        String MissileRegenOnKill = Utils.getString("sms_descriptions", "MissileRegenOnKill");
        String MissileRegenOnKillPost = Utils.getString("sms_descriptions", "MissileRegenOnKillPost");
        String HEFExplosion = Utils.getString("sms_descriptions", "HEFExplosion");
        String HEFExplosionPost = Utils.getString("sms_descriptions", "HEFExplosionPost");
        String HEFExplosionPost2 = Utils.getString("sms_descriptions", "HEFExplosionPost2");
        String HEFExplosionDesc1 = Utils.getString("sms_descriptions", "HEFExplosionDesc1");
        String HEFExplosionTitle = Utils.getString("sms_descriptions", "HEFExplosionTitle");
        String ConvertedHangarBays = Utils.getString("sms_descriptions", "ConvertedHangarBays");
        String ConvertedHangarBaysPost = Utils.getString("sms_descriptions", "ConvertedHangarBaysPost");
        String LidarArrayRange = Utils.getString("sms_descriptions", "LidarArrayRange");
        String LidarArrayRangeTitle = Utils.getString("sms_descriptions", "LidarArrayRangeTitle");
        String LidarArrayRangeDesc1 = Utils.getString("sms_descriptions", "LidarArrayRangeDesc1");
        String LidarArrayFlux = Utils.getString("sms_descriptions", "LidarArrayFlux");
        String LidarArrayFluxTitle = Utils.getString("sms_descriptions", "LidarArrayFluxTitle");
        String LidarArrayFluxDesc1 = Utils.getString("sms_descriptions", "LidarArrayFluxDesc1");
        String ShipSystemDR = Utils.getString("sms_descriptions", "ShipSystemDR");
        String ShipSystemDRPost = Utils.getString("sms_descriptions", "ShipSystemDRPost");
        String BurnDriveDRTitle = Utils.getString("sms_descriptions", "BurnDriveDRTitle");
        String ManeuveringJetsDRTitle = Utils.getString("sms_descriptions", "ManeuveringJetsDRTitle");
        String ShipSystemDRDesc1 = Utils.getString("sms_descriptions", "ShipSystemDRDesc1");
        String BurnDriveMissileBoost = Utils.getString("sms_descriptions", "BurnDriveMissileBoost");
        String BurnDriveMissileBoostTitle = Utils.getString("sms_descriptions", "BurnDriveMissileBoostTitle");
        String BurnDriveMissileBoostDesc1 = Utils.getString("sms_descriptions", "BurnDriveMissileBoostDesc1");
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
        String TPCUpgrade = Utils.getString( "sms_descriptions", "TPCUpgrade");
        String TPCName = Utils.getString( "sms_descriptions", "TPCName");
        String TPCUpgradePost = Utils.getString( "sms_descriptions", "TPCUpgradePost");
        String TPCChaining = Utils.getString( "sms_descriptions", "TPCChaining");
        String TPCChainingPost = Utils.getString( "sms_descriptions", "TPCChainingPost");
        String TorpedoTracking = Utils.getString( "sms_descriptions", "TorpedoTracking");
        String TorpedoTrackingPost = Utils.getString( "sms_descriptions", "TorpedoTrackingPost");
        String FMRRegen = Utils.getString( "sms_descriptions", "FMRRegen");
        String FMRRegenPost = Utils.getString( "sms_descriptions", "FMRRegenPost");
        String BurnDriveImpulse = Utils.getString( "sms_descriptions", "BurnDriveImpulse");
        String BurnDriveImpulsePost = Utils.getString( "sms_descriptions", "BurnDriveImpulsePost");
        String OrionDeviceDamage = Utils.getString( "sms_descriptions", "OrionDeviceDamage");
        String OrionDeviceDamagePost = Utils.getString( "sms_descriptions", "OrionDeviceDamagePost");
        String ArmorRepair = Utils.getString( "sms_descriptions", "ArmorRepair");
        String ArmorRepairPost = Utils.getString( "sms_descriptions", "ArmorRepairPost");
        String NovaBurstDamage = Utils.getString( "sms_descriptions", "NovaBurstDamage");
        String NovaBurstDamagePost = Utils.getString( "sms_descriptions", "NovaBurstDamagePost");
        String HullThresholdDR = Utils.getString( "sms_descriptions", "HullThresholdDR");
        String HullThresholdDRPost = Utils.getString( "sms_descriptions", "HullThresholdDRPost");
        String HullThresholdDRTitle = Utils.getString( "sms_descriptions", "HullThresholdDRTitle");
        String HullThresholdDRDesc1 = Utils.getString( "sms_descriptions", "HullThresholdDRDesc1");
        String FastSkimmer = Utils.getString( "sms_descriptions", "FastSkimmer");
        String FastSkimmerPost = Utils.getString( "sms_descriptions", "FastSkimmerPost");
        String SkimmerDR = Utils.getString( "sms_descriptions", "SkimmerDR");
        String SkimmerDRPost = Utils.getString( "sms_descriptions", "SkimmerDRPost");
        String SkimmerDRTitle = Utils.getString( "sms_descriptions", "SkimmerDRTitle");
        String SkimmerDRDesc1 = Utils.getString( "sms_descriptions", "SkimmerDRDesc1");
        String EfficiencyOverhaulBoost = Utils.getString( "sms_descriptions", "EfficiencyOverhaulBoost");
        String ExpandedCargoBoost = Utils.getString( "sms_descriptions", "ExpandedCargoBoost");
        String ExpandedFuelBoost = Utils.getString( "sms_descriptions", "ExpandedFuelBoost");
        String DroneStrikeBoost = Utils.getString( "sms_descriptions", "DroneStrikeBoost");
        String DroneStrikeRegen = Utils.getString( "sms_descriptions", "DroneStrikeRegen");
        String DroneStrikeRegenPost = Utils.getString( "sms_descriptions", "DroneStrikeRegenPost");
        String BackArmorBoost = Utils.getString( "sms_descriptions", "BackArmorBoost");
        String BackArmorBoostPost = Utils.getString( "sms_descriptions", "BackArmorBoostPost");
        String FlareLauncherWhileVenting = Utils.getString( "sms_descriptions", "FlareLauncherWhileVenting");
        String FlareLauncherWhileVentingPost = Utils.getString( "sms_descriptions", "FlareLauncherWhileVentingPost");
        String ShieldEfficiencyNearbyEnemies = Utils.getString( "sms_descriptions", "ShieldEfficiencyNearbyEnemies");
        String ShieldEfficiencyNearbyEnemiesPost = Utils.getString( "sms_descriptions", "ShieldEfficiencyNearbyEnemiesPost");
        String ShieldEfficiencyNearbyEnemiesTitle = Utils.getString( "sms_descriptions", "ShieldEfficiencyNearbyEnemiesTitle");
        String ShieldEfficiencyNearbyEnemiesDesc1 = Utils.getString( "sms_descriptions", "ShieldEfficiencyNearbyEnemiesDesc1");
        String VentingSpeedBoost = Utils.getString( "sms_descriptions", "VentingSpeedBoost");
        String VentingSpeedBoostTitle = Utils.getString( "sms_descriptions", "VentingSpeedBoostTitle");
        String VentingSpeedBoostDesc1 = Utils.getString( "sms_descriptions", "VentingSpeedBoostDesc1");
        String PlasmaJetsGrazeChance = Utils.getString( "sms_descriptions", "PlasmaJetsGrazeChance");
        String PlasmaJetsGrazeChancePost = Utils.getString( "sms_descriptions", "PlasmaJetsGrazeChancePost");
        String PlasmaJetsGrazeChanceTitle = Utils.getString( "sms_descriptions", "PlasmaJetsGrazeChanceTitle");
        String PlasmaJetsGrazeChanceDesc1 = Utils.getString( "sms_descriptions", "PlasmaJetsGrazeChanceDesc1");
        String PlasmaBurnEnergyRoF = Utils.getString( "sms_descriptions", "PlasmaBurnEnergyRoF");
        String PlasmaBurnEnergyRoFPost = Utils.getString( "sms_descriptions", "PlasmaBurnEnergyRoFPost");
        String PlasmaBurnEnergyRoFTitle = Utils.getString( "sms_descriptions", "PlasmaBurnEnergyRoFTitle");
        String PlasmaBurnEnergyRoFDesc1 = Utils.getString( "sms_descriptions", "PlasmaBurnEnergyRoFDesc1");
        String HEFRangeRoF = Utils.getString( "sms_descriptions", "HEFRangeRoF");
        String HEFRangeRoFPost = Utils.getString( "sms_descriptions", "HEFRangeRoFPost");
        String HEFRangeRoFTitle = Utils.getString( "sms_descriptions", "HEFRangeRoFTitle");
        String HEFRangeRoFDesc1 = Utils.getString( "sms_descriptions", "HEFRangeRoFDesc1");
        String RangeNotMoving = Utils.getString( "sms_descriptions", "RangeNotMoving");
        String RangeNotMovingPost = Utils.getString( "sms_descriptions", "RangeNotMovingPost");
        String RangeNotMovingTitle = Utils.getString( "sms_descriptions", "RangeNotMovingTitle");
        String RangeNotMovingDesc1 = Utils.getString( "sms_descriptions", "RangeNotMovingDesc1");
        String SafetyOverridesBoost = Utils.getString( "sms_descriptions", "SafetyOverridesBoost");
        String SafetyOverridesPPT = Utils.getString( "sms_descriptions", "SafetyOverridesPPT");
        String BuiltInMissileRegen = Utils.getString( "sms_descriptions", "BuiltInMissileRegen");
        String ConvertedCargoBayNoPenalty = Utils.getString( "sms_descriptions", "ConvertedCargoBayNoPenalty");
        String ExtraFighterPerWing = Utils.getString( "sms_descriptions", "ExtraFighterPerWing");
        String ExtraFighterPerWingPost = Utils.getString( "sms_descriptions", "ExtraFighterPerWingPost");
        String BurnDriveCooldown = Utils.getString( "sms_descriptions", "BurnDriveCooldown");
        String BurnDriveCooldownPost = Utils.getString( "sms_descriptions", "BurnDriveCooldownPost");
        String DamageTakenNearbyEnemies = Utils.getString( "sms_descriptions", "DamageTakenNearbyEnemies");
        String DamageTakenNearbyEnemiesPost = Utils.getString( "sms_descriptions", "DamageTakenNearbyEnemiesPost");
        String DamageTakenNearbyEnemiesTitle = Utils.getString( "sms_descriptions", "DamageTakenNearbyEnemiesTitle");
        String DamageTakenNearbyEnemiesDesc1 = Utils.getString( "sms_descriptions", "DamageTakenNearbyEnemiesDesc1");
        String LargeBallisticFragDamage = Utils.getString( "sms_descriptions", "LargeBallisticFragDamage");
        String LargeBallisticFragDamagePost = Utils.getString( "sms_descriptions", "LargeBallisticFragDamagePost");
        String PhasedCRDegradation = Utils.getString( "sms_descriptions", "PhasedCRDegradation");
        String ManeuveringJetsBoost = Utils.getString( "sms_descriptions", "ManeuveringJetsBoost");
        String ManeuveringJetsBoostTitle = Utils.getString( "sms_descriptions", "ManeuveringJetsBoostTitle");
        String ManeuveringJetsBoostDesc1 = Utils.getString( "sms_descriptions", "ManeuveringJetsBoostDesc1");
        String RangeNoNearbyEnemies = Utils.getString( "sms_descriptions", "RangeNoNearbyEnemies");
        String RangeNoNearbyEnemiesPost = Utils.getString( "sms_descriptions", "RangeNoNearbyEnemiesPost");
        String RangeNoNearbyEnemiesTitle = Utils.getString( "sms_descriptions", "RangeNoNearbyEnemiesTitle");
        String RangeNoNearbyEnemiesDesc1 = Utils.getString( "sms_descriptions", "RangeNoNearbyEnemiesDesc1");
        String EnergyProjectileRange = Utils.getString( "sms_descriptions", "EnergyProjectileRange");
        String RandomBeamIntensity = Utils.getString( "sms_descriptions", "RandomBeamIntensity");
        String RandomBeamIntensityPost = Utils.getString( "sms_descriptions", "RandomBeamIntensityPost");
        String RandomBeamIntensityTitle = Utils.getString( "sms_descriptions", "RandomBeamIntensityTitle");
        String RandomBeamIntensityDesc1 = Utils.getString( "sms_descriptions", "RandomBeamIntensityDesc1");
        String MissileAutoforgeExtraCharge = Utils.getString( "sms_descriptions", "MissileAutoforgeExtraCharge");
        String DEMBoost = Utils.getString( "sms_descriptions", "DEMBoost");
        String DEMBoostPost = Utils.getString( "sms_descriptions", "DEMBoostPost");
        String TargetingFeedBoost = Utils.getString( "sms_descriptions", "TargetingFeedBoost");
        String TargetingFeedBoostTitle = Utils.getString( "sms_descriptions", "TargetingFeedBoostTitle");
        String TargetingFeedBoostDesc1 = Utils.getString( "sms_descriptions", "TargetingFeedBoostDesc1");
        String TargetingFeedBoostDesc2 = Utils.getString( "sms_descriptions", "TargetingFeedBoostDesc2");
        String RegroupReplacement = Utils.getString( "sms_descriptions", "RegroupReplacement");
        String DamageChargesDamperField = Utils.getString( "sms_descriptions", "DamageChargesDamperField");
        String DamageChargesDamperFieldPost = Utils.getString( "sms_descriptions", "DamageChargesDamperFieldPost");
        String DamageChargesDamperFieldPost2 = Utils.getString( "sms_descriptions", "DamageChargesDamperFieldPost2");
        String DamageChargesDamperFieldTitle = Utils.getString( "sms_descriptions", "DamageChargesDamperFieldTitle");
        String DamageChargesDamperFieldDesc1 = Utils.getString( "sms_descriptions", "DamageChargesDamperFieldDesc1");
        String DamageChargesDamperFieldDesc2 = Utils.getString( "sms_descriptions", "DamageChargesDamperFieldDesc2");
        String DamperFieldFighters = Utils.getString( "sms_descriptions", "DamperFieldFighters");
        String ExpandedCrewBoost = Utils.getString( "sms_descriptions", "ExpandedCrewBoost");
        String ExtraMiningDrones = Utils.getString( "sms_descriptions", "ExtraMiningDrones");
        String ExtraMiningDronesPost = Utils.getString( "sms_descriptions", "ExtraMiningDronesPost");
        String LimitedArmorRegen = Utils.getString( "sms_descriptions", "LimitedArmorRegen");
        String LimitedArmorRegenPost = Utils.getString( "sms_descriptions", "LimitedArmorRegenPost");
        String FMRNoFlux = Utils.getString( "sms_descriptions", "FMRNoFlux");
        String HammerLingeringDamage = Utils.getString( "sms_descriptions", "HammerLingeringDamage");
        String HammerLingeringDamagePost = Utils.getString( "sms_descriptions", "HammerLingeringDamagePost");
        String FlareRegen = Utils.getString( "sms_descriptions", "FlareRegen");
        String FlareRegenPost = Utils.getString( "sms_descriptions", "FlareRegenPost");
        String FMRFastReplacement = Utils.getString( "sms_descriptions", "FMRFastReplacement");
        String PilumSalamanderBoost = Utils.getString( "sms_descriptions", "PilumSalamanderBoost");
        String PilumName = Utils.getString( "sms_descriptions", "PilumName");
        String SalamanderName = Utils.getString( "sms_descriptions", "SalamanderName");
        String FasterActiveFlares = Utils.getString( "sms_descriptions", "FasterActiveFlares");
        String BDeckExtraCharges = Utils.getString( "sms_descriptions", "BDeckExtraCharges");
        String BDeckExtraChargesPost = Utils.getString( "sms_descriptions", "BDeckExtraChargesPost");
        String HEFMissileBoost = Utils.getString( "sms_descriptions", "HEFMissileBoost");
        String HEFMissileBoostPost = Utils.getString( "sms_descriptions", "HEFMissileBoostPost");
        String HEFMissileBoostTitle = Utils.getString( "sms_descriptions", "HEFMissileBoostTitle");
        String HEFMissileBoostDesc1 = Utils.getString( "sms_descriptions", "HEFMissileBoostDesc1");
        String ReserveDeploymentBoost = Utils.getString( "sms_descriptions", "ReserveDeploymentBoost");
        String ReserveDeploymentBoostPost = Utils.getString( "sms_descriptions", "ReserveDeploymentBoostPost");
        String FighterCountDR = Utils.getString( "sms_descriptions", "FighterCountDR");
        String FighterCountDRPost = Utils.getString( "sms_descriptions", "FighterCountDRPost");
        String FighterCountDRTitle = Utils.getString( "sms_descriptions", "FighterCountDRTitle");
        String FighterCountDRDesc1 = Utils.getString( "sms_descriptions", "FighterCountDRDesc1");
        String QuantumDisruptorDuration = Utils.getString( "sms_descriptions", "QuantumDisruptorDuration");
        String PhasedArmorRepair = Utils.getString( "sms_descriptions", "PhasedArmorRepair");
        String PhasedArmorRepairPost = Utils.getString( "sms_descriptions", "PhasedArmorRepairPost");
        String MissileAutoloaderCapacity = Utils.getString( "sms_descriptions", "MissileAutoloaderCapacity");
        String CanisterFlakWhileVenting = Utils.getString( "sms_descriptions", "CanisterFlakWhileVenting");
        String BallisticFireRateFluxLevel = Utils.getString( "sms_descriptions", "BallisticFireRateFluxLevel");
        String BallisticFireRateFluxLevelPost = Utils.getString( "sms_descriptions", "BallisticFireRateFluxLevelPost");
        String BallisticFireRateFluxLevelTitle = Utils.getString( "sms_descriptions", "BallisticFireRateFluxLevelTitle");
        String BallisticFireRateFluxLevelDesc1 = Utils.getString( "sms_descriptions", "BallisticFireRateFluxLevelDesc1");
        String BallisticFireRateFluxLevelDesc2 = Utils.getString( "sms_descriptions", "BallisticFireRateFluxLevelDesc2");
        String SkimmerEMP = Utils.getString( "sms_descriptions", "SkimmerEMP");
        String SkimmerEMPPost = Utils.getString( "sms_descriptions", "SkimmerEMPPost");
        String ShieldEfficiencyHardFlux = Utils.getString( "sms_descriptions", "ShieldEfficiencyHardFlux");
        String ShieldEfficiencyHardFluxPost = Utils.getString( "sms_descriptions", "ShieldEfficiencyHardFluxPost");
        String ShieldEfficiencyHardFluxTitle = Utils.getString( "sms_descriptions", "ShieldEfficiencyHardFluxTitle");
        String ShieldEfficiencyHardFluxDesc1 = Utils.getString( "sms_descriptions", "ShieldEfficiencyHardFluxDesc1");
        String ConvertedHangarNoPenalty = Utils.getString( "sms_descriptions", "ConvertedHangarNoPenalty");
        String MinimumReplacementRate = Utils.getString( "sms_descriptions", "MinimumReplacementRate");
        String MinimumReplacementRatePost = Utils.getString( "sms_descriptions", "MinimumReplacementRatePost");
        String VentSpeedFluxLevel = Utils.getString( "sms_descriptions", "VentSpeedFluxLevel");
        String VentSpeedFluxLevelTitle = Utils.getString( "sms_descriptions", "VentSpeedFluxLevelTitle");
        String VentSpeedFluxLevelDesc1 = Utils.getString( "sms_descriptions", "VentSpeedFluxLevelDesc1");
        String VentSpeedFluxLevelDesc2 = Utils.getString( "sms_descriptions", "VentSpeedFluxLevelDesc2");
        String EntropyAmplifierChaining = Utils.getString( "sms_descriptions", "EntropyAmplifierChaining");
        String EntropyAmplifierChainingPost = Utils.getString( "sms_descriptions", "EntropyAmplifierChainingPost");
        String EntropyAmplifierChainingDesc1 = Utils.getString( "sms_descriptions", "EntropyAmplifierChainingDesc1");
        String EntropyAmplifierMobility = Utils.getString( "sms_descriptions", "EntropyAmplifierMobility");
        String EntropyAmplifierMobilityDesc1 = Utils.getString( "sms_descriptions", "EntropyAmplifierMobilityDesc1");
        String PlasmaJetsDissipation = Utils.getString( "sms_descriptions", "PlasmaJetsDissipation");
        String DamperFieldDissipation = Utils.getString( "sms_descriptions", "DamperFieldDissipation");
        String DamperFieldDissipationTitle = Utils.getString( "sms_descriptions", "DamperFieldDissipationTitle");
        String DamperFieldDissipationDesc1 = Utils.getString( "sms_descriptions", "DamperFieldDissipationDesc1");
        String MakeshiftShieldBoost = Utils.getString( "sms_descriptions", "MakeshiftShieldBoost");
        String DecoyFlareBoost = Utils.getString( "sms_descriptions", "DecoyFlareBoost");
        String MinimumCR = Utils.getString( "sms_descriptions", "MinimumCR");
        String PhaseTeleporterTimeFlow = Utils.getString( "sms_descriptions", "PhaseTeleporterTimeFlow");
        String PhaseTeleporterTimeFlowPost = Utils.getString( "sms_descriptions", "PhaseTeleporterTimeFlowPost");
        String PhaseTeleporterTimeFlowTitle = Utils.getString( "sms_descriptions", "PhaseTeleporterTimeFlowTitle");
        String PhaseTeleporterTimeFlowDesc1 = Utils.getString( "sms_descriptions", "PhaseTeleporterTimeFlowDesc1");
        String HEFShieldEfficiency = Utils.getString( "sms_descriptions", "HEFShieldEfficiency");
        String HEFShieldEfficiencyTitle = Utils.getString( "sms_descriptions", "HEFShieldEfficiencyTitle");
        String HEFShieldEfficiencyDesc1 = Utils.getString( "sms_descriptions", "HEFShieldEfficiencyDesc1");
        String ECMPackageBoost = Utils.getString( "sms_descriptions", "ECMPackageBoost");
        String NavRelayBoost = Utils.getString( "sms_descriptions", "NavRelayBoost");
        String OperationsCenterBoost = Utils.getString( "sms_descriptions", "OperationsCenterBoost");
        String ReactiveFortressShield = Utils.getString( "sms_descriptions", "ReactiveFortressShield");
        String ReactiveFortressShieldPost = Utils.getString( "sms_descriptions", "ReactiveFortressShieldPost");
        String EMPEmitterFluxBoost = Utils.getString( "sms_descriptions", "EMPEmitterFluxBoost");
        String EMPEmitterFluxBoostPost = Utils.getString( "sms_descriptions", "EMPEmitterFluxBoostPost");
        String EMPEmitterFluxBoostTitle = Utils.getString( "sms_descriptions", "EMPEmitterFluxBoostTitle");
        String EMPEmitterFluxBoostDesc1 = Utils.getString( "sms_descriptions", "EMPEmitterFluxBoostDesc1");
        String DriveFieldStabilizerBoost = Utils.getString( "sms_descriptions", "DriveFieldStabilizerBoost");
        String GroundSupportBoost = Utils.getString( "sms_descriptions", "GroundSupportBoost");
        String TemporalShellCRDegradation = Utils.getString( "sms_descriptions", "TemporalShellCRDegradation");
        String EMPEmitterEnergyDamage = Utils.getString( "sms_descriptions", "EMPEmitterEnergyDamage");
        String EMPEmitterFragDamage = Utils.getString( "sms_descriptions", "EMPEmitterFragDamage");
        String MaxAutomatedCR = Utils.getString( "sms_descriptions", "MaxAutomatedCR");
        String TimeFlowNearbyEnemies = Utils.getString( "sms_descriptions", "TimeFlowNearbyEnemies");
        String TimeFlowNearbyEnemiesPost = Utils.getString( "sms_descriptions", "TimeFlowNearbyEnemiesPost");
        String TimeFlowNearbyEnemiesTitle = Utils.getString( "sms_descriptions", "TimeFlowNearbyEnemiesTitle");
        String TimeFlowNearbyEnemiesDesc1 = Utils.getString( "sms_descriptions", "TimeFlowNearbyEnemiesDesc1");
        String ApplyOfficerSkill = Utils.getString( "sms_descriptions", "ApplyOfficerSkill");
        String ShieldHullmodPackage = Utils.getString( "sms_descriptions", "ShieldHullmodPackage");
        String ArmorHullmodPackage = Utils.getString( "sms_descriptions", "ArmorHullmodPackage");
        String ArmorHullmodPackagePost = Utils.getString( "sms_descriptions", "ArmorHullmodPackagePost");
        String HullHullmodPackage = Utils.getString( "sms_descriptions", "HullHullmodPackage");
        String EngineHullmodPackage = Utils.getString( "sms_descriptions", "EngineHullmodPackage");
        String FighterHullmodPackage = Utils.getString( "sms_descriptions", "FighterHullmodPackage");
        String FluxHullmodPackage = Utils.getString( "sms_descriptions", "FluxHullmodPackage");
        String BuiltInHSA = Utils.getString( "sms_descriptions", "BuiltInHSA");
        String AmmoHullmodPackage = Utils.getString( "sms_descriptions", "AmmoHullmodPackage");
        String BuiltInNeuralIntegrator = Utils.getString( "sms_descriptions", "BuiltInNeuralIntegrator");
        String EmptyMountsReduceFlux = Utils.getString( "sms_descriptions", "EmptyMountsReduceFlux");
        String EmptyMountsReduceFluxPost = Utils.getString( "sms_descriptions", "EmptyMountsReduceFluxPost");
    }
}
