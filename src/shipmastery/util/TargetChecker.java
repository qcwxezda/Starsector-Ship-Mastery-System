package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public interface TargetChecker {
    boolean check(CombatEntityAPI entity);

    class CommonChecker implements TargetChecker {
        public int side;
        public CommonChecker(CombatEntityAPI owner) {
            side = owner.getOwner();
        }

        public void setSide(int side) {
            this.side = side;
        }

        @Override
        public boolean check(CombatEntityAPI entity) {
            if (entity instanceof ShipAPI && ((ShipAPI) entity).isPhased()) return false;
            return entity != null &&
                    Global.getCombatEngine().isEntityInPlay(entity)
                    && entity.getHitpoints() > 0
                    && entity.getOwner() != side
                    && entity.getOwner() != 100;
        }
    }
}
