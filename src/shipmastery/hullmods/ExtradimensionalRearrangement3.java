package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExtradimensionalRearrangement3 extends BaseHullMod {
    public static final float INITIAL_FLUX_CAPACITY = 0.8f;
    public static final float FINAL_FLUX_CAPACITY = 1.2f;
    public static final float INITIAL_FLUX_GENERATION = 1.1f;
    public static final float[] FLUX_CAPACITY_PER = {0.004f, 0.002f, 0.00133333333f, 0.001f};
    public static final float[] FLUX_GENERATION_PER = {-0.002f, -0.001f, -0.00066666667f, -0.0005f};
    public static final int[] MAX_ACTIVATIONS;
    static {
        MAX_ACTIVATIONS = new int[] {
                Math.round((FINAL_FLUX_CAPACITY-INITIAL_FLUX_CAPACITY)/FLUX_CAPACITY_PER[0]),
                Math.round((FINAL_FLUX_CAPACITY-INITIAL_FLUX_CAPACITY)/FLUX_CAPACITY_PER[1]),
                Math.round((FINAL_FLUX_CAPACITY-INITIAL_FLUX_CAPACITY)/FLUX_CAPACITY_PER[2]),
                Math.round((FINAL_FLUX_CAPACITY-INITIAL_FLUX_CAPACITY)/FLUX_CAPACITY_PER[3])
        };
    }

    // Map fleet member id -> number of kills
    public static final String KILL_COUNT_KEY = "$sms_extradimensional_rearrangement3KillCountTracker";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null) return;
        String fmId = stats.getFleetMember().getId();
        if (fmId == null) return;
        //noinspection unchecked
        int kills = ((Map<String, Integer>) Global.getSector().getPersistentData()
                .getOrDefault(KILL_COUNT_KEY, new HashMap<>()))
                .getOrDefault(fmId, 0);
        applyStatMods(fmId, stats, hullSize, id, kills);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener((ShipDestroyedListener) (recentlyDamagedBy, target) -> {
            if (target.isFighter() || target.getOwner() == ship.getOwner()) return;
            if (!EngineUtils.shipOrOwnerInSet(recentlyDamagedBy, ship)) return;
            if (ship.getFleetMemberId() == null) return;
            if (Global.getCombatEngine().isSimulation() ||
                    Objects.equals(Global.getCombatEngine().getCustomExitButtonTitle(), Strings.RecentBattles.exitReplay)) return;
            //noinspection unchecked
            int kills = ((Map<String, Integer>) Global.getSector().getPersistentData()
                    .computeIfAbsent(KILL_COUNT_KEY, k -> new HashMap<>()))
                    .compute(ship.getFleetMemberId(), (k,v) -> v == null ? 1 : v + 1);
            applyStatMods(ship.getFleetMemberId(), ship.getMutableStats(), ship.getHullSize(), id, kills);
        });
    }

    private void applyStatMods(String fleetMemberId, MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String modifyId, int kills) {
        if (fleetMemberId == null) return;
        float capacityMod = getFluxCapacityMult(kills, hullSize);
        float generationMod = getFluxGenerationMult(kills, hullSize);

        if (capacityMod > 1f) {
            stats.getFluxCapacity().modifyPercent(modifyId, (capacityMod - 1f) * 100f);
        } else {
            stats.getFluxCapacity().modifyMult(modifyId, capacityMod);
        }

        if (generationMod > 1f) {
            stats.getMissileWeaponFluxCostMod().modifyPercent(modifyId, (generationMod - 1f) * 100f);
            stats.getBallisticWeaponFluxCostMod().modifyPercent(modifyId, (generationMod - 1f) * 100f);
            stats.getEnergyWeaponFluxCostMod().modifyPercent(modifyId, (generationMod - 1f) * 100f);
        } else {
            stats.getMissileWeaponFluxCostMod().modifyMult(modifyId, generationMod);
            stats.getBallisticWeaponFluxCostMod().modifyMult(modifyId, generationMod);
            stats.getEnergyWeaponFluxCostMod().modifyMult(modifyId, generationMod);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || ship.getFleetMemberId() == null) return;

        int hullSizeInt = Utils.hullSizeToInt(hullSize);
        tooltip.addPara(
                Strings.Hullmods.rearrangement3Effect1,
                8f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(FLUX_CAPACITY_PER[hullSizeInt]),
                Utils.asPercent(-FLUX_GENERATION_PER[hullSizeInt]),
                "" + MAX_ACTIVATIONS[hullSizeInt]);
        //noinspection unchecked
        int kills = ((Map<String, Integer>) Global.getSector().getPersistentData()
                .getOrDefault(KILL_COUNT_KEY, new HashMap<>()))
                .getOrDefault(ship.getFleetMemberId(), 0);

        float capacityMod = getFluxCapacityMult(kills, hullSize);
        float generationMod = getFluxGenerationMult(kills, hullSize);
        if (capacityMod >= 1f) {
            tooltip.addPara(Strings.Hullmods.rearrangement3Effect2Inc, 8f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent( capacityMod-1f));
        } else {
            tooltip.addPara(Strings.Hullmods.rearrangement3Effect2Dec, 8f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent( 1f-capacityMod));
        }
        if (generationMod >= 1f) {
            tooltip.addPara(Strings.Hullmods.rearrangement3Effect3Inc, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asPercent( generationMod-1f));
        } else {
            tooltip.addPara(Strings.Hullmods.rearrangement3Effect3Dec, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent( 1f-generationMod));
        }
        tooltip.addPara(Strings.Hullmods.rearrangement3Effect4, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, "" + kills);
    }

    private float getFluxCapacityMult(int kills, ShipAPI.HullSize hullSize) {
        int hullSizeInt = Utils.hullSizeToInt(hullSize);
        return INITIAL_FLUX_CAPACITY + FLUX_CAPACITY_PER[hullSizeInt] * Math.min(kills, MAX_ACTIVATIONS[hullSizeInt]);
    }

    private float getFluxGenerationMult(int kills, ShipAPI.HullSize hullSize) {
        int hullSizeInt = Utils.hullSizeToInt(hullSize);
        return INITIAL_FLUX_GENERATION + FLUX_GENERATION_PER[hullSizeInt] * Math.min(kills, MAX_ACTIVATIONS[hullSizeInt]);
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
