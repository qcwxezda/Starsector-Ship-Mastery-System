package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class FastSkimmer extends ShipSystemEffect {

    public static final float RANGE_MULT = 0.4f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FastSkimmer).params(getSystemName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedVariant);
        tooltip.addPara(
                Strings.Descriptions.FastSkimmerPost,
                0f,
                new Color[] {
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(Math.min(1f, strength/2f)),
                Utils.asPercent(1f-RANGE_MULT),
                Utils.asPercent(strength),
                Utils.asPercent(strength));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(FastSkimmerScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new FastSkimmerScript(ship, Math.max(0f, 1f-strength/2f), 1f+strength, 1f+strength, id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "displacer";
    }

    static class FastSkimmerScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float cooldownMult;
        final String id;

        FastSkimmerScript(ShipAPI ship, float cooldownMult, float regenMult, float usesMult, String id) {
            this.ship = ship;
            this.cooldownMult = cooldownMult;
            this.id = id;
            ship.getMutableStats().getSystemRangeBonus().modifyMult(id, RANGE_MULT);
            ship.getMutableStats().getSystemUsesBonus().modifyMult(id, usesMult);
            ship.getMutableStats().getSystemRegenBonus().modifyMult(id, regenMult);
            ship.getSystem().setAmmo((int) ship.getMutableStats().getSystemUsesBonus().computeEffective(ship.getSystem().getMaxAmmo()));
        }

        @Override
        public void onFullyActivate() {
            ship.getSystem().setCooldownRemaining(cooldownMult * ship.getSystem().getChargeDownDur());
        }
    }
}
