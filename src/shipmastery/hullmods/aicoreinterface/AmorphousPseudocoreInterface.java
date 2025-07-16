package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.config.Settings;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class AmorphousPseudocoreInterface extends AICoreInterfaceHullmod {

    public static final float IMMUNITY_SECONDS = 10f;
    public static final float IMMUNITY_FADE_TIME = 10f;
    public static final float CR_INCREASE = 1f;

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.amorphousCoreIntegrationEffect,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(CR_INCREASE),
                Utils.asFloatOneDecimal(IMMUNITY_SECONDS),
                Utils.asFloatOneDecimal(IMMUNITY_FADE_TIME));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxCombatReadiness().modifyFlat(id, CR_INCREASE, Strings.Items.integratedDesc);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new IntegrationScript(ship, id));
    }


    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return 5000000f;
    }

    private static class IntegrationScript implements AdvanceableListener {
        private final ShipAPI ship;
        private final String id;
        float timeElapsed;
        final IntervalUtil checkerInterval = new IntervalUtil(0.1f, 0.1f);
        final EntityBurstEmitter emitter;

        public IntegrationScript(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
            timeElapsed = 0f;
            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), new Color(200, 50, 255), 11, 25f, 0.5f);
            emitter.alphaMult = 0.2f;
            emitter.widthGrowth = -25f;
            emitter.fadeInFrac = 0.5f;
            emitter.fadeOutFrac = 0.5f;
            emitter.layer = CombatEngineLayers.ABOVE_SHIPS_LAYER;
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive() || ship.getHitpoints() < 0f || timeElapsed > IMMUNITY_FADE_TIME + IMMUNITY_SECONDS) {
                ship.removeListener(this);
                return;
            }
            var stats = ship.getMutableStats();
            checkerInterval.advance(amount);
            if (!checkerInterval.intervalElapsed()) return;
            if (ship.getFluxTracker().isVenting()) {
                float effectMult = timeElapsed <= IMMUNITY_SECONDS ? 1f : Math.max(0f, 1f - ((timeElapsed - IMMUNITY_SECONDS) / IMMUNITY_FADE_TIME));
                stats.getHullDamageTakenMult().modifyMult(id, 1f - effectMult);
                stats.getArmorDamageTakenMult().modifyMult(id, 1f - effectMult);
                stats.getShieldDamageTakenMult().modifyMult(id, 1f - effectMult);
                stats.getEmpDamageTakenMult().modifyMult(id, 1f - effectMult);
                emitter.alphaMult = effectMult*0.2f;
                emitter.burst(11);
                timeElapsed += checkerInterval.getIntervalDuration();
            } else {
                stats.getHullDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().unmodify(id);
                stats.getShieldDamageTakenMult().unmodify(id);
                stats.getEmpDamageTakenMult().unmodify(id);
            }
        }
    }
}
