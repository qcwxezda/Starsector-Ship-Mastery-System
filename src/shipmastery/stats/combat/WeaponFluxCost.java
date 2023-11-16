
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class WeaponFluxCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(Arrays.asList(stats.getBallisticWeaponFluxCostMod(), stats.getEnergyWeaponFluxCostMod(), stats.getMissileWeaponFluxCostMod()));
    }
}
