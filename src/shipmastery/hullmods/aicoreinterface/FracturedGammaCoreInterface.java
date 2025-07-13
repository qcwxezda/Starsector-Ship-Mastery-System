package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.HullModItemManager;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class FracturedGammaCoreInterface extends AICoreInterfaceHullmod {

    public static final float CR_THRESHOLD = 0.4f;
    public static final float REPAIR_AMT = 0.5f;
    public static final String HULLMOD_ID = "sms_fractured_gamma_core" + AICoreInterfaceHullmod.INTEGRATED_SUFFIX;

    public static class IntegrationScript extends BaseCampaignEventListener implements EveryFrameScript {
        private final IntervalUtil checkerInterval = new IntervalUtil(1f, 1.5f);

        public IntegrationScript() {
            super(false);
            Global.getSector().addTransientListener(this);
            Global.getSector().addTransientScript(this);
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        public void check() {
            var fleet = Global.getSector().getPlayerFleet();
            fleet.getFleetData().getMembersListCopy()
                    .stream()
                    .filter(fm -> !fm.isMothballed() && fm.getVariant().hasHullMod(HULLMOD_ID))
                    .forEach(fm -> {
                        if (fm.getStats() == null) return;
                        if (fm.getStats().getMaxCombatReadiness().getModifiedValue() <= CR_THRESHOLD) return;
                        if (fm.getRepairTracker().getCR() >= CR_THRESHOLD) return;
                        fm.getRepairTracker().applyCREvent(REPAIR_AMT, Strings.Items.integratedDesc);
                        // Give back item to random temp cargo so it doesn't get added back to the player fleet
                        HullModItemManager.getInstance().giveBackRequiredItems(HULLMOD_ID, fm, Global.getFactory().createCargo(true));
                        fm.getVariant().removePermaMod(HULLMOD_ID);
                    });
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            check();
        }

        @Override
        public void advance(float amount) {
            checkerInterval.advance(amount);
            if (checkerInterval.intervalElapsed()) {
                check();
            }
        }
    }

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 25000f, 75000f);
    }

    @Override
    public String getCannotIntegrateReason(FleetMemberAPI member) {
        if (member.getRepairTracker() == null || member.getRepairTracker().getCR() < CR_THRESHOLD) {
            return String.format(Strings.Items.fracturedGammaCoreIntegrationCannotAdd, Utils.asPercent(CR_THRESHOLD));
        }
        return super.getCannotIntegrateReason(member);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(
                Strings.Items.fracturedGammaCoreIntegrationEffect,
                0f,
                new Color[] {
                        Misc.getHighlightColor(),
                        Misc.getHighlightColor(),
                        Misc.getNegativeHighlightColor()
                },
                Utils.asPercent(CR_THRESHOLD),
                Utils.asPercent(REPAIR_AMT),
                Utils.asPercent(CR_THRESHOLD));
    }
}
