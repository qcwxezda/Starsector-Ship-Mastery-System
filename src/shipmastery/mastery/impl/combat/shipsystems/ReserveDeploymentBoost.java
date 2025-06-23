package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.ReserveWingStats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReserveDeploymentBoost extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ReserveDeploymentBoost).params(
                Utils.asInt(getCount(selectedVariant)), getSystemName(), Utils.asPercent(getRefitTimeDecrease(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ReserveDeploymentBoostPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ReserveDeploymentBoostScript.class)) {
            ship.addListener(new ReserveDeploymentBoostScript(ship, getCount(ship.getVariant()), getRefitTimeDecrease(ship.getVariant()), id));
        }
    }

    public int getCount(ShipVariantAPI variant) {
        return (int) getStrength(variant);
    }

    public float getRefitTimeDecrease(ShipVariantAPI variant) {
        return getStrength(variant) * 0.25f / 3.5f;
    }

    @Override
    public String getSystemSpecId() {
        return "reservewing";
    }

    static class ReserveDeploymentBoostScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final int count;
        final float refitTimeDecrease;
        final String id;

        ReserveDeploymentBoostScript(ShipAPI ship, int count, float refitTimeDecrease, String id) {
            this.ship = ship;
            this.count = count;
            this.refitTimeDecrease = refitTimeDecrease;
            this.id = id;
        }

        @Override
        public void onActivate() {
            CombatDeferredActionPlugin.performLater(() -> {
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    bay.setExtraDeployments(bay.getExtraDeployments() + count);
                }
                ship.getMutableStats().getFighterRefitTimeMult().modifyMult(id, 1f - refitTimeDecrease);
            }, 1f);

            CombatDeferredActionPlugin.performLater(() -> ship.getMutableStats().getFighterRefitTimeMult().unmodify(id), ReserveWingStats.EXTRA_FIGHTER_DURATION);
        }
    }
}
