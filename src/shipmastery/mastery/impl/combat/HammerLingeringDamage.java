package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.FireEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HammerLingeringDamage extends BaseMasteryEffect {

    public static final float DAMAGE_OVER_TIME_DURATION = 10f;
    public static final float AMMO_GAIN = 1f;
    public static final String ON_FIRE_KEY = "sms_on_fire";
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.HammerLingeringDamage).params(
                Utils.asPercentNoDecimal(AMMO_GAIN)).colors(Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.HammerLingeringDamagePost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(getStrength(selectedModule)),
                Utils.asInt(300f * getStrength(selectedModule)),
                Utils.asInt(DAMAGE_OVER_TIME_DURATION));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(HammerLingeringDamageScript.class)) {
            ship.addListener(new HammerLingeringDamageScript(ship, getStrength(ship), AMMO_GAIN, 300f * getStrength(ship)));
        }
    }

    static class HammerLingeringDamageScript implements DamageDealtModifier {

        final ShipAPI ship;
        final float damageFrac;
        final float effectRadius;
        final Set<WeaponAPI> hammerWeapons = new HashSet<>();

        HammerLingeringDamageScript(ShipAPI ship, float damageFrac, float ammoGain, float effectRadius) {
            this.ship = ship;
            this.damageFrac = damageFrac;
            this.effectRadius = effectRadius;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                Object projSpec = weapon.getSpec().getProjectileSpec();
                if (projSpec instanceof MissileSpecAPI missileSpec) {
                    if ("hammer_torp".equals(missileSpec.getHullSpec().getHullId())) {
                        hammerWeapons.add(weapon);
                        int baseAmmo = weapon.getSpec().getMaxAmmo();
                        int additionalAmmo = (int) (ammoGain * baseAmmo);
                        int newAmmo = weapon.getMaxAmmo() + additionalAmmo;
                        weapon.setMaxAmmo(newAmmo);
                        weapon.setAmmo(newAmmo);
                    }
                }
            }
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f pt, boolean shieldHit) {
            if (shieldHit) return null;
            if (!(param instanceof MissileAPI missile)) return null;
            if (!hammerWeapons.contains(missile.getWeapon())) return null;
            if (!(target instanceof ShipAPI targetShip)) return null;

            List<HammerLingeringDamageDealer.LingeringDamageData> damageData = new ArrayList<>();
            float[][] armorGrid = targetShip.getArmorGrid().getGrid();
            for (int i = 0; i < armorGrid.length; i++) {
                for (int j = 0; j < armorGrid[0].length; j++) {
                    Vector2f loc = targetShip.getArmorGrid().getLocation(i, j);
                    float dist = MathUtils.dist(loc, pt);
                    float linearMult = Math.max(0f, (effectRadius - dist)) / effectRadius;
                    float armorDamage = damageFrac * missile.getDamageAmount() * linearMult / 15f;
                    if (armorDamage > 0f) {
                        damageData.add(new HammerLingeringDamageDealer.LingeringDamageData(i, j, armorDamage));
                    }
                }
            }

            if (!damageData.isEmpty()) {
                targetShip.addListener(new HammerLingeringDamageDealer(ship, targetShip, damageData));
            }

            return null;
        }
    }

    static class HammerLingeringDamageDealer implements AdvanceableListener {

        final ShipAPI source, target;
        final IntervalUtil tickInterval = new IntervalUtil(0.75f, 0.75f);
        final Map<Pair<Integer, Integer>, Float> dpsAmounts = new HashMap<>();
        float elapsed = 0f;
        float maxDpsInGrid = 0f;
        final FireEmitter emitter;

        static class LingeringDamageData {
            final int i;
            final int j;
            final float damage;

            LingeringDamageData(int i, int j, float damage) {
                this.i = i;
                this.j = j;
                this.damage = damage;
            }
        }

        HammerLingeringDamageDealer(ShipAPI source, ShipAPI target, List<LingeringDamageData> toDamage) {
            this.source = source;
            this.target = target;
            for (LingeringDamageData data : toDamage) {
                dpsAmounts.put(new Pair<>(data.i, data.j), data.damage / DAMAGE_OVER_TIME_DURATION);
                maxDpsInGrid = Math.max(data.damage / DAMAGE_OVER_TIME_DURATION, maxDpsInGrid);
            }
            emitter = new FireEmitter(new Vector2f());
            emitter.randRadius = 12f;
            emitter.driftSpeed = 25f;
            emitter.size = 40f;
        }

        @Override
        public void advance(float amount) {
            // Remove if destroyed, so we don't get array index out of bounds...
            if (!target.isAlive()) {
                target.removeListener(this);
                return;
            }

            tickInterval.advance(amount);
            if (tickInterval.intervalElapsed()) {
                float totalArmorDamage = 0f;
                Vector2f averageDamageLocation = new Vector2f();
                int count = 0;

                BoundsAPI shipBounds = target.getExactBounds();
                List<Vector2f> bounds = new ArrayList<>();
                if (shipBounds != null && shipBounds.getSegments() != null && !shipBounds.getSegments().isEmpty()) {
                    shipBounds.update(target.getLocation(), target.getFacing());
                    List<BoundsAPI.SegmentAPI> segments = shipBounds.getSegments();
                    for (int i = 0; i < segments.size(); i++) {
                        BoundsAPI.SegmentAPI segment = segments.get(i);
                        if (i == 0) bounds.add(segment.getP1());
                        bounds.add(segment.getP2());
                    }
                }
                for (Map.Entry<Pair<Integer, Integer>, Float> dps : dpsAmounts.entrySet()) {
                    Pair<Integer, Integer> gridIndex = dps.getKey();
                    final int i = gridIndex.one, j = gridIndex.two;
                    final float dpsAmount = dps.getValue();
                    float damage = dpsAmount * tickInterval.getIntervalDuration();

                    float[][] armorGrid = target.getArmorGrid().getGrid();
                    if (damage <= armorGrid[i][j]) {
                        armorGrid[i][j] -= damage;
                        totalArmorDamage += damage;
                    }
                    else {
                        totalArmorDamage += armorGrid[i][j];
                        armorGrid[i][j] = 0f;
                    }
                    Vector2f gridLocation = target.getArmorGrid().getLocation(i, j);
                    Vector2f.add(averageDamageLocation, gridLocation, averageDamageLocation);
                    count++;

                    if (shipBounds != null && Misc.isPointInBounds(gridLocation, bounds)) {
                        Particles.stream(
                                emitter, 1, 3f, tickInterval.getIntervalDuration(),
                                fireEmitter -> {
                                    if (!target.isAlive()) return false;
                                    emitter.location = target.getArmorGrid().getLocation(i, j);
                                    emitter.alphaMult = dpsAmount / maxDpsInGrid;
                                    Pair<Integer, Integer> index = new Pair<>(i, j);
                                    if (target.getCustomData() == null ||
                                            !target.getCustomData().containsKey(ON_FIRE_KEY)) {
                                        Map<Object, Object> map = new HashMap<>();
                                        map.put(index, HammerLingeringDamageDealer.this);
                                        target.setCustomData(ON_FIRE_KEY, map);
                                        return true;
                                    } else {
                                        //noinspection unchecked
                                        Map<Object, Object> onFire =
                                                (Map<Object, Object>) target.getCustomData().get(ON_FIRE_KEY);
                                        Object listener = onFire.get(index);
                                        if (HammerLingeringDamageDealer.this == listener) {
                                            return true;
                                        }
                                        if (listener == null || !target.hasListener(listener)) {
                                            onFire.put(index, HammerLingeringDamageDealer.this);
                                            return true;
                                        }
                                        return false;
                                    }
                                });
                    }
                }
                averageDamageLocation.x /= count;
                averageDamageLocation.y /= count;

                if (Misc.shouldShowDamageFloaty(source, target)) {
                    if (totalArmorDamage > 0f) {
                        Global.getCombatEngine().addFloatingDamageText(averageDamageLocation, totalArmorDamage,
                                                                       Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, source);
                    }
                }
                target.syncWithArmorGridState();

                elapsed += tickInterval.getIntervalDuration();
                if (elapsed >= DAMAGE_OVER_TIME_DURATION) {
                    target.removeListener(this);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f);
        if (weight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 0.4f, 1f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        float score = 0f;
        for (String id : fm.getVariant().getFittedWeaponSlots()) {
            var spec = fm.getVariant().getWeaponSpec(id);
            if (spec != null  && spec.getProjectileSpec() instanceof MissileSpecAPI mSpec) {
                if ("hammer_torp".equals(mSpec.getHullSpec().getHullId())) {
                    score += switch (spec.getSize()) {
                        case SMALL -> 1f;
                        case MEDIUM -> 2f;
                        case LARGE -> 4f;
                    };
                }
            }
        }
        return score / 3f;
    }
}
