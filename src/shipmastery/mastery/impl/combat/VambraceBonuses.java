package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class VambraceBonuses extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.VambraceBonuses)
                .params(Utils.asPercent(strength), Utils.asFloatOneDecimal(1000f*strength), Utils.asFloatOneDecimal(100f*strength), Utils.asPercent(5f*strength));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.VambraceBonusesPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        float strength = getStrength(ship);
        ship.addListener(new VambraceBonusesScript(ship, id, strength, 1000f*strength, 100f*strength, 5f*strength));
    }

    static class VambraceBonusesScript implements AdvanceableListener {
        final ShipAPI ship;
        final String id;
        final float damageReductionMultPer;
        final float effectiveArmorFlatPer;
        final float maxSpeedFlatPer;
        final float maneuverabilityMultPer;
        final IntervalUtil checkerInterval = new IntervalUtil(0.6f, 1.2f);
        final int maxVambraces;

        VambraceBonusesScript(ShipAPI ship, String id, float dr, float armor, float speed, float maneuverability) {
            this.ship = ship;
            this.id = id;
            this.damageReductionMultPer = dr;
            this.effectiveArmorFlatPer = armor;
            this.maxSpeedFlatPer = speed;
            this.maneuverabilityMultPer = maneuverability;

            int maxVambs = 0;
            ShipVariantAPI variant = ship.getVariant();
            for (String slotId : variant.getModuleSlots()) {
                ShipVariantAPI moduleVariant = variant.getModuleVariant(slotId);
                if ("module_onslaught_armor_left".equals(moduleVariant.getHullSpec().getHullId())) {
                    maxVambs++;
                }
                if ("module_onslaught_armor_right".equals(moduleVariant.getHullSpec().getHullId())) {
                    maxVambs++;
                }
            }
            maxVambraces = maxVambs;
        }

        @Override
        public void advance(float amount) {
            checkerInterval.advance(amount);
            if (checkerInterval.intervalElapsed()) {
                int numVambs = 0;
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (!module.isAlive()) continue;
                    if ("module_onslaught_armor_left".equals(module.getHullSpec().getHullId())) {
                        numVambs++;
                    }
                    if ("module_onslaught_armor_right".equals(module.getHullSpec().getHullId())) {
                        numVambs++;
                    }
                }
                int numMissing = maxVambraces - numVambs;
                if (numVambs == 0) {
                    ship.getMutableStats().getEffectiveArmorBonus().unmodify(id);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                } else {
                    ship.getMutableStats().getEffectiveArmorBonus().modifyFlat(id, effectiveArmorFlatPer*numVambs);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - numVambs*damageReductionMultPer);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - numVambs*damageReductionMultPer);
                }
                if (numMissing == 0) {
                    ship.getMutableStats().getMaxSpeed().unmodify(id);
                    ship.getMutableStats().getMaxTurnRate().unmodify(id);
                    ship.getMutableStats().getAcceleration().unmodify(id);
                    ship.getMutableStats().getDeceleration().unmodify(id);
                    ship.getMutableStats().getTurnAcceleration().unmodify(id);
                } else {
                    ship.getMutableStats().getMaxSpeed().modifyFlat(id, numMissing*maxSpeedFlatPer);
                    ship.getMutableStats().getMaxTurnRate().modifyPercent(id, 100f*numMissing*maneuverabilityMultPer);
                    ship.getMutableStats().getAcceleration().modifyPercent(id, 100f*numMissing*maneuverabilityMultPer);
                    ship.getMutableStats().getDeceleration().modifyPercent(id, 100f*numMissing*maneuverabilityMultPer);
                    ship.getMutableStats().getTurnAcceleration().modifyPercent(id, 100f*numMissing*maneuverabilityMultPer);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if ("onslaught_mk1".equals(spec.getHullId())) {
            return 2f;
        }
        return null;
    }
}
