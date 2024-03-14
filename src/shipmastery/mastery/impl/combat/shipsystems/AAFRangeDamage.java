package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AAFRangeDamage extends ShipSystemEffect {
    static final float[] FLUX_PER_SECOND = new float[] {50f, 100f, 150f, 200f};
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.AAFRangeDamage).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.AAFRangeDamagePost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Utils.asInt(FLUX_PER_SECOND[Utils.hullSizeToInt(selectedModule.getHullSize())]));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(AAFRangeDamageScript.class)) {
            ship.addListener(new AAFRangeDamageScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "ammofeed";
    }

    static class AAFRangeDamageScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float mult;
        final String id;

        AAFRangeDamageScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
            ship.getSystem().setFluxPerSecond(ship.getSystem().getFluxPerSecond() + FLUX_PER_SECOND[Utils.hullSizeToInt(ship.getHullSize())]);
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(id);
            ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel() * mult;
            ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, effectLevel * 100f);
            ship.getMutableStats().getBallisticWeaponDamageMult().modifyPercent(id, effectLevel * 100f);
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    "graphics/icons/hullsys/ammo_feeder.png",
                    Strings.Descriptions.AAFRangeDamageTitle,
                    String.format(Strings.Descriptions.AAFRangeDamageDesc1, Utils.asPercentNoDecimal(effectLevel)),
                    false);
        }
    }
}
