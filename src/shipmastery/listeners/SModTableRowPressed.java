package shipmastery.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.Settings;
import shipmastery.campaign.Action;
import shipmastery.campaign.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Utils;

import java.util.List;

public class SModTableRowPressed extends ProxyTrigger {

    static final String ENHANCE_STR = Utils.getString("sms_masteryPanel", "enhanceConfirm");
    static final String BUILD_IN_STR = Utils.getString("sms_masteryPanel", "builtInConfirm");

    MasteryPanel masteryPanel;
    long lastClickTime = 0;

    public SModTableRowPressed(MasteryPanel masteryPanel) {
        super(ClassRefs.uiTableDelegateClass, ClassRefs.uiTableDelegateMethodName);
        this.masteryPanel = masteryPanel;
    }

    // arg0: table; arg1: row; arg2: event into
    @Override
    public void trigger(Object... args) {
        if (args.length != 3) return;

        Object row = args[1];
        MasteryPanel.TableRowData rowData = (MasteryPanel.TableRowData) ReflectionUtils.invokeMethod(row, "getData");

        final ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
        if (rowData.cantBuildInReason == null) {
            long newTime = System.currentTimeMillis();
            if (!button.isHighlighted()) {
                exclusiveHighlight(args[0], row);

                DeferredActionPlugin.performLater(new Action() {
                    @Override
                    public void perform() {
                        button.unhighlight();
                    }
                }, Settings.doubleClickInterval);

            }
            else if (newTime - lastClickTime > (int) (Settings.doubleClickInterval * 1000f)) {
                button.unhighlight();
            } else {
                ShipVariantAPI variant = masteryPanel.getShip().getVariant();
                if (variant != null) {
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(rowData.hullModSpecId);
                    String name = spec.getDisplayName();
                    if (SModUtils.isHullmodBuiltIn(spec, variant)) {
                        variant.getSModdedBuiltIns().add(rowData.hullModSpecId);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(ENHANCE_STR + name, Misc.getStoryBrightColor());
                    }
                    else {
                        variant.addPermaMod(rowData.hullModSpecId, true);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(BUILD_IN_STR + name, Misc.getStoryBrightColor());
                    }
                    Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);

                    Settings.spendMasteryPoints(variant.getHullSpec(), rowData.mpCost);
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(rowData.creditsCost);
                    masteryPanel.forceRefresh(true);
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
