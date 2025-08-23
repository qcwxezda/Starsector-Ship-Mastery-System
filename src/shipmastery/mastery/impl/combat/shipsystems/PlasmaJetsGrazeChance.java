package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PlasmaJetsGrazeChance extends ShipSystemEffect {

    public static final float DAMAGE_MULT = 0.5f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.PlasmaJetsGrazeChance)
                .params(getSystemName(), Utils.asPercent(DAMAGE_MULT))
                .colors(Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.PlasmaJetsGrazeChancePost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(PlasmaJetsGrazeChanceScript.class)) {
            ship.addListener(new PlasmaJetsGrazeChanceScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "plasmajets";
    }

    static class PlasmaJetsGrazeChanceScript extends BaseShipSystemListener implements DamageTakenModifier {

        final ShipAPI ship;
        final float maxChance;
        final String id;
        boolean active = false;
        final OverlayEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.15f, 0.15f);

        PlasmaJetsGrazeChanceScript(ShipAPI ship, float maxChance, String id) {
            this.ship = ship;
            this.maxChance = maxChance;
            this.id = id;
            emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1f);
            emitter.blendDestFac = GL11.GL_ONE_MINUS_SRC_ALPHA;
            emitter.layer = CombatEngineLayers.BELOW_SHIPS_LAYER;
            emitter.fadeInFrac = 0.05f;
            emitter.fadeOutFrac = 0.75f;
        }

        @Override
        public void onFullyDeactivate() {
            active = false;
        }

        @Override
        public void advanceWhileOn(float amount) {
            active = true;
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id,
                    "graphics/icons/hullsys/displacer.png",
                    Strings.Descriptions.PlasmaJetsGrazeChanceTitle,
                    String.format(
                            Strings.Descriptions.PlasmaJetsGrazeChanceDesc1,
                            Utils.asPercentNoDecimal(getGrazeChance()),
                            Utils.asPercent(1f-DAMAGE_MULT)),
                    false);

            burstInterval.advance(amount);
            if (burstInterval.intervalElapsed()) {
                emitter.alphaMult = 0.3f * ship.getSystem().getEffectLevel();
                Particles.burst(emitter, 1);
            }
        }

        float getGrazeChance() {
            return maxChance*Math.min(1f, ship.getSystem().getEffectLevel()*ship.getVelocity().length()/ship.getMaxSpeed());
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI) && !(param instanceof BeamAPI)) return null;
            if (param instanceof MissileAPI) return null;
            if (!active) return null;

            if (Math.random() < getGrazeChance()) {
                damage.getModifier().modifyMult(id, DAMAGE_MULT);
                return id;
            }
            return null;
        }
    }
}
