package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
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
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.campaign.items.BetaKCorePlugin;
import shipmastery.util.IntRef;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Random;

public class SeekerOfficerPlugin extends BaseGenerateFleetOfficersPlugin {
    @Override
    public void addCommanderAndOfficers(CampaignFleetAPI fleet, FleetParamsV3 params, Random random) {
        // In case a random fleet patrol is generated, etc.
        if (params.aiCores == null) {
            params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        }

        WeightedRandomPicker<String> officerPicker = new WeightedRandomPicker<>(random);
        switch (params.aiCores) {
            case AI_GAMMA: {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add("sms_gamma_k_core", 0.5f);
                officerPicker.add(null, 1.5f);
                break;
            }
            case AI_BETA_OR_GAMMA: {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add("sms_gamma_k_core", 1f);
                officerPicker.add("sms_beta_k_core", 0.5f);
                officerPicker.add(null, 1.5f);
                break;
            }
            case AI_BETA: case AI_MIXED: {
                officerPicker.add("sms_fractured_gamma_core", 0.25f);
                officerPicker.add("sms_gamma_k_core", 1f);
                officerPicker.add("sms_beta_k_core", 1f);
                officerPicker.add("sms_alpha_k_core", 0.4f);
                officerPicker.add(null, 1f);
                break;
            }
            case AI_ALPHA: {
                officerPicker.add("sms_fractured_gamma_core", 0.5f);
                officerPicker.add("sms_gamma_k_core", 1f);
                officerPicker.add("sms_beta_k_core", 1.5f);
                officerPicker.add("sms_alpha_k_core", 2f);
                break;
            }
            case AI_OMEGA: {
                officerPicker.add("sms_alpha_k_core", 1f);
                break;
            }
        }

        var pluginCache = new HashMap<String, AICoreOfficerPlugin>();

        float biggestCommanderScore = 0f;
        PersonAPI commander = null;
        FleetMemberAPI flagship = null;
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            String coreId = officerPicker.pick();
            if (coreId == null) continue;
            var plugin = pluginCache.computeIfAbsent(coreId, k -> Misc.getAICoreOfficerPlugin(coreId));
            if (plugin == null) continue;
            var person = plugin.createPerson(coreId, "sms_seeker", random);
            assignOfficerSkillsAndIntegrate(person, fm, random);
            fm.setCaptain(person);

            float commanderScore = switch (coreId) {
                case "sms_fractured_gamma_core" -> 1000f;
                case "sms_gamma_k_core" -> 10000f;
                case "sms_beta_k_core" -> 100000f;
                case "sms_alpha_k_core" -> 1000000f;
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
            int numCommanderSkills = switch (commander.getAICoreId()) {
                case "sms_fractured_gamma_core" -> 1;
                case "sms_gamma_k_core" -> 3;
                case "sms_beta_k_core" -> 5;
                case "sms_alpha_k_core" -> 7;
                default -> 2;
            };
            RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, params, numCommanderSkills, random);
        }
    }

    private void assignOfficerSkillsAndIntegrate(PersonAPI captain, FleetMemberAPI fm, Random random) {
        if (fm.getStats() == null || fm.getVariant() == null || fm.getHullSpec() == null) return;

        captain.getStats().setLevel(captain.getStats().getLevel() + 1);
        captain.getStats().setSkipRefresh(true);
        // First, remove all existing skills *except* the unique one, if it exists
        IntRef numSkillsRemoved = new IntRef(0);
        var skills = ((CharacterStats) captain.getStats()).getSkills();
        skills.removeIf(skill -> {
            if (!skill.getSkill().isCombatOfficerSkill()) return false;
            if (!BetaKCorePlugin.UNIQUE_SKILL_ID.equals(skill.getSkill().getId())) {
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
            // no charges
            if (systemSpec.getMaxUses(fm.getStats()) == Integer.MAX_VALUE) {
                skillsPicker.add(Skills.SYSTEMS_EXPERTISE, 2.5f);
            }
            // charges
            else {
                skillsPicker.add(Skills.SYSTEMS_EXPERTISE, 5f);
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
            return data.fleet != null && "sms_seeker".equals(data.fleet.getFaction().getId()) ? 1000 : -1;
        }
    }
}