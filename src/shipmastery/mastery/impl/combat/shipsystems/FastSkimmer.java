package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class FastSkimmer extends BaseMasteryEffect {

    public static final float RANGE_MULT = 0.4f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FastSkimmer).params(selectedModule.getSystem().getDisplayName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        tooltip.addPara(
                Strings.Descriptions.FastSkimmerPost,
                0f,
                new Color[] {
                        Misc.getHighlightColor(),
                        Misc.getNegativeHighlightColor(),
                        Misc.getHighlightColor(),
                        Misc.getHighlightColor()},
                Utils.asPercent(Math.min(1f, strength/2f)),
                Utils.asPercent(1f-RANGE_MULT),
                Utils.asPercent(strength),
                Utils.asPercent(strength));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"displacer".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(FastSkimmerScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new FastSkimmerScript(ship, Math.max(0f, 1f-strength/2f), 1f+strength, 1f+strength, id));
        }
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
