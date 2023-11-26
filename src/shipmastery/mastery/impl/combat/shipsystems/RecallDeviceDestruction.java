package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import particleengine.Emitter;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.Iterator;

public class RecallDeviceDestruction extends BaseMasteryEffect {

    static final float[] EFFECT_RADIUS = new float[] {400f, 500f, 600f, 700f};

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float radius = selectedModule.getMutableStats().getSystemRangeBonus().computeEffective(EFFECT_RADIUS[Utils.hullSizeToInt(selectedModule.getHullSize())]);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RecallDeviceDestruction).params(Strings.Descriptions.RecallDeviceName, (int) radius);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.RecallDeviceDestructionPost, 0f);
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null || ship.getSystem() == null || !"recalldevice".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(RecallDeviceDestructionScript.class)) {
            ship.addListener(new RecallDeviceDestructionScript(ship));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(RecallDeviceDestructionScript.class);
    }

    public static class RecallDeviceDestructionScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float effectRadius;

        RecallDeviceDestructionScript(ShipAPI ship) {
            this.ship = ship;
            effectRadius = ship.getMutableStats().getSystemRangeBonus().computeEffective(EFFECT_RADIUS[Utils.hullSizeToInt(ship.getHullSize())]);
        }

        @Override
        public void onActivate() {
            Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 2f*effectRadius + 2f*ship.getCollisionRadius(), 2f*effectRadius + 2f*ship.getCollisionRadius());
            while (itr.hasNext()) {
                Object o = itr.next();
                if (!(o instanceof CombatEntityAPI)) continue;
                CombatEntityAPI entity = (CombatEntityAPI) o;
                if (MathUtils.dist(ship.getLocation(), entity.getLocation()) > effectRadius + ship.getCollisionRadius()) continue;
                if (!(o instanceof ShipAPI)) continue;
                ShipAPI target = (ShipAPI) o;
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
}
