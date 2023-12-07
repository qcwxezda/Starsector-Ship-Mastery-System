package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Pair;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.*;

public class LimitedArmorRegen extends BaseMasteryEffect {

    public static final float REGEN_RATE = 3f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.LimitedArmorRegen).params(Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.LimitedArmorRegenPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(LimitedArmorRegenScript.class)) {
            ship.addListener(new LimitedArmorRegenScript(ship, getStrength(ship)));
        }
    }

    static class LimitedArmorRegenScript implements AdvanceableListener {
        final ShipAPI ship;
        final float regenFrac;
        final float regenAmount;
        final IntervalUtil armorCheckInterval = new IntervalUtil(0.5f, 0.5f);
        final boolean[][] activated;
        final Map<Pair<Integer, Integer>, FloatRef> activationLevels = new HashMap<>();

        private static class FloatRef {
            float f;
            FloatRef(float f) {
                this.f = f;
            }
        }

        LimitedArmorRegenScript(ShipAPI ship, float frac) {
            this.ship = ship;
            regenFrac = frac;
            regenAmount = regenFrac * ship.getArmorGrid().getArmorRating() / 15f;
            float[][] grid = ship.getArmorGrid().getGrid();
            activated = new boolean[grid.length][grid[0].length];
        }

        @Override
        public void advance(float amount) {
            armorCheckInterval.advance(amount);
            if (armorCheckInterval.intervalElapsed()) {
                float[][] grid = ship.getArmorGrid().getGrid();
                for (int i = 0; i < grid.length; i++) {
                    for (int j = 0; j < grid[0].length; j++) {
                        if (!activated[i][j] && grid[i][j] <= 0f) {
                            activated[i][j] = true;
                            activationLevels.put(new Pair<>(i, j), new FloatRef(0f));
                        }
                    }
                }
            }

            for (Iterator<Map.Entry<Pair<Integer, Integer>, FloatRef>> iterator = activationLevels.entrySet().iterator();
                 iterator.hasNext(); ) {
                Map.Entry<Pair<Integer, Integer>, FloatRef> entry = iterator.next();
                Pair<Integer, Integer> index = entry.getKey();
                FloatRef ref = entry.getValue();
                float level = ref.f;
                if (level < 1f) {
                    level += amount * REGEN_RATE;
                    ref.f = level;
                    ship.getArmorGrid().getGrid()[index.one][index.two] += amount * REGEN_RATE * regenAmount;
                } else {
                    iterator.remove();
                }
            }
        }
    }
}
