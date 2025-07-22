package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import particleengine.Particles;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Set;

public class SystemRegenOnKill extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.SystemRegenOnKill)
                                 .params(Misc.getHullSizeStr(selectedVariant.getHullSize()))
                                 .colors(Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asFloatOneDecimal(getStrength(selectedVariant)));
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost2, 0f);
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost3, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null) {
            return;
        }
        if (!ship.hasListenerOfClass(SystemRegenOnKillScript.class)) {
            ship.addListener(new SystemRegenOnKillScript(ship, getStrength(ship)));
        }
    }

    static class SystemRegenOnKillScript implements ShipDestroyedListener {
        final ShipAPI ship;
        final boolean usesAmmo;
        final float strength;

        SystemRegenOnKillScript(ShipAPI ship, float strength) {
            this.ship = ship;
            // Yes, this is the only way to check if the ship system uses ammo without reflection...
            usesAmmo = ship.getSystem().getMaxAmmo() < Integer.MAX_VALUE;
            this.strength = strength;
        }

        @Override
        public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
            boolean countAsKill = false;
            for (ShipAPI source : recentlyDamagedBy) {
                if (EngineUtils.shipIsOwnedBy(source, ship)) {
                    countAsKill = true;
                    break;
                }
            }

            if (countAsKill) {
                if (target.isFighter() || Utils.hullSizeToInt(target.getHullSize()) < Utils.hullSizeToInt(ship.getHullSize())) return;
                ShipSystemAPI system = ship.getSystem();
                boolean activated = false;
                if (usesAmmo) {
                    system.setAmmoReloadProgress(system.getAmmoReloadProgress() + system.getAmmoPerSecond() * strength);
                    activated = true;
                } else {
                    if (!system.isActive()) {
                        system.setCooldownRemaining(Math.max(0f, system.getCooldownRemaining() - strength));
                        activated = true;
                    }
                }
                if (activated) {
                    Color color = ship.getSpriteAPI().getAverageColor();
                    Color newColor = new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.6f);
                    OverlayEmitter emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1f);
                    emitter.color = newColor;
                    emitter.enableDynamicAnchoring();
                    Particles.burst(emitter, 1);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getShipSystemId() == null) return null;
        ShipSystemSpecAPI system = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (system == null) return null;
        float regen = system.getRegen(null);
        if (regen <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueDecreasing(regen, 0f, 0.1f, 0.4f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0.25f; // hard to use for NPCs
    }
}
