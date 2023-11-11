package shipmastery.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.Settings;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;

import java.util.List;

public class SModTableRowPressed extends ProxyTrigger {

    MasteryButtonPressed parentListener;
    long time = System.currentTimeMillis();

    public SModTableRowPressed(MasteryButtonPressed parentListener) {
        super(ClassRefs.uiTableDelegateClass, ClassRefs.uiTableDelegateMethodName);
        this.parentListener = parentListener;
    }

    // arg0: table; arg1: row; arg2: event into
    @Override
    public void trigger(Object... args) {
        if (args.length != 3) return;

        long newTime = System.currentTimeMillis();

        Object row = args[1];
        MasteryButtonPressed.TableRowData rowData = (MasteryButtonPressed.TableRowData) ReflectionUtils.invokeMethod(row, "getData");

        ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
        if (rowData.cantBuildInReason == null) {
            if (!button.isHighlighted()) {
                exclusiveHighlight(args[0], row);
            }
            else if (newTime - time > (int) (Settings.doubleClickInterval * 1000f)) {
                button.unhighlight();
            } else {
                ShipVariantAPI variant = parentListener.getShip().getVariant();
                if (variant != null) {
                    List<String> builtIns = variant.getHullSpec().getBuiltInMods();
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(rowData.hullModSpecId);
                    String name = spec.getDisplayName();
                    if (builtIns.contains(rowData.hullModSpecId)) {
                        variant.getSModdedBuiltIns().add(rowData.hullModSpecId);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Enhanced " + name, Misc.getStoryBrightColor());
                    }
                    else {
                        variant.addPermaMod(rowData.hullModSpecId, true);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Built in " + name, Misc.getStoryBrightColor());
                    }

                    Settings.spendMasteryPoints(variant.getHullSpec(), rowData.mpCost);
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(rowData.creditsCost);
                    parentListener.forceRefresh();
                }
                button.unhighlight();
            }
        }
        else {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(rowData.cantBuildInReason, Misc.getNegativeHighlightColor());
        }

        time = newTime;
    }

    void exclusiveHighlight(Object table, Object rowToHighlight) {
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        for (Object row : rows) {
            ButtonAPI button = (ButtonAPI) ReflectionUtils.invokeMethod(row, "getButton");
            if (row == rowToHighlight) {
                button.highlight();
            }
            else {
                button.unhighlight();
            }
        }
    }
}
