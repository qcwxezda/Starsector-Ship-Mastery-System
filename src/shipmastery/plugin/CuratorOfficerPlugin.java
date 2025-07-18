package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.BaseGenerateFleetOfficersPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.items.PseudocorePlugin;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.IntRef;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Random;

public class CuratorOfficerPlugin extends BaseGenerateFleetOfficersPlugin {

    public static final float REPLACE_WARPED_PROB = 0.03f;
    public static final float REPLACE_CRYSTALLINE_PROB = 0.03f;

    @Override
    public void addCommanderAndOfficers(CampaignFleetAPI fleet, FleetParamsV3 params, Random random) {
        // In case a random fleet patrol is generated, etc.
        if (params.aiCores == null) {
            params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        }

        WeightedRandomPicker<String> officerPicker = new WeightedRandomPicker<>(random);
        switch (params.aiCores) {
            case AI_GAMMA -> {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add(null, 1.5f);
            }
            case AI_BETA_OR_GAMMA -> {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add("sms_gamma_pseudocore", 1f);
                officerPicker.add(null, 1.5f);
            }
            case AI_BETA -> {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add("sms_gamma_pseudocore", 1.5f);
                officerPicker.add("sms_beta_pseudocore", 1.5f);
                officerPicker.add(null, 0.5f);
            }
            case AI_MIXED -> {
                officerPicker.add("sms_fractured_gamma_core", 0.25f);
                officerPicker.add("sms_gamma_pseudocore", 0.75f);
                officerPicker.add("sms_beta_pseudocore", 1.5f);
                officerPicker.add("sms_alpha_pseudocore", 0.75f);
            }
            case AI_ALPHA -> {
                officerPicker.add("sms_fractured_gamma_core", 0.25f);
                officerPicker.add("sms_gamma_pseudocore", 0.5f);
                officerPicker.add("sms_beta_pseudocore", 1f);
                officerPicker.add("sms_alpha_pseudocore", 2f);
            }
            case AI_OMEGA -> officerPicker.add("sms_alpha_pseudocore", 1f);
        }

        float biggestCommanderScore = 0f;
        PersonAPI commander = null;
        FleetMemberAPI flagship = null;
        var mem = fleet.getMemoryWithoutUpdate();
        String fleetType = mem == null ? null : mem.getString("$fleetType");
        boolean isNucleusDefender = Strings.Campaign.NUCLEUS_DEFENDER_FLEET_TYPE.equals(fleetType);
        boolean isRemoteDefender = Strings.Campaign.REMOTE_BEACON_DEFENDER_FLEET_TYPE.equals(fleetType);
        float replaceCrystallineProb = REPLACE_CRYSTALLINE_PROB;
        float replaceWarpedProb = REPLACE_WARPED_PROB;
        if (isNucleusDefender) {
            replaceCrystallineProb = 0.2f;
            replaceWarpedProb = 0.2f;
        } else if (isRemoteDefender) {
            replaceCrystallineProb = 0.33f;
            replaceWarpedProb = 0.33f;
        }

        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            String coreId = officerPicker.pick();
            if (coreId != null && !"sms_fractured_gamma_core".equals(coreId)) {
                if (random.nextFloat() <= replaceCrystallineProb) {
                    coreId = "sms_crystalline_pseudocore";
                } else if (random.nextFloat() <= replaceWarpedProb) {
                    coreId = "sms_warped_pseudocore";
                }
            }
            if (isRemoteDefender && "tesseract".equals(fm.getHullId())) {
                coreId = "sms_amorphous_pseudocore";
            }
            if (coreId == null) continue;
            // Don't use Misc.getAICoreOfficerPlugin because we only register the plugin on game load, but we
            // need the plugin on game enable (to set AI core for custom station)
            var plugin = PseudocorePlugin.getPluginForPseudocore(coreId);
            if (plugin == null) continue;
            var person = plugin.createPerson(coreId, "sms_curator", random);
            assignOfficerSkillsAndIntegrate(person, fm, random);
            fm.setCaptain(person);

            float commanderScore = switch (coreId) {
                case "sms_fractured_gamma_core" -> 1000f;
                case "sms_gamma_pseudocore" -> 10000f;
                case "sms_beta_pseudocore" -> 100000f;
                case "sms_warped_pseudocore", "sms_crystalline_pseudocore" -> 250000f;
                case "sms_alpha_pseudocore" -> 1000000f;
                case "sms_amorphous_pseudocore" -> 10000000f;
                default -> 0f;
            };
            commanderScore += fm.getFleetPointCost();
            if (commanderScore > biggestCommanderScore) {
                biggestCommanderScore = commanderScore;
                commander = person;
                flagship = fm;
            }
        }

