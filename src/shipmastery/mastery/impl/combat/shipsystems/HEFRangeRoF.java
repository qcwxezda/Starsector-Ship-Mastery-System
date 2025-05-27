package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class HEFRangeRoF extends ShipSystemEffect {
    static final float[] FLUX_PER_SECOND = new float[] {100f, 200f, 300f, 400f};
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.HEFRangeRoF).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.HEFRangeRoFPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Utils.asInt(FLUX_PER_SECOND[Utils.hullSizeToInt(selectedModule.getHullSize())]));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(HEFRangeRoFScript.class)) {
            ship.addListener(new HEFRangeRoFScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "highenergyfocus";
    }

    static class HEFRangeRoFScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float mult;
        final String id;

        HEFRangeRoFScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
            ship.getSystem().setFluxPerSecond(ship.getSystem().getFluxPerSecond() + FLUX_PER_SECOND[Utils.hullSizeToInt(ship.getHullSize())]);
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getEnergyRoFMult().unmodify(id);
            ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(id);
            ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel() * mult;
            ship.getMutableStats().getEnergyRoFMult().modifyPercent(id, effectLevel * 100f);
            ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(id, 1f - effectLevel);
            ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, effectLevel * 100f);
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    "graphics/icons/hullsys/high_energy_focus.png",
                    Strings.Descriptions.HEFRangeRoFTitle,
                    String.format(Strings.Descriptions.HEFRangeRoFDesc1, Utils.asPercentNoDecimal(effectLevel)),
                    false);
        }
    }
}
