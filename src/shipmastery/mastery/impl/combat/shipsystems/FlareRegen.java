package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class FlareRegen extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.FlareRegen)
                .params(systemName, Utils.asFloatOneDecimal(1f / getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.FlareRegenPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"flarelauncher".equals(ship.getSystem().getId())) return;

        if (!ship.hasListenerOfClass(FlareRegenScript.class)) {
            ship.addListener(new FlareRegenScript(ship, getStrength(ship)));
        }
    }

    static class FlareRegenScript implements AdvanceableListener {
        final ShipAPI ship;
        final float chargesPerSecond;
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle getAmmoTracker;
        AmmoTrackerAPI ammoTracker;
        final IntervalUtil setRegenInterval = new IntervalUtil(1f, 1f);

        FlareRegenScript(ShipAPI ship, final float chargesPerSecond) {
            this.ship = ship;
            this.chargesPerSecond = chargesPerSecond;
            try {
                getAmmoTracker = lookup.unreflect(ship.getSystem().getClass().getMethod("getAmmoTracker"));
                ammoTracker = (AmmoTrackerAPI) getAmmoTracker.invoke(ship.getSystem());
            }
            catch (Throwable t) {
                ammoTracker = null;
            }
        }

        @Override
        public void advance(float amount) {
            // Need to do this periodically -- gets set back to 0 for example if command is transferred from or to
            if (ammoTracker != null) {
                setRegenInterval.advance(amount);
                if (ship.isAlive() && setRegenInterval.intervalElapsed()) {
                    ammoTracker.setAmmoPerSecond(ship.getMutableStats().getSystemRegenBonus().computeEffective(chargesPerSecond));
                }
            }
        }
    }
}
