package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Set;

public class DroneStrikeRegen extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedVariant);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.DroneStrikeRegen)
                .params(getSystemName(), Utils.asPercent(strength), Utils.asPercent(strength));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(DroneStrikeRegenScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new DroneStrikeRegenScript(ship, strength, strength, id));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.DroneStrikeRegenPost, 0f);
    }

    @Override
    public String getSystemSpecId() {
        return "drone_strike";
    }

    static class DroneStrikeRegenScript extends BaseShipSystemListener implements ShipDestroyedListener {
        final ShipAPI ship;
        final String id;
        final float increase;
        final float replaceChance;

        DroneStrikeRegenScript(ShipAPI ship, float increase, float replaceChance, String id) {
            this.ship = ship;
            this.id = id;
            this.increase = increase;
            this.replaceChance = replaceChance;
        }

        @Override
        public void onActivate() {
            List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
            if (bays != null) {
                for (FighterLaunchBayAPI bay : bays) {
                    if (bay.getWing() == null || !"terminator_wing".equals(bay.getWing().getSpec().getId())) continue;
                    if (getNumLost(bay) > bay.getFastReplacements()) {
                        if (Math.random() <= replaceChance) {
                            bay.makeCurrentIntervalFast();
                            bay.setFastReplacements(bay.getFastReplacements() + 1);
                        }
                        break;
                    }
                }
            }
        }

        // bay.getNumLost() doesn't report terminator drone losses for some reason
        int getNumLost(FighterLaunchBayAPI bay) {
            return bay.getWing().getSpec().getNumFighters() - bay.getWing().getWingMembers().size();
        }

        @Override
        public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
            if (target.isFighter()) return;
            Object lastDamagedBy = target.getParamAboutToApplyDamage();
            if (lastDamagedBy instanceof DamagingProjectileAPI proj) {
                if (proj.getSource() == ship && "terminator_missile_proj".equals(proj.getProjectileSpecId())) {
                    List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
                    if (bays != null) {
                        for (FighterLaunchBayAPI bay : bays) {
                            bay.setCurrRate(Math.min(1f, bay.getCurrRate() + increase));
                        }
                    }
                }
            }
        }
    }
}
