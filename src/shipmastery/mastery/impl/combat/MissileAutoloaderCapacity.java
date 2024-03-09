package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.hullmods.MissileAutoloader;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MissileAutoloaderCapacity extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MissileAutoloaderCapacity).params(
                Global.getSettings().getHullModSpec(HullMods.MISSILE_AUTOLOADER).getDisplayName(),
                Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship) {
        if (!ship.getVariant().hasHullMod(HullMods.MISSILE_AUTOLOADER)) return;
        MissileAutoloader.MissileAutoloaderData data = (MissileAutoloader.MissileAutoloaderData) ship.getCustomData().get(MissileAutoloader.MA_DATA_KEY);
        if (data == null) {
            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    applyEffectsAfterShipCreation(ship);
                }
            }, 0.5f);
        }
        else {
            data.opLeft *= 1f + getStrength(ship);
            data.opLeft = (int) data.opLeft;
        }
    }
}
