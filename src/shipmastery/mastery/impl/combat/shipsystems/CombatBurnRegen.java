package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class CombatBurnRegen extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .init(Strings.Descriptions.CombatBurnRegen)
                .params(
                        Utils.asFloatOneDecimal(strength),
                        getSystemName(),
                        Utils.asFloatOneDecimal(10f*strength),
                        Utils.asFloatOneDecimal(2f*strength),
                        Utils.asFloatOneDecimal(250f / strength),
                        getSystemName(),
                        Global.getSettings().getWeaponSpec("heavy_adjudicator").getWeaponName())
                .colors(
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR
                );
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        float strength = getStrength(ship);
        ship.addListener(new CombatBurnRegenScript(ship, id, strength, 10f*strength, 2f*strength, strength / 250f));
    }

    static class CombatBurnRegenScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final String id;
        final float speedIncreasePerSecond;
        final float maxSpeedIncrease;
        final float speedDecreasePerSecond;
        final float regenProgressPerSecond;
        float curIncrease = 0f;
        float curRegenProgress = 1f;
        final List<WeaponAPI> heavyAdjudicators = new ArrayList<>();

        CombatBurnRegenScript(ShipAPI ship, String id, float increase, float max, float decrease, float regen) {
            this.ship = ship;
            this.id = id;
            this.speedIncreasePerSecond = increase;
            this.speedDecreasePerSecond = decrease;
            this.maxSpeedIncrease = max;
            this.regenProgressPerSecond = regen;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDecorative()) continue;
                if ("heavy_adjudicator".equals(weapon.getId())) {
                    heavyAdjudicators.add(weapon);
                }
            }
        }

        @Override
        public void advanceWhileOn(float amount) {
            curIncrease = Math.min(maxSpeedIncrease, curIncrease + speedIncreasePerSecond*amount);
        }

        @Override
        public void advance(float amount) {
            if (!ship.getSystem().isOn()) {
                curIncrease = Math.max(0f, curIncrease - speedDecreasePerSecond * amount);
            }
            ship.getMutableStats().getMaxSpeed().modifyFlat(id, curIncrease);
            curRegenProgress += regenProgressPerSecond*amount;
            if (curIncrease > 0f) {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id + "1",
                        "graphics/icons/hullsys/combat_burn.png",
                        Strings.Descriptions.CombatBurnRegenTitle,
                        String.format(Strings.Descriptions.CombatBurnRegenDesc1, Utils.asInt(curIncrease)),
                        false);
            }
            if (curRegenProgress < 1f) {
                float secondsToRecharge = (1f - curRegenProgress) / regenProgressPerSecond;
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id + "2",
                        "graphics/icons/hullsys/combat_burn.png",
                        Strings.Descriptions.CombatBurnRegenTitle,
                        String.format(Strings.Descriptions.CombatBurnRegenDesc2, Utils.asInt(secondsToRecharge)),
                        false);
            } else {
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id + "2",
                        "graphics/icons/hullsys/combat_burn.png",
                        Strings.Descriptions.CombatBurnRegenTitle,
                        Strings.Descriptions.CombatBurnRegenDesc3,
                        false);
            }
        }

        @Override
        public void onActivate() {
            if (curRegenProgress >= 1f) {
                boolean needsRegen = false;
                for (WeaponAPI weapon : heavyAdjudicators) {
                    if (weapon.getAmmo() < weapon.getMaxAmmo()) {
                        needsRegen = true;
                        break;
                    }
                }
                if (needsRegen) {
                    for (WeaponAPI weapon : heavyAdjudicators) {
                        weapon.setAmmo(weapon.getMaxAmmo());
                    }
                    curRegenProgress = 0f;
                }
            }
        }
    }

    @Override
    public String getSystemSpecId() {
        return "combat_burn";
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if ("onslaught_mk1".equals(spec.getHullId())) {
            return 2f;
        }
        return null;
    }
}
