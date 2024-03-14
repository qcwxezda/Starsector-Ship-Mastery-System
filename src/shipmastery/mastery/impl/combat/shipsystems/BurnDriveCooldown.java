package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BurnDriveCooldown extends ShipSystemEffect{
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BurnDriveCooldown).params(getSystemName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BurnDriveCooldownPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Utils.asFloatOneDecimal(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(BurnDriveCooldownScript.class)) {
            ship.addListener(new BurnDriveCooldownScript(ship, getStrength(ship)));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "burndrive";
    }

    static class BurnDriveCooldownScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final float maxMult;
        float activeDur = 0f;
        float cooldownRateMult = 1f;

        BurnDriveCooldownScript(ShipAPI ship, float maxMult) {
            this.ship = ship;
            this.maxMult = maxMult;
        }

        @Override
        public void onActivate() {
            activeDur = 0f;
        }

        @Override
        public void advanceWhileOn(float amount) {
            activeDur += amount;
        }

        @Override
        public void onDeactivate() {
            ShipSystemAPI system = ship.getSystem();
            float ratio = activeDur / (system.getChargeUpDur() + system.getChargeActiveDur() + system.getChargeDownDur());
            ratio = Math.max(1f / maxMult, Math.min(1f, ratio));

            cooldownRateMult = 1f / ratio;
        }

        @Override
        public void advance(float amount) {
            ShipSystemAPI system = ship.getSystem();
            if (system.isCoolingDown()) {
                float cooldownAmount = amount * cooldownRateMult;
                cooldownAmount -= amount; // to counteract the natural cooldown reduction
                system.setCooldownRemaining(system.getCooldownRemaining() - cooldownAmount);
            }
        }
    }
}
