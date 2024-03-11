package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.EntropyAmplifierStats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.TargetChecker;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class EntropyAmplifierChaining extends ShipSystemEffect {

    public static final float MAX_RANGE = 1200f;
    public static final String ENTROPY_AMPLIFIER_ID = "entropyamplifier";

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EntropyAmplifierChaining)
                                 .params(systemName,
                                         Utils.asInt(getStrength(selectedModule)),
                                         Utils.asFloatOneDecimal(selectedModule.getMutableStats().getSystemRangeBonus().computeEffective(MAX_RANGE)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EntropyAmplifierChainingPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !ENTROPY_AMPLIFIER_ID.equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(EntropyAmplifierChainingScript.class)) {
            ship.addListener(new EntropyAmplifierChainingScript(ship, (int) getStrength(ship), id));
        }
    }

    static class EntropyAmplifierChainingScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final int count;
        final String id;
        final List<ShipAPI> additionalTargets = new ArrayList<>();
        EntropyAmplifierStats.TargetData curTargetData;

        EntropyAmplifierChainingScript(ShipAPI ship, int count, String id) {
            this.ship = ship;
            this.count = count;
            this.id = id;
        }

        @Override
        public void onFullyActivate() {
            Object targetDataObj = Global.getCombatEngine().getCustomData().get(ship.getId() + "_entropy_target_data");
            if (targetDataObj == null) return;

            curTargetData = ((EntropyAmplifierStats.TargetData) targetDataObj);
            if (curTargetData.target == null) return;

            additionalTargets.clear();
            Collection<CombatEntityAPI> additional =
                    EngineUtils.getKNearestEntities(
                            count,
                            curTargetData.target.getLocation(),
                            ShipAPI.HullSize.FRIGATE,
                            false,
                            ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_RANGE),
                            true,
                            new TargetChecker.CommonChecker(ship) {
                                @Override
                                public boolean check(CombatEntityAPI entity) {
                                    return super.check(entity) && entity != curTargetData.target;
                                }
                            });

            for (CombatEntityAPI entity : additional) {
                if (entity instanceof ShipAPI) {
                    additionalTargets.add((ShipAPI) entity);
                }
            }
            for (ShipAPI target : additionalTargets) {
                Global.getCombatEngine().spawnEmpArcVisual(
                        curTargetData.target.getLocation(),
                        curTargetData.target,
                        target.getLocation(),
                        target,
                        80f,
                        EntropyAmplifierStats.JITTER_COLOR,
                        Color.WHITE);
            }
        }
        @Override
        public void advance(float amount) {
            if (curTargetData == null) return;

            if (curTargetData.currDamMult <= 1f) {
                curTargetData = null;
                for (ShipAPI target : additionalTargets) {
                    target.getMutableStats().getHullDamageTakenMult().unmodify(ENTROPY_AMPLIFIER_ID);
                    target.getMutableStats().getArmorDamageTakenMult().unmodify(ENTROPY_AMPLIFIER_ID);
                    target.getMutableStats().getShieldDamageTakenMult().unmodify(ENTROPY_AMPLIFIER_ID);
                    target.getMutableStats().getEmpDamageTakenMult().unmodify(ENTROPY_AMPLIFIER_ID);
                }
                additionalTargets.clear();
                return;
            }

            float effectLevel = ship.getSystem().getEffectLevel();
            for (ShipAPI target : additionalTargets) {
                Utils.maintainStatusForPlayerShip(target,
                                                  ENTROPY_AMPLIFIER_ID,
                                                  ship.getSystem().getSpecAPI().getIconSpriteName(),
                                                  ship.getSystem().getDisplayName(),
                                                  String.format(Strings.Descriptions.EntropyAmplifierChainingDesc1, (int)((curTargetData.currDamMult - 1f) * 100f)),
                                                  true);

                // Use ENTROPY_AMPLIFIER_ID so that the effect doesn't stack
                target.getMutableStats().getHullDamageTakenMult().modifyMult(ENTROPY_AMPLIFIER_ID, curTargetData.currDamMult);
                target.getMutableStats().getArmorDamageTakenMult().modifyMult(ENTROPY_AMPLIFIER_ID, curTargetData.currDamMult);
                target.getMutableStats().getShieldDamageTakenMult().modifyMult(ENTROPY_AMPLIFIER_ID, curTargetData.currDamMult);
                target.getMutableStats().getEmpDamageTakenMult().modifyMult(ENTROPY_AMPLIFIER_ID, curTargetData.currDamMult);
                if (effectLevel > 0) {
                    target.setJitter(
                            ENTROPY_AMPLIFIER_ID, EntropyAmplifierStats.JITTER_COLOR, effectLevel, 3, 0f, 5f);
                }
            }
        }
    }
}
