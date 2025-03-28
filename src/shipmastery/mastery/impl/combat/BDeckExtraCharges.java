package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.hullmods.BDeck;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BDeckExtraCharges extends BaseMasteryEffect {

    public static final float DELAY_SECONDS = 60f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BDeckExtraCharges)
                                 .params(Global.getSettings().getHullModSpec("bdeck").getDisplayName()
                                         , getExtraCharges(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BDeckExtraChargesPost, 0f, Misc.getTextColor(), Utils.asFloatOneDecimal(DELAY_SECONDS));
    }

    public int getExtraCharges(float strength) {
        return (int) Math.max(1f, strength);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.hasListenerOfClass(BDeck.BDeckListener.class) && !ship.hasListenerOfClass(EnhancedBDeckListener.class)) {
            ship.removeListenerOfClass(BDeck.BDeckListener.class);
            ship.addListener(new EnhancedBDeckListener(ship, getExtraCharges(getStrength(ship))));
        }
    }

    public static class EnhancedBDeckListener extends BDeck.BDeckListener {
        protected int extraCharges;
        public EnhancedBDeckListener(ShipAPI ship, int extraCharges) {
            super(ship);
            this.extraCharges = extraCharges;
        }
        @Override
        public void advance(float amount) {
            super.advance(amount);

            if (fired && extraCharges > 0) {
                CombatDeferredActionPlugin.performLater(() -> {
                    fired = false;
                    extraCharges--;
                }, DELAY_SECONDS);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isBuiltInMod("bdeck")) return null;
        return 3f;
    }
}