        // commander can be null if the entire fleet has no cores (for a small fleet)
        if (commander != null) {
            commander.setRankId(Ranks.SPACE_COMMANDER);
            commander.setPostId(Ranks.POST_FLEET_COMMANDER);
            fleet.setCommander(commander);
            fleet.getFleetData().setFlagship(flagship);
            int numCommanderSkills = getNumCommanderSkillsAndSetMinMastery(commander);
            RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, params, numCommanderSkills, random);
        }

        // Commander skill reduces DP, which might affect CR bonuses that depend on a DP threshold (crew training)
        for (var fm : fleet.getFleetData().getMembersListCopy()) {
            // Have to wait a few frames or hovering over a ship shows it's not actually repaired...
            DeferredActionPlugin.performLater(() -> fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR()), 0.1f);
        }
    }

    private static int getNumCommanderSkillsAndSetMinMastery(PersonAPI commander) {
        int numCommanderSkills;
        var memory = commander.getMemoryWithoutUpdate();
        String key = FleetHandler.MINIMUM_MASTERY_LEVEL_KEY;
        switch (commander.getAICoreId()) {
            case "sms_fractured_gamma_core" -> {
                numCommanderSkills = 1;
                memory.set(key, 1f);
            }
            case "sms_gamma_pseudocore" -> {
                numCommanderSkills = 2;
                memory.set(key, 3f);
            }
            case "sms_beta_pseudocore" -> {
                numCommanderSkills = 3;
                memory.set(key, 5f);
            }
            case "sms_warped =_pseudocore", "sms_crystalline_pseudocore" -> {
                numCommanderSkills = 4;
                memory.set(key, 6f);
            }
            case "sms_alpha_pseudocore" -> {
                numCommanderSkills = 5;
                memory.set(key, 6.5f);
            }
            case "sms_amorphous_pseudocore" -> {
                numCommanderSkills = 5;
                commander.getStats().setSkillLevel("carrier_group", 1f);
                commander.getStats().setSkillLevel("fighter_uplink", 1f);
                memory.set(key, 999999f);
            }
            default -> numCommanderSkills = 0;
        }
        return numCommanderSkills;
    }

    private void assignOfficerSkillsAndIntegrate(PersonAPI captain, FleetMemberAPI fm, Random random) {
        if (fm.getStats() == null || fm.getVariant() == null || fm.getHullSpec() == null) return;

        captain.getStats().setLevel(captain.getStats().getLevel() + 1);
        captain.getStats().setSkipRefresh(true);
        // First, remove all existing skills *except* the unique ones if they exist
        IntRef numSkillsRemoved = new IntRef(0);
        var skills = ((CharacterStats) captain.getStats()).getSkills();
        skills.removeIf(skill -> {
            if (!skill.getSkill().isCombatOfficerSkill()) return false;
            if (Utils.combatSkillIds.contains(skill.getSkill().getId())) {
                numSkillsRemoved.value++;
                return true;
            }
            return false;
        });

        // Now, figure out what skills we'd like to set
        // Note: all skills are elite
        WeightedRandomPicker<String> skillsPicker = new WeightedRandomPicker<>(random);

        // Helmsmanship: since flat bonus benefits slow ships more, assign greater weight the slower the ship is
        float speed = fm.getStats().getMaxSpeed().getModifiedValue();
        skillsPicker.add(Skills.HELMSMANSHIP, Utils.getSelectionWeightScaledByValueDecreasing(speed, 20f, 50f, 180f, 5f));

        // Combat endurance: prefer ships with high hull, prefer ships with CR <= 85%
        float hull = fm.getStats().getHullBonus().computeEffective(fm.getHullSpec().getHitpoints());
        float modifiedHullCE = hull * 1.3f - fm.getRepairTracker().getMaxCR();
        skillsPicker.add(Skills.COMBAT_ENDURANCE, Utils.getSelectionWeightScaledByValueIncreasing(modifiedHullCE, 500f, 3000f, 12000f, 5f));

        // Impact mitigation: prefer high armor ships, prefer capitals
        float armor = fm.getStats().getArmorBonus().computeEffective(fm.getHullSpec().getArmorRating());
        float modifiedArmorIM = fm.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP ?  armor * 1.5f : armor;
        skillsPicker.add(Skills.IMPACT_MITIGATION, Utils.getSelectionWeightScaledByValueIncreasing(modifiedArmorIM, 250f, 750f, 2500f, 5f));

        // Damage control: prefer high hull ships, prefer capitals
        float modifiedHullDC = fm.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP ? hull * 1.5f : hull;
        skillsPicker.add(Skills.DAMAGE_CONTROL, Utils.getSelectionWeightScaledByValueIncreasing(modifiedHullDC, 1000f, 6000f, 30000f, 5f));

        // Field modulation: prefer ships with greater shield HP, strongly prefer if phase ship
        float shieldHP = fm.getStats().getFluxCapacity().getModifiedValue() / fm.getStats().getShieldDamageTakenMult().getModifiedValue();
        if (fm.isPhaseShip()) {
            skillsPicker.add(Skills.FIELD_MODULATION, 10f);
        } else {
            skillsPicker.add(Skills.FIELD_MODULATION, Utils.getSelectionWeightScaledByValueIncreasing(shieldHP, 1000f, 4000f, 20000f, 5f));
        }

        // Point defense: prefer ships with lots of PD weapons, just copy vanilla implementation
        float pdWeight = 0f;
        float ballisticWeight = 0f;
        float energyWeight = 0f;
        float missileWeight = 0f;
        for (var weaponId : fm.getVariant().getFittedWeaponSlots()) {
            var spec = fm.getVariant().getWeaponSpec(weaponId);
            float size = switch (spec.getSize()) {
                case SMALL -> 1f;
                case MEDIUM -> 2f;
                case LARGE -> 4f;
            };
            if (spec.getAIHints().contains(WeaponAPI.AIHints.PD)) {
                pdWeight += size;
            }
            switch (spec.getType()) {
                case BALLISTIC -> ballisticWeight += size;
                case ENERGY -> energyWeight += size;
                case MISSILE -> missileWeight += size;
            }
        }
        skillsPicker.add(Skills.POINT_DEFENSE, Utils.getSelectionWeightScaledByValueIncreasing(pdWeight, 2f, 6f, 16f, 5f));

        // Target analysis: just all around good
        skillsPicker.add(Skills.TARGET_ANALYSIS, 5f);

        // Ballistic mastery, EWM, missile spec: based on weapons count
        skillsPicker.add(Skills.BALLISTIC_MASTERY, Utils.getSelectionWeightScaledByValueIncreasing(ballisticWeight, 2f, 6f, 16f, 5f));
        skillsPicker.add(Skills.ENERGY_WEAPON_MASTERY, Utils.getSelectionWeightScaledByValueIncreasing(energyWeight, 2f, 6f, 16f, 5f));
        skillsPicker.add(Skills.MISSILE_SPECIALIZATION, Utils.getSelectionWeightScaledByValueIncreasing(missileWeight, 2f, 6f, 16f, 5f));

        // Systems expertise: don't pick if ship has no system, otherwise prefer charges over no charges
        String systemId = fm.getHullSpec().getShipSystemId();
        if (systemId != null) {
            var systemSpec = Global.getSettings().getShipSystemSpec(fm.getHullSpec().getShipSystemId());
            if (systemSpec != null) {
                // no charges
                if (systemSpec.getMaxUses(fm.getStats()) == Integer.MAX_VALUE) {
                    skillsPicker.add(Skills.SYSTEMS_EXPERTISE, 2.5f);
                }
                // charges
                else {
                    skillsPicker.add(Skills.SYSTEMS_EXPERTISE, 5f);
                }
            }
        }

        // Gunnery implants and ordnance expertise: prefer by default
        skillsPicker.add(Skills.GUNNERY_IMPLANTS, 5f);
        skillsPicker.add(Skills.ORDNANCE_EXPERTISE, 5f);

        // Polarized armor: scale based on armor
        skillsPicker.add(Skills.POLARIZED_ARMOR, Utils.getSelectionWeightScaledByValueIncreasing(armor, 300f, 900f, 2500f, 5f));

        // +1 for integration
        for (int i = 0; i < numSkillsRemoved.value + 1; i++) {
            if (skillsPicker.isEmpty()) break;
            captain.getStats().setSkillLevel(skillsPicker.pickAndRemove(), 2f);
        }
        captain.getStats().setSkipRefresh(false);
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof GenerateFleetOfficersPickData data)) {
            return -1;
        } else if (data.params == null || !data.params.withOfficers) {
            return -1;
        } else {
            return data.fleet != null && "sms_curator".equals(data.fleet.getFaction().getId()) ? 1000 : -1;
        }
    }
}