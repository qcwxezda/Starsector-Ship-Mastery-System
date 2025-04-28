package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Set;

public class DamagePerShipDestroyed extends BaseMasteryEffect {

    public static final float AMOUNT_FOR_EQUAL_DP = 0.05f;
    public static final float BASE_CAP = 0.1f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.DamagePerShipDestroyed)
                .params(Utils.asPercent(AMOUNT_FOR_EQUAL_DP), Utils.asPercent(AMOUNT_FOR_EQUAL_DP))
                .colors(Misc.getTextColor(), Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.DamagePerShipDestroyedPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercentNoDecimal(BASE_CAP * getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new DamagePerShipDestroyedListener(ship, BASE_CAP * getStrength(ship)));
    }

    private class DamagePerShipDestroyedListener implements AdvanceableListener, ShipDestroyedListener {
        private final ShipAPI ship;
        private final float cap;
        private float cur = 0f;

        private DamagePerShipDestroyedListener(ShipAPI ship, float cap) {
            this.ship = ship;
            this.cap = cap;
        }

        @Override
        public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
            if (target.isFighter() || target.getOwner() == ship.getOwner()) return;
            boolean countAsKill = false;
            for (ShipAPI source : recentlyDamagedBy) {
                if (EngineUtils.shipIsOwnedBy(source, ship)) {
                    countAsKill = true;
                    break;
                }
            }
            if (!countAsKill) return;

            var shipStats = ship.getMutableStats();
            float dp = shipStats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(shipStats.getSuppliesToRecover().getBaseValue());
            var targetStats = target.getMutableStats();
            float targetDp = targetStats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(targetStats.getSuppliesToRecover().getBaseValue());
            float ratio = targetDp / dp;

            float increase = AMOUNT_FOR_EQUAL_DP * Math.min(1f, ratio);
            cur += increase;
            cur = Math.min(cap, cur);
            shipStats.getDamageToFighters().modifyPercent(id, 100f*cur);
            shipStats.getDamageToFrigates().modifyPercent(id, 100f*cur);
            shipStats.getDamageToDestroyers().modifyPercent(id, 100f*cur);
            shipStats.getDamageToCruisers().modifyPercent(id, 100f*cur);
            shipStats.getDamageToCapital().modifyPercent(id, 100f*cur);
        }

        @Override
        public void advance(float amount) {
            if (cur > 0f) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/high_energy_focus.png",
                        Strings.Descriptions.DamagePerShipDestroyedTitle,
                        String.format(Strings.Descriptions.DamagePerShipDestroyedDesc1, Utils.asPercentOneDecimal(cur)),
                        false);
            }
        }
    }
}
