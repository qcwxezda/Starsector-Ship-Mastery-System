package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.impl.logistics.SModsOverCapacity;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.*;

import java.util.List;

public class SModTableRowPressed extends TriggerableProxy {

    final MasteryPanel masteryPanel;
    final ShipAPI module;
    final ShipAPI root;
    long lastClickTime = 0;


    public SModTableRowPressed(MasteryPanel masteryPanel, ShipAPI module, ShipAPI root) {
        super(ClassRefs.uiTableDelegateClass, ClassRefs.uiTableDelegateMethodName);
        this.masteryPanel = masteryPanel;
        this.root = root;
        this.module = module;
    }

    // arg0: table; arg1: row; arg2: event into
    @Override
    public void trigger(Object... args) {
        if (args.length != 3) return;

        Object row = args[1];
        MasteryPanel.TableRowData rowData = (MasteryPanel.TableRowData) ReflectionUtils.invokeMethod(row, "getData");

        ShipVariantAPI variant = module.getVariant();
        ShipVariantAPI rootVariant = root.getVariant();
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(rowData.hullModSpecId);

        final int cur = variant.getSMods().size();
        int limit = SModUtils.getMaxSMods(module.getMutableStats());

        // Don't track as over-capacity if it's from the lvl 3 enhancement bonus
        boolean isLogistic = spec.hasUITag("Logistics");
        boolean hasLogisticsBonus = rootVariant != null && MasteryUtils.getEnhanceCount(rootVariant.getHullSpec()) >= 3;
        boolean hasLogistics = false;
        for (String id : variant.getSMods()) {
            if (Global.getSettings().getHullModSpec(id).hasUITag("Logistics")) {
                hasLogistics = true;
                break;
            }
        }
        boolean logisticDontTrack = (isLogistic && hasLogisticsBonus && !hasLogistics);
        if (hasLogisticsBonus && hasLogistics) limit++;

        final ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
        if (rowData.cantBuildInReason == null) {
            long newTime = System.currentTimeMillis();
            if (!button.isHighlighted()) {
                exclusiveHighlight(args[0], row);

                if (rowData.isModular && cur >= limit && !logisticDontTrack && !Global.getSettings().isDevMode()) {
                    Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.MasteryPanel.buildInOverMaxWarning, Misc.getNegativeHighlightColor());
                }

                DeferredActionPlugin.performLater(new Action() {
                    @Override
                    public void perform() {
                        button.unhighlight();
                    }
                }, Settings.DOUBLE_CLICK_INTERVAL);

            }
            else if (newTime - lastClickTime > (int) (Settings.DOUBLE_CLICK_INTERVAL * 1000f)) {
                button.unhighlight();
            } else {
                if (rootVariant != null) {
                    String name = spec.getDisplayName();
                    if (SModUtils.isHullmodBuiltIn(spec, variant)) {
                        variant.getSModdedBuiltIns().add(rowData.hullModSpecId);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.MasteryPanel.enhanceConfirm + name, Settings.MASTERY_COLOR);
                    }
                    else {
                        if (module.getFleetMember() != null && cur >= limit && !logisticDontTrack) {
                            final int finalLimit = limit;
                            DeferredActionPlugin.performLater(new Action() {
                                @Override
                                public void perform() {
                                    SModsOverCapacity.trackOverCapacityMod(module.getFleetMember(), cur - finalLimit);
                                }
                            }, 0f);
                        }
                        variant.addPermaMod(rowData.hullModSpecId, true);
                        if (module.getFleetMember() != null) {
                            ShipMasterySModRecord record = new ShipMasterySModRecord(module.getFleetMember());
                            record.getSMods().add(rowData.hullModSpecId);
                            record.setSPSpent(0);
                            record.setMPSpent(rowData.mpCost);
                            record.setCreditsSpent(rowData.creditsCost);
                            PlaythroughLog.getInstance().getSModsInstalled().add(record);
                        }
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.MasteryPanel.builtInConfirm + name, Settings.MASTERY_COLOR);
                    }
                    Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);

                    ShipMastery.spendPlayerMasteryPoints(rootVariant.getHullSpec(), rowData.mpCost);
                    Utils.getPlayerCredits().subtract(rowData.creditsCost);
                    masteryPanel.forceRefresh(true, true, true, false);
                }
                button.unhighlight();
            }
            lastClickTime = newTime;
        }
        else {
            exclusiveHighlight(args[0], null);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(rowData.cantBuildInReason, Misc.getNegativeHighlightColor());
            Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
        }
    }

    void exclusiveHighlight(Object table, Object rowToHighlight) {
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        for (Object row : rows) {
            ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
            if (row == rowToHighlight && row != null) {
                button.highlight();
            }
            else {
                button.unhighlight();
            }
        }
    }
}
