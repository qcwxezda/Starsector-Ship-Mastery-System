package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.combat.entities.Ship;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ApplyOfficerSkill extends BaseMasteryEffect {

    String skillId;
    private static MethodHandle applyPersonalToStats;
    private static final Set<String> supportDoctrineSkillIds = new HashSet<>();
    static {
        supportDoctrineSkillIds.add(Skills.HELMSMANSHIP);
        supportDoctrineSkillIds.add(Skills.COMBAT_ENDURANCE);
        supportDoctrineSkillIds.add(Skills.DAMAGE_CONTROL);
        supportDoctrineSkillIds.add(Skills.ORDNANCE_EXPERTISE);
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

        var lookup = VariantLookup.getVariantInfo(stats.getVariant());
        if (noCaptain && lookup != null) {
            var commander = lookup.commander;
            if (commander != null && commander.getStats().getSkillLevel(Skills.SUPPORT_DOCTRINE) >= 1) {
                if (supportDoctrineSkillIds.contains(skillId)) {
                    return 999; // Don't apply anything if the skill is the result of support doctrine
                }
            }
        }
        return noCaptain ? 0 : (int) captain.getStats().getSkillLevel(skillId);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        int existingLevel = getSkillLevelForStats(ship.getMutableStats());
        if (existingLevel >= 2) return;
        PersonAPI dummy = Global.getSettings().createPerson();
        dummy.getStats().setSkillLevel(skillId, existingLevel + 1);
        ((CharacterStats) dummy.getStats()).applyPersonalToShip((Ship) ship);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        int existingLevel = getSkillLevelForStats(stats);
        if (existingLevel >= 2) return;
        PersonAPI dummy = Global.getSettings().createPerson();
        dummy.getStats().setSkillLevel(skillId, existingLevel + 1);
        if (applyPersonalToStats == null) {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                //noinspection JavaLangInvokeHandleSignature
                applyPersonalToStats = lookup.findVirtual(
                        CharacterStats.class, "applyPersonalToStats",
                        MethodType.methodType(void.class, stats.getClass(), ShipAPI.HullSize.class));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        if (applyPersonalToStats != null) {
            try {
                applyPersonalToStats.invoke(dummy.getStats(), stats, hullSize);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
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
}
