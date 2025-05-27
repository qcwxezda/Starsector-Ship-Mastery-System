package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public abstract class ShipSystemDR extends ShipSystemEffect {

    protected abstract String getStatusTitle();
    protected abstract boolean affectsFighters();
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ShipSystemDR).params(
                Global.getSettings().getShipSystemSpec(getSystemSpecId()).getName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        if (selectedModule.getNumFighterBays() > 0 && affectsFighters()) {
            tooltip.addPara(Strings.Descriptions.ShipSystemDRPost, 0f);
        }
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ShipSystemDRScript.class)) {
            ship.addListener(new ShipSystemDRScript(ship, getStrength(ship), getStatusTitle(), id));
        }
    }

    @Override
    public void applyEffectsToFighterIfHasSystem(ShipAPI fighter, ShipAPI ship) {
        for (ShipSystemDRScript script : ship.getListeners(ShipSystemDRScript.class)) {
            fighter.addListener(new ShipSystemDRModifier(fighter, script));
        }
    }

    static class ShipSystemDRScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float maxReduction;
        final String statusTitle;
        float reduction = 0f;

        @Override
        public void advanceWhileOn(float amount) {
            reduction = maxReduction * ship.getSystem().getEffectLevel();
            if (reduction > 0f) {
                Utils.maintainStatusForPlayerShip(ship,
                        id,
                        "graphics/icons/hullsys/burn_drive.png",
                        statusTitle,
                        String.format(Strings.Descriptions.ShipSystemDRDesc1, Utils.asPercentNoDecimal(reduction)),
                        false);
            }
        }

        final String id;
        boolean active = false;

        ShipSystemDRScript(ShipAPI ship, float maxReduction, String statusTitle, String id) {
            this.ship = ship;
            this.maxReduction = maxReduction;
            this.id = id;
            this.statusTitle = statusTitle;
            ship.addListener(new ShipSystemDRModifier(ship, this));
        }

        @Override
        public void onActivate() {
            active = true;
        }

        @Override
        public void onFullyDeactivate() {
            active = false;
        }
    }

    /** Separate out the script and the modifier so that the active field doesn't change based on the ship system status of the fighters */
    static class ShipSystemDRModifier implements AdvanceableListener, DamageTakenModifier {
        final ShipSystemDRScript script;
        final OverlayEmitter emitter;
        final ShipAPI ship;
        final float[] color = new float[] {1f, 0.2f, 0f};
        final IntervalUtil interval;

        ShipSystemDRModifier(final ShipAPI ship, ShipSystemDRScript script) {
            this.ship = ship;
            this.script = script;
            emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1f);
            float time = ship.isFighter() ? 0.05f : 0.15f;
            interval = new IntervalUtil(time, time);
        }


        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f pt, boolean shieldHit) {
            if (!script.active || shieldHit) return null;
            damage.getModifier().modifyMult(script.id, 1f - script.reduction);
            return script.id;
        }

        @Override
        public void advance(float amount) {
            if (ship.getHitpoints() <= 0f) {
                ship.removeListener(this);
            }
            if (script.active) {
                interval.advance(amount);
                if (interval.intervalElapsed()) {
                    emitter.color =
                            new Color(color[0], color[1], color[2], script.ship.getSystem().getEffectLevel() * 0.5f * (ship.isFighter() ? 2f : 1f));
                    Particles.burst(emitter, 1);
                }
            }
        }
    }
}
