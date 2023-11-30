package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AAFRangeDamage extends ShipSystemEffect {
    static final float[] FLUX_PER_SECOND = new float[] {100f, 200f, 300f, 400f};
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.AAFRangeDamage).params(systemName, Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.AAFRangeDamagePost, 0f, Misc.getNegativeHighlightColor(),
                        "" + (int) FLUX_PER_SECOND[Utils.hullSizeToInt(selectedModule.getHullSize())]);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"ammofeed".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(AAFRangeDamageScript.class)) {
            ship.addListener(new AAFRangeDamageScript(ship, getStrength(ship), id));
        }
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
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    id,
                    "graphics/icons/hullsys/ammo_feeder.png",
                    Strings.Descriptions.AAFRangeDamageTitle,
                    String.format(Strings.Descriptions.AAFRangeDamageDesc1, Utils.asPercent(effectLevel)),
                    false);
        }
    }
}
