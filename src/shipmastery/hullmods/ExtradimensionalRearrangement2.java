package shipmastery.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ExtradimensionalRearrangement2 extends BaseHullMod {

    public static final int ADDITIONAL_SMODS = 1;
    public static final float DAMAGE_DEALT_PER_SMOD = 0.1f;
    public static final float DAMAGE_TAKEN_PER_SMOD = 0.05f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, ADDITIONAL_SMODS);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new ExtradimensionalRearrangement2Script(id));
    }

    private record ExtradimensionalRearrangement2Script(String id) implements DamageTakenModifier, DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return modifyDamage(target, damage, DAMAGE_DEALT_PER_SMOD);
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return modifyDamage(target, damage, DAMAGE_TAKEN_PER_SMOD);
        }

        private String modifyDamage(CombatEntityAPI target, DamageAPI damage, float amountPerSMod) {
            if (!(target instanceof ShipAPI ship)) return null;
            if (ship.getVariant() == null) return null;
            int sMods = ship.getVariant().getSMods().size();
            if (sMods == 0) return null;
            damage.getModifier().modifyPercent(id, 100f * amountPerSMod * sMods);
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
