package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class PlasmaBurnEnergyRoF extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.PlasmaBurnEnergyRoF)
                .params(getSystemName(),Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.PlasmaBurnEnergyRoFPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asFloatOneDecimal(getStrength(selectedModule) * 20f),
                getSystemName());
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship == null || ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(PlasmaBurnEnergyRoFScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new PlasmaBurnEnergyRoFScript(ship, strength, 20f * strength, id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "microburn";
    }

    static class PlasmaBurnEnergyRoFScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final float increase, increaseTime;
        final String id;
        float postEffectLevel = 0f;
        static final Color color = new Color(89, 255, 255);
        final EntityBurstEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.2f, 0.2f);

        PlasmaBurnEnergyRoFScript(ShipAPI ship, float increase, float increaseTime, String id) {
            this.ship = ship;
            this.increase = increase;
            this.increaseTime = increaseTime;
            this.id = id;
            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), color, 6, 0f, 0.75f);
            emitter.alphaMult = 0.2f;
            emitter.widthGrowth = 3f;
            emitter.jitterRadius = 10f;
            emitter.fadeInFrac = 0.25f;
            emitter.fadeOutFrac = 0.75f;
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void onDeactivate() {
            postEffectLevel = 1f;
        }

        @Override
        public void advanceWhileOn(float amount) {
            ship.getMutableStats().getEnergyRoFMult().modifyPercent(id, 100f * increase);
            ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(id, 1f - increase * postEffectLevel);
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id,
          "graphics/icons/hullsys/high_energy_focus.png",
                    Strings.Descriptions.PlasmaBurnEnergyRoFTitle,
                    String.format(Strings.Descriptions.PlasmaBurnEnergyRoFDesc1, Utils.asPercentNoDecimal(increase)),
                    false);
        }

        @Override
        public void advance(float amount) {
            if (postEffectLevel > 0f && !ship.getSystem().isActive()) {
                ship.getMutableStats().getEnergyRoFMult().modifyPercent(id, 100f * increase * postEffectLevel);
                ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(id, 1f - increase * postEffectLevel);
                postEffectLevel -= amount / increaseTime;
                if (postEffectLevel <= 0f) {
                    ship.getMutableStats().getEnergyRoFMult().unmodify(id);
                    ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(id);
                }
            }
            if (postEffectLevel > 0f) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/high_energy_focus.png",
                        Strings.Descriptions.PlasmaBurnEnergyRoFTitle,
                        String.format(Strings.Descriptions.PlasmaBurnEnergyRoFDesc1, Utils.asPercentNoDecimal(increase * postEffectLevel)),
                        false);
            }

            float effectAlpha = postEffectLevel;
            if (ship.getSystem().isActive()) {
                effectAlpha = 1f;
            }
            if (effectAlpha > 0f) {
                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    emitter.alphaMult = effectAlpha;
                    Particles.burst(emitter, 6);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Float mult = super.getSelectionWeight(spec);
        if (mult == null) return null;
        // Must have at least one energy weapon
        Utils.WeaponSlotCount count = Utils.countWeaponSlots(spec);
        float weight = count.computeWeaponWeight(WeaponAPI.WeaponType.ENERGY, 0.2f, 0.3f);
        return weight == 0 ? null : mult * Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 0.4f, 1f);
    }
}
