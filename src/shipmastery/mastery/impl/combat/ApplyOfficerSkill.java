package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillEffectType;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.CampaignUtils;
import shipmastery.util.IntRef;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static shipmastery.util.MasteryUtils.makeSharedId;

public class ApplyOfficerSkill extends BaseMasteryEffect {

    String skillId;
    public static final Set<String> SUPPORT_DOCTRINE_SKILL_IDS = new HashSet<>();
    static {
        SUPPORT_DOCTRINE_SKILL_IDS.add(Skills.HELMSMANSHIP);
        SUPPORT_DOCTRINE_SKILL_IDS.add(Skills.COMBAT_ENDURANCE);
        SUPPORT_DOCTRINE_SKILL_IDS.add(Skills.DAMAGE_CONTROL);
        SUPPORT_DOCTRINE_SKILL_IDS.add(Skills.ORDNANCE_EXPERTISE);
    }

    @Override
    public MasteryEffect postInit(String... args) {
        if (args.length < 2) throw new RuntimeException("ApplyOfficerSkill initialized without a skill argument");
        skillId = args[1];
        return this;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ApplyOfficerSkill).params(
                Global.getSettings().getSkillSpec(skillId).getName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ApplyOfficerSkillPost, 0f);
    }

    private int getSkillLevelForStats(MutableShipStatsAPI stats) {
        if (stats == null) return 0;
        PersonAPI captain;
        if (stats.getEntity() instanceof ShipAPI ship) {
            captain = ship.getCaptain();
        } else {
            captain = stats.getFleetMember() == null ? null : stats.getFleetMember().getCaptain();
        }
        boolean noCaptain = captain == null || captain.isDefault() || captain.getStats() == null;
        return noCaptain ? 0 : (int) captain.getStats().getSkillLevel(skillId);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        int existingLevel = getSkillLevelForStats(ship.getMutableStats());
        if (existingLevel >= 2) return;

        applyEffectsBeforeShipCreation(ship.getHullSize(), ship.getMutableStats());
        Global.getSettings().getSkillSpec(skillId).getEffectsAPI().forEach(
                effect -> {
                    if (effect.getType() != SkillEffectType.SHIP) return;
                    var afterCreationEffect = effect.getAsAfterShipCreationEffect();
                    if (afterCreationEffect == null || effect.getRequiredSkillLevel() != existingLevel + 1) return;
                    if (effect.getRequiredSkillLevel() == 1 && isNoOfficer(ship.getMutableStats())) {
                        var commander = ship.getFleetCommander();
                        if (commander != null && commander.getStats().getSkillLevel(Skills.SUPPORT_DOCTRINE) > 0f && SUPPORT_DOCTRINE_SKILL_IDS.contains(skillId)) {
                            return;
                        }
                    }
                    if (SUPPORT_DOCTRINE_SKILL_IDS.contains(skillId)) {
                        DeferredActionPlugin.performLater(() -> afterCreationEffect.unapplyEffectsAfterShipCreation(ship, "support_doctrine_ships_0"), 0f);
                        // SiC version of support doctrine, seems to be a hullmod so can just unapply (can't unapply character skills b/c they happen after hullmods)
                        afterCreationEffect.unapplyEffectsAfterShipCreation(ship, "sc_skill_controller_sc_smallcraft_support_doctrine");
                    }
                    afterCreationEffect.unapplyEffectsAfterShipCreation(ship, makeSharedId(this));
                    afterCreationEffect.applyEffectsAfterShipCreation(ship, makeSharedId(this));
                }
        );
    }

    public static boolean isNoOfficer(MutableShipStatsAPI stats) {
        if (stats.getEntity() instanceof ShipAPI ship) {
            return ship.getCaptain() == null || ship.getCaptain().isDefault();
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return true;
            return member.getCaptain().isDefault();
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        int existingLevel = getSkillLevelForStats(stats);
        if (existingLevel >= 2) return;

        IntRef count = new IntRef(0);
        Global.getSettings().getSkillSpec(skillId).getEffectsAPI().forEach(
                effect -> {
                    if (effect.getType() != SkillEffectType.SHIP) return;
                    if (effect.getRequiredSkillLevel() != existingLevel + 1) return;
                    if (effect.getRequiredSkillLevel() == 1 && isNoOfficer(stats)) {
                        var commander = CampaignUtils.getFleetCommanderForStats(stats);
                        if (commander != null && commander.getStats().getSkillLevel(Skills.SUPPORT_DOCTRINE) > 0f && SUPPORT_DOCTRINE_SKILL_IDS.contains(skillId)) {
                            return;
                        }
                    }
                    // SiC version of support doctrine, seems to be a hullmod so can just unapply (can't unapply character skills b/c they happen after hullmods)
                    if (SUPPORT_DOCTRINE_SKILL_IDS.contains(skillId)) {
                        effect.getAsShipEffect().unapply(stats, hullSize, "sc_skill_controller_sc_smallcraft_support_doctrine");
                    }
                    effect.getAsShipEffect().apply(stats, hullSize, skillId + "_ship_" + count.value, effect.getRequiredSkillLevel());
                    count.value++;
                }
        );
    }

    @Override
    public List<String> generateRandomArgs(ShipHullSpecAPI spec, int maxTier, long seed) {
        List<String> skillIds = Global.getSettings().getSkillIds();
        Set<String> seenIds = new HashSet<>();
        for (String[] args : getAllUsedArgs()) {
            if (args.length >= 2) {
                seenIds.add(args[1]);
            }
        }

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        picker.setRandom(new Random(seed));
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float energyWeight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.ENERGY, 0.2f, 0.3f);
        float ballisticWeight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.BALLISTIC, 0.2f, 0.3f);
        float missileWeight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f);

        for (String id : skillIds) {
            SkillSpecAPI skill = Global.getSettings().getSkillSpec(id);
            if (skill.hasTag("deprecated")) continue;
            if (Utils.combatSkillIds.contains(id) && !seenIds.contains(id) && maxTier >= getSkillTier(id)) {
                boolean valid = true;
                switch (id) {
                    case Skills.FIELD_MODULATION:
                        if (!Utils.hasShield(spec)) valid = false; break;
                    case Skills.BALLISTIC_MASTERY:
                        if (ballisticWeight < 0.3f) valid = false; break;
                    case Skills.SYSTEMS_EXPERTISE:
                        if (spec.getShipSystemId() == null) valid = false; break;
                    case Skills.MISSILE_SPECIALIZATION:
                        if (missileWeight < 0.3f) valid = false; break;
                    case Skills.ENERGY_WEAPON_MASTERY:
                        if (energyWeight < 0.3f) valid = false; break;
                    default: break;
                }

                if (valid) {
                    picker.add(id);
                }
            }
        }

        if (picker.isEmpty()) return null;
        return Collections.singletonList(picker.pick());
    }

    private int getSkillTier(String id) {
        if (id.equals(Skills.MISSILE_SPECIALIZATION) || id.equals(Skills.SYSTEMS_EXPERTISE))
            return 2;
        return 1;
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return 2.5f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        boolean applies;
        if (fm.getCaptain() == null || fm.getCaptain().isDefault()) {
            applies = true;
        } else {
            applies = fm.getCaptain().getStats().getSkillLevel(skillId) < 2f;
        }
        return applies ? super.getNPCWeight(fm) : 0f;
    }
}
