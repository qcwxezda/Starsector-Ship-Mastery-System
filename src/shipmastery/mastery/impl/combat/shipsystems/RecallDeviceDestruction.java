package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import particleengine.Emitter;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Iterator;

public class RecallDeviceDestruction extends ShipSystemEffect {
    public float getEffectRadius(PersonAPI commander, ShipAPI.HullSize hullSize) {
        float strength = getStrength(commander);
        return strength + Utils.hullSizeToInt(hullSize) * strength/3.5f;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float radius = getEffectRadius(Global.getSector().getPlayerPerson(), selectedModule.getHullSize());
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RecallDeviceDestruction).params(getSystemName(), (int) radius);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.RecallDeviceDestructionPost, 0f);
    }

    @Override
    public void onFlagshipStatusGainedIfHasSystem(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        if (!ship.hasListenerOfClass(RecallDeviceDestructionScript.class)) {
            ship.addListener(new RecallDeviceDestructionScript(ship, getEffectRadius(commander, ship.getHullSize())));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(RecallDeviceDestructionScript.class);
    }

    @Override
    public String getSystemSpecId() {
        return "recalldevice";
    }

    public static class RecallDeviceDestructionScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float effectRadius;

        RecallDeviceDestructionScript(ShipAPI ship, float effectRadius) {
            this.ship = ship;
            this.effectRadius = effectRadius;
        }

        @Override
        public void onActivate() {
            Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 2f*effectRadius + 2f*ship.getCollisionRadius(), 2f*effectRadius + 2f*ship.getCollisionRadius());
            while (itr.hasNext()) {
                Object o = itr.next();
                if (!(o instanceof CombatEntityAPI entity)) continue;
                if (MathUtils.dist(ship.getLocation(), entity.getLocation()) > effectRadius + ship.getCollisionRadius()) continue;
                if (!(o instanceof ShipAPI target)) continue;
                if (target.isFighter() && target.getOwner() != ship.getOwner()) {
                    Global.getCombatEngine().removeEntity(target);

                    float particlesPerSecond = 100f, maxDuration = 0.5f;
                    JitterEmitter fighterJitter = new JitterEmitter(entity, target.getSpriteAPI(), new Color(0.8f, 1f, 1f, 1f), 0f, entity.getCollisionRadius(), 0.5f, true, 0.8f,
                                                             (int) (particlesPerSecond*maxDuration));
                    fighterJitter.setBaseIntensity(0.6f);
                    Particles.stream(fighterJitter, 1, particlesPerSecond, maxDuration);
                }
            }
            float particlesPerSecond = 50f, maxDuration = 1f;
            JitterEmitter shipJitter = new JitterEmitter(ship, ship.getSpriteAPI(), new Color(0.8f, 1f, 1f, 1f), 60f, 30f, 0.8f, false, 0.2f,
                                                         (int) (particlesPerSecond*maxDuration));
            shipJitter.enableDynamicAnchoring();
            Particles.stream(shipJitter, 1, particlesPerSecond, maxDuration);

            Emitter circleEmitter = Particles.initialize(ship.getLocation(), "graphics/fx/fog_circle.png");
            circleEmitter.size(2f * (effectRadius + ship.getCollisionRadius()), 2f * (effectRadius + ship.getCollisionRadius()));
            circleEmitter.fadeTime(0.2f, 0.2f, 0.8f, 0.8f);
            circleEmitter.color(0.8f, 1f, 1f, 0.175f);
            Particles.burst(circleEmitter, 1);
        }
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return !fm.isFlagship() ? 0f : 10f*super.getNPCWeight(fm);
    }
}
