package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

public class TPCUpgrade extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init("");
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(TPCUpgradeScript.class)) {
            ship.addListener(new TPCUpgradeScript());
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        super.onFlagshipStatusLost(commander, stats, ship);
    }

    static class TPCUpgradeScript implements AdvanceableListener {

        @Override
        public void advance(float amount) {

        }
    }
}
