package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.ShipMasterySModRecord;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;

public class SModTableRowPressed extends TriggerableProxy {

    final MasteryPanel masteryPanel;
    final ShipAPI module;
    final ShipAPI root;
    long lastClickTime = 0;
    public static final float CREDITS_FOR_NO_BONUS_XP = 200000f;

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

//        final int cur = variant.getSMods().size();
//        int limit = masteryPanel.getSModLimit(module).one;

        // Don't track as over-capacity if it's from the lvl 3 enhancement bonus
//        boolean isLogistic = spec.hasUITag(HullMods.TAG_UI_LOGISTICS);
//        boolean hasLogisticsBonus = rootVariant != null && SModUtils.hasBonusLogisticSlot(variant);
//        boolean hasLogistics = SModUtils.hasLogisticSMod(variant);
        // boolean logisticDontTrack = (isLogistic && hasLogisticsBonus && !hasLogistics);
        //if (hasLogisticsBonus && hasLogistics) limit++;

        final ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
        if (rowData.cantBuildInReason == null) {
            long newTime = System.currentTimeMillis();
            if (!button.isHighlighted()) {
                exclusiveHighlight(args[0], row);

//                if (rowData.isModular && cur >= limit && !logisticDontTrack && !Global.getSettings().isDevMode()) {
//                    Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.MasteryPanel.buildInOverMaxWarning, Misc.getNegativeHighlightColor());
//                }

                DeferredActionPlugin.performLater(button::unhighlight, Settings.DOUBLE_CLICK_INTERVAL);

            }
            else if (newTime - lastClickTime > (int) (Settings.DOUBLE_CLICK_INTERVAL * 1000f)) {
                button.unhighlight();
            } else {
                if (rootVariant != null) {
                    String name = spec.getDisplayName();

                    boolean isEnhance = SModUtils.isHullmodBuiltIn(spec, variant);
                    float bonusXPFraction = 0f;
                    if (masteryPanel.isUsingSP()) {
                        float origCreditsCost = SModUtils.getCreditsCost(spec, module);
                        bonusXPFraction = isEnhance ? 1f : 1f - Math.min(1f, origCreditsCost / CREDITS_FOR_NO_BONUS_XP);
                        Global.getSector().getPlayerStats().spendStoryPoints(
                                1,
                                true,
                                null,
                                true,
                                bonusXPFraction,
                                String.format(
                                        Strings.RefitScreen.sModAutofitSPText,
                                        module.getName(),
                                        module.getHullSpec().getNameWithDesignationWithDashClass(),
                                        spec.getDisplayName()));
                    }
                    if (module.getFleetMember() != null) {
                        ShipMasterySModRecord record = new ShipMasterySModRecord(module.getFleetMember());
                        record.getSMods().add(rowData.hullModSpecId);
                        record.setSPSpent(masteryPanel.isUsingSP() ? 1 : 0);
                        record.setMPSpent(0);
                        record.setBonusXPFractionGained(bonusXPFraction);
                        record.setCreditsSpent(rowData.creditsCost);
                        PlaythroughLog.getInstance().getSModsInstalled().add(record);
                    }

                    if (isEnhance) {
                        variant.getSModdedBuiltIns().add(rowData.hullModSpecId);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.MasteryPanel.enhanceConfirm + name, Settings.MASTERY_COLOR);
                    }
                    else {
//                        if (module.getFleetMember() != null && cur >= limit && !logisticDontTrack) {
//                            DeferredActionPlugin.performLater(() -> SModsOverCapacity.trackOverCapacityMod(module.getFleetMember(), cur - limit), 0f);
//                        }
                        variant.addPermaMod(rowData.hullModSpecId, true);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.MasteryPanel.builtInConfirm + name, masteryPanel.isUsingSP() ? Misc.getStoryBrightColor() : Settings.MASTERY_COLOR);
                    }

                    if (masteryPanel.isUsingSP()) {
                        Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);
                    } else {
                        Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);
                    }

                    // If engineering override is installed, it becomes permanent
                    if (variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE) && !variant.getPermaMods().contains(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
                        variant.addPermaMod(Strings.Hullmods.ENGINEERING_OVERRIDE, false);
                    }

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
