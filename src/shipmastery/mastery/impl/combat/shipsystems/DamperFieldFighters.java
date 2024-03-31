package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class DamperFieldFighters extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.DamperFieldFighters)
                .params(getSystemName(), Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(DamperFieldFightersScript.class)) {
            // Also add to carrier just for the invulnerability effect
            ship.addListener(new DamperFieldFightersScript(null, ship, getStrength(ship), id));
        }
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!fighter.hasListenerOfClass(DamperFieldFightersScript.class)) {
            fighter.addListener(new DamperFieldFightersScript(fighter, ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "damper";
    }

    static class DamperFieldFightersScript implements AdvanceableListener, DamageTakenModifier, DamageListener {
        final @Nullable ShipAPI fighter;
        final ShipAPI carrier;
        final String id;
        boolean active = false;
        // Use jitter for consistency with carrier's damper field, could change to particle effect if slow
        static final Color jitterUnderColor = new Color(255, 165, 90, 155);
        static final Color jitterColor = new Color(255, 165, 90, 55);
        final float minHPFrac;
        float tempHPRevert = -1f;
        final float tempHPAmount = 1000000f;

        DamperFieldFightersScript(@Nullable ShipAPI fighter, ShipAPI carrier, float minHPFrac, String id) {
            this.fighter = fighter;
            this.carrier = carrier;
            this.minHPFrac = minHPFrac;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            active = carrier.getSystem().isActive();
            if (fighter == null || !fighter.isAlive()) return;
            MutableShipStatsAPI fighterStats = fighter.getMutableStats();
            MutableShipStatsAPI carrierStats = carrier.getMutableStats();
            if (active) {
                fighterStats.getHullDamageTakenMult().modifyMult(id, carrierStats.getHullDamageTakenMult().getMult());
                fighterStats.getArmorDamageTakenMult().modifyMult(id, carrierStats.getArmorDamageTakenMult().getMult());
                fighterStats.getEmpDamageTakenMult().modifyMult(id, carrierStats.getEmpDamageTakenMult().getMult());
                fighter.setJitterUnder(fighter, jitterUnderColor, 1f, 25, 7f);
                fighter.setJitter(fighter, jitterColor, 1f, 2, 5f);
            }
            else {
                fighterStats.getHullDamageTakenMult().unmodify(id);
                fighterStats.getArmorDamageTakenMult().unmodify(id);
                fighterStats.getEmpDamageTakenMult().unmodify(id);
            }
        }

        // This strategy looks cursed, but because applyDamageInner is sandwiched between modifyDamageTaken and
        // reportDamageApplied with nothing else in between, it's actually fairly foolproof
        @Override
        public String modifyDamageTaken(Object param, final CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            if (carrier.getSystem().isActive()) {
                // This isn't strictly needed and is only used to ensure the damage floaties are as consistent as possible
                if (target.getHitpoints() <= target.getMaxHitpoints()*minHPFrac) {
                    damage.getModifier().modifyMult(id, 0f);
                    return id;
                }
                tempHPRevert = target.getHitpoints();
                target.setHitpoints(tempHPAmount);
            }
            return null;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (tempHPRevert > 0f) {
                float damageTaken = tempHPAmount - target.getHitpoints();
                target.setHitpoints(Math.max(Math.min(tempHPRevert, target.getMaxHitpoints()*minHPFrac), tempHPRevert-damageTaken));
                tempHPRevert = -1f;
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Float mult = super.getSelectionWeight(spec);
        if (mult == null) return null;
        int n = spec.getFighterBays();
        if (n <= 0) return null;
        return mult * Utils.getSelectionWeightScaledByValue(n, 1f, false);
    }
}
