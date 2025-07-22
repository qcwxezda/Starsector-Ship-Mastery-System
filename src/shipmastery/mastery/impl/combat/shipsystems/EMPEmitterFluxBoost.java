package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.EMPEmitterDamageListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class EMPEmitterFluxBoost extends ShipSystemEffect {

    public static final int MAX_STACKS = 20;
    public static final float DECAY_RATE = 0.01f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrength(selectedVariant));
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EMPEmitterFluxBoost)
                                 .params(getSystemName(), str, str);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EMPEmitterFluxBoostPost, 0f,
                        new Color[]{Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR,
                                Settings.NEGATIVE_HIGHLIGHT_COLOR}, Utils.asInt(MAX_STACKS),
                        Utils.asPercent(MAX_STACKS * getStrength(selectedVariant)), Utils.asPercent(DECAY_RATE));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(EMPEmitterFluxBoostScript.class)) {
            ship.addListener(new EMPEmitterFluxBoostScript(ship, getStrength(ship)));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "emp";
    }

    class EMPEmitterFluxBoostScript implements EMPEmitterDamageListener, AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        float currentBoost = 0f;

        EMPEmitterFluxBoostScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
        }

        @Override
        public void reportEMPEmitterHit(ShipAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!target.isFighter()) {
                currentBoost += strength;
            }
        }

        @Override
        public void advance(float amount) {
            currentBoost = Math.min(currentBoost, MAX_STACKS * strength);

            if (currentBoost > 0f) {
                ship.getMutableStats().getFluxDissipation().modifyPercent(id, 100f * currentBoost);
                ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(id, currentBoost);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        getSystemSpec().getIconSpriteName(),
                        Strings.Descriptions.EMPEmitterFluxBoostTitle,
                        String.format(Strings.Descriptions.EMPEmitterFluxBoostDesc1, Utils.asPercentNoDecimal(currentBoost)),
                        false);
            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(id);
                ship.getMutableStats().getHardFluxDissipationFraction().unmodify(id);
            }

            currentBoost -= DECAY_RATE * amount;
            currentBoost = Math.max(0f, currentBoost);
        }
    }
}
