package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Objects;
import java.util.Set;

public class ExtradimensionalRearrangement5 extends BaseHullMod implements HullModFleetEffect {
    public static final float SENSOR_RANGE_MULT = 1.1f;
    public static final float SENSOR_PROFILE_MULT = 0.9f;
    public static final int DP_PER_SP = 500;
    public static final int DP_PER_SP_INCREMENT = 50;
    public static final String CURRENT_DP_DESTROYED_KEY = "$sms_extradimensional_rearrangement5DPDestroyed";
    public static final String NUM_TIMES_ACTIVATED_KEY = "$sms_extradimensional_rearrangement5Activations";
    public static final String MOD_KEY = "sms_extradimensional_rearrangement5";
    public static final String HULLMOD_ID = "sms_extradimensional_rearrangement5";

    private static class DPDestroyedTracker implements ShipDestroyedListener {
        @Override
        public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
            if (target.isFighter()) return;
            if (target.getFleetMember() == null) return;
            if (Global.getCombatEngine().isSimulation() || Objects.equals(Global.getCombatEngine().getCustomExitButtonTitle(), Strings.RecentBattles.exitReplay)) return;

            float dp = target.getFleetMember().getDeploymentPointsCost();
            boolean counts = false;
            for (ShipAPI ship : recentlyDamagedBy) {
                if (ship.getOwner() == 0) {
                    counts = true;
                    break;
                }
            }
            if (!counts) return;
            float cur = (float) Global.getSector().getPersistentData().getOrDefault(CURRENT_DP_DESTROYED_KEY, 0f);
            float next = cur + dp;
            int spGained = (int) next / getDpPerSp();
            next = next % getDpPerSp();

            if (spGained > 0) {
                Global.getSector().getPlayerStats().addStoryPoints(spGained);
                Global.getCombatEngine().getCombatUI().addMessage(
                        1,
                        Strings.Hullmods.rearrangement5Message1,
                        Misc.getStoryBrightColor(),
                        "" + spGained, Misc.getTextColor(),
                        spGained == 1 ? Strings.Hullmods.rearrangement5Message2Singular
                                : Strings.Hullmods.rearrangement5Message2Plural);
                Global.getSector().getPersistentData().compute(NUM_TIMES_ACTIVATED_KEY,
                        (k, v) -> v == null ? spGained : (int) v + spGained);
            }

            Global.getSector().getPersistentData().put(CURRENT_DP_DESTROYED_KEY, next);
        }
    }

    public static int getDpPerSp() {
        return DP_PER_SP + DP_PER_SP_INCREMENT * (int) Global.getSector().getPersistentData().getOrDefault(NUM_TIMES_ACTIVATED_KEY, 0);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getFleetCommander() == null || !ship.getFleetCommander().isPlayer()) return;
        ship.addListener(new DPDestroyedTracker());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangement5Effect,
                8f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(SENSOR_RANGE_MULT - 1f),
                Utils.asPercent(1f - SENSOR_PROFILE_MULT),
                "" + getDpPerSp(),
                Utils.asInt((float) Global.getSector().getPersistentData().getOrDefault(CURRENT_DP_DESTROYED_KEY, 0f)),
                "" + getDpPerSp());
    }

    @Override
    public Color getBorderColor() {
        return Settings.MASTERY_COLOR;
    }

    @Override
    public Color getNameColor() {
        return Settings.MASTERY_COLOR;
    }

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        boolean has = false;
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            if (fm.getVariant() != null && fm.getVariant().hasHullMod(HULLMOD_ID)) {
                has = true;
                break;
            }
        }

        if (!has) {
            fleet.getDetectedRangeMod().unmodify(MOD_KEY);
            fleet.getSensorRangeMod().unmodify(MOD_KEY);
        } else {
            fleet.getDetectedRangeMod().modifyMult(MOD_KEY, SENSOR_PROFILE_MULT, Strings.Hullmods.rearrangement5Desc);
            fleet.getSensorRangeMod().modifyMult(MOD_KEY, SENSOR_RANGE_MULT, Strings.Hullmods.rearrangement5Desc);
        }
    }
}
