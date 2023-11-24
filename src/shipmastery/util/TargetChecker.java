package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;

public interface TargetChecker {
    boolean check(CombatEntityAPI entity);

    class CommonChecker implements TargetChecker {
        public int side;

        public CommonChecker() {
            side = 100;
        }
        public CommonChecker(CombatEntityAPI owner) {
            side = owner.getOwner();
        }

        public void setSide(int side) {
            this.side = side;
        }

        @Override
        public boolean check(CombatEntityAPI entity) {
            return entity != null && Global.getCombatEngine().isEntityInPlay(entity) && entity.getHitpoints() > 0 && entity.getOwner() != side && entity.getOwner() != 100;
        }
    }
}
