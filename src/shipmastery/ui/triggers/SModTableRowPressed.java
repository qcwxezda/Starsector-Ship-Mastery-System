package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.Action;
import shipmastery.campaign.DeferredActionPlugin;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;

import java.util.List;

public class SModTableRowPressed extends TriggerableProxy {

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

                ShipAPI ship = masteryPanel.getShip();
                if (ship.getVariant().getSMods().size() >= SModUtils.getMaxSMods(ship.getMutableStats())) {
                    Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.BUILD_IN_OVER_MAX_WARNING, Misc.getNegativeHighlightColor());
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
                ShipVariantAPI variant = masteryPanel.getShip().getVariant();
                if (variant != null) {
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(rowData.hullModSpecId);
                    String name = spec.getDisplayName();
                    if (SModUtils.isHullmodBuiltIn(spec, variant)) {
                        variant.getSModdedBuiltIns().add(rowData.hullModSpecId);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.ENHANCE_STR + name, Misc.getStoryBrightColor());
                    }
                    else {
                        variant.addPermaMod(rowData.hullModSpecId, true);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.BUILD_IN_STR + name, Misc.getStoryBrightColor());
                    }
                    Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);

                    ShipMastery.spendMasteryPoints(variant.getHullSpec(), rowData.mpCost);
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(rowData.creditsCost);
                    masteryPanel.forceRefresh(true, true);
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
