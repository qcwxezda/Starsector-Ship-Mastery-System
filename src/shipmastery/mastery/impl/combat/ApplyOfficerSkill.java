package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class ApplyOfficerSkill extends BaseMasteryEffect {

    String skillId;
    private static MethodHandle applyPersonalToStats;
    private static final Set<String> validSkillIds = new HashSet<>();
    static {
        validSkillIds.add(Skills.HELMSMANSHIP);
        validSkillIds.add(Skills.COMBAT_ENDURANCE);
        validSkillIds.add(Skills.IMPACT_MITIGATION);
        validSkillIds.add(Skills.DAMAGE_CONTROL);
        validSkillIds.add(Skills.FIELD_MODULATION);
        validSkillIds.add(Skills.POINT_DEFENSE);
        validSkillIds.add(Skills.TARGET_ANALYSIS);
        validSkillIds.add(Skills.BALLISTIC_MASTERY);
        validSkillIds.add(Skills.SYSTEMS_EXPERTISE);
        validSkillIds.add(Skills.MISSILE_SPECIALIZATION);
        validSkillIds.add(Skills.GUNNERY_IMPLANTS);
        validSkillIds.add(Skills.ENERGY_WEAPON_MASTERY);
        validSkillIds.add(Skills.ORDNANCE_EXPERTISE);
        validSkillIds.add(Skills.POLARIZED_ARMOR);
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
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        PersonAPI dummy = Global.getSettings().createPerson();
        dummy.getStats().setSkillLevel(skillId, 1);
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
        int energy = wsc.se + 2*wsc.me + 4*wsc.le;
        int ballistic = wsc.sb + 2*wsc.mb + 4*wsc.lb;
        int missile = wsc.sm + 2*wsc.mm + 4*wsc.lm;
        for (String id : skillIds) {
            SkillSpecAPI skill = Global.getSettings().getSkillSpec(id);
            if (skill.hasTag("deprecated")) continue;
            if (validSkillIds.contains(id) && !seenIds.contains(id) && maxTier >= getSkillTier(id)) {
                boolean valid = true;
                switch (id) {
                    case Skills.FIELD_MODULATION:
                        if (!Utils.hasShield(spec)) valid = false; break;
                    case Skills.BALLISTIC_MASTERY:
                        if (ballistic <= 3) valid = false; break;
                    case Skills.SYSTEMS_EXPERTISE:
                        if (spec.getShipSystemId() == null) valid = false; break;
                    case Skills.MISSILE_SPECIALIZATION:
                        if (missile <= 3) valid = false; break;
                    case Skills.ENERGY_WEAPON_MASTERY:
                        if (energy <= 3) valid = false; break;
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
        return 3f;
    }
}
