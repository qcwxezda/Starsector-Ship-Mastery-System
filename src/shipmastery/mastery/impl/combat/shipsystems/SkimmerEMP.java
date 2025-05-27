package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.TargetChecker;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Collection;

public class SkimmerEMP extends ShipSystemEffect {

    public static final float[] MAX_RANGE = new float[] {400f, 500f, 600f, 700f};
    public static final float[] DAMAGE_MULT = new float[] {0.75f, 1f, 1.5f, 2f};
    public static final float MIN_RANGE_FRAC_VISUAL = 0.25f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        int hullSize = Utils.hullSizeToInt(selectedModule.getHullSize());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.SkimmerEMP)
                                 .params(
                                         getSystemName(),
                                         Utils.asInt(strength),
                                         Utils.asFloatOneDecimal(selectedModule.getMutableStats().getSystemRangeBonus().computeEffective(MAX_RANGE[Utils.hullSizeToInt(selectedModule.getHullSize())])),
                                         Utils.asFloatOneDecimal(strength*50f*DAMAGE_MULT[hullSize]),
                                         Utils.asFloatOneDecimal(strength*125f*DAMAGE_MULT[hullSize]));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SkimmerEMPPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(SkimmerEMPScript.class)) {
            float strength = getStrength(ship);
            int hullSize = Utils.hullSizeToInt(ship.getHullSize());
            ship.addListener(new SkimmerEMPScript(
                    ship,
                    (int) strength,
                    strength*50f*DAMAGE_MULT[hullSize],
                    strength*125f*DAMAGE_MULT[hullSize]));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "displacer";
    }

    static class SkimmerEMPScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final int numArcs;
        final float energyDamage;
        final float empDamage;
        final float minRange, maxRange;

        SkimmerEMPScript(ShipAPI ship, int numArcs, float energyDamage, float empDamage) {
            this.ship = ship;
            this.numArcs = numArcs;
            this.energyDamage = energyDamage;
            this.empDamage = empDamage;
            maxRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_RANGE[Utils.hullSizeToInt(ship.getHullSize())]);
            minRange = MIN_RANGE_FRAC_VISUAL * maxRange;
        }

        @Override
        public void onFullyActivate() {
            Collection<CombatEntityAPI> targets =
                    EngineUtils.getKNearestEntities(
                            numArcs,
                            ship.getLocation(),
                            null,
                            false,
                            maxRange,
                            true,
                            new TargetChecker.CommonChecker(ship));

            for (CombatEntityAPI target : targets) {
                Global.getCombatEngine().spawnEmpArc(
                        ship,
                        ship.getLocation(),
                        null,
                        target,
                        DamageType.ENERGY,
                        energyDamage,
                        empDamage,
                        1000000f,
                        "tachyon_lance_emp_impact",
                        50f,
                        new Color(25,100,155,255),
                        new Color(255,255,255,255)).setSingleFlickerMode();
            }
//            for (int i = 0; i < numArcs - targets.size(); i++) {
//                Vector2f targetPt = MathUtils.randomPointInRing(ship.getLocation(), minRange, maxRange);
//                Global.getCombatEngine().spawnEmpArcVisual(
//                        ship.getLocation(),
//                        null,
//                        targetPt,
//                        null,
//                        30f,
//                        new Color(25,100,155,255),
//                        new Color(255,255,255,255)).setSingleFlickerMode();
//            }
        }
    }
}
