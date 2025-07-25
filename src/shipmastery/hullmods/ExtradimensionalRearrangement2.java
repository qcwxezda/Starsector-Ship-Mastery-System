package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ExtradimensionalRearrangement2 extends BaseHullMod {

    public static final int ADDITIONAL_SMODS = 1;
    public static final float DAMAGE_DEALT_PER_SMOD = 0.05f;
    public static final float DAMAGE_TAKEN_PER_SMOD = 0.05f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, ADDITIONAL_SMODS);
        if (stats.getVariant() == null) return;
        int num = Misc.getCurrSpecialMods(stats.getVariant());
        stats.getArmorDamageTakenMult().modifyPercent(id, 100f * num * DAMAGE_TAKEN_PER_SMOD);
        stats.getHullDamageTakenMult().modifyPercent(id, 100f * num * DAMAGE_TAKEN_PER_SMOD);
        stats.getShieldDamageTakenMult().modifyPercent(id, 100f * num * DAMAGE_TAKEN_PER_SMOD);
        stats.getEmpDamageTakenMult().modifyPercent(id, 100f * num * DAMAGE_TAKEN_PER_SMOD);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new ExtradimensionalRearrangement2Script(id));
    }

    private record ExtradimensionalRearrangement2Script(String id) implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return modifyDamage(target, damage);
        }

        private String modifyDamage(CombatEntityAPI target, DamageAPI damage) {
            if (!(target instanceof ShipAPI ship)) return null;
            if (ship.getVariant() == null) return null;
            int sMods = Misc.getCurrSpecialMods(ship.getVariant());
            if (sMods == 0) return null;
            damage.getModifier().modifyPercent(id, 100f * ExtradimensionalRearrangement2.DAMAGE_DEALT_PER_SMOD * sMods);
            return id;
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangement2Effect,
                8f,
                new Color[]{Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(DAMAGE_DEALT_PER_SMOD),
                Utils.asPercent(DAMAGE_TAKEN_PER_SMOD),
                "" + ADDITIONAL_SMODS);
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
