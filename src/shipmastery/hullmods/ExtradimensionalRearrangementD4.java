package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;

import java.awt.Color;

public class ExtradimensionalRearrangementD4 extends BaseHullMod {

    public static final float CR_LOSS_PER_100_DAMAGE = 0.04f;
    public static final float MAX_CR_LOSS_PER_HIT = 1f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener((DamageTakenModifier) (param, target, damage, point, shieldHit) -> {
            if (ship.getCurrentCR() <= 0.2f) return null;
            float amount = damage.getDamage();
            ship.setCurrentCR(Math.max(0f, ship.getCurrentCR() - Math.min(MAX_CR_LOSS_PER_HIT, CR_LOSS_PER_100_DAMAGE*amount/10000f)));
            return null;
        });
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(Strings.Hullmods.rearrangementD4Effect, 8f);
    }

    @Override
    public Color getBorderColor() {
        return Settings.MASTERY_COLOR;
    }

    @Override
    public Color getNameColor() {
        return Settings.MASTERY_COLOR;
    }
}
