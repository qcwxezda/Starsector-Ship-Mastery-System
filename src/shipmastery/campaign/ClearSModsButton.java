package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import lunalib.lunaRefit.BaseRefitButton;
import shipmastery.Settings;
import shipmastery.util.Utils;

public class ClearSModsButton extends BaseRefitButton {
    boolean isConfirming = false;
    int confirmMouseX = 0, confirmMouseY = 0;

    @Override
    public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) {
        return isConfirming ? "Click again to confirm" : "Clear S-Mods (1 MP)";
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return "graphics/hullmods/integrated_targeting_unit.png";
    }

    @Override
    public int getOrder(FleetMemberAPI member, ShipVariantAPI variant) {
        return 1;
    }

    @Override
    public void onClick(FleetMemberAPI member, ShipVariantAPI variant, InputEventAPI event, MarketAPI market) {
        super.onClick(member, variant, event, market);

        if (!isConfirming) {
            isConfirming = true;
            confirmMouseX = Global.getSettings().getMouseX();
            confirmMouseY = Global.getSettings().getMouseY();
        } else {
            isConfirming = false;

            for (String mod : variant.getSMods()) {
                variant.removePermaMod(mod);
            }
            refreshVariant();
        }

        refreshButtonList();
    }

    @Override
    public void advance(FleetMemberAPI member, ShipVariantAPI variant, Float amount, MarketAPI market) {
        if (isConfirming) {
            if (Utils.dist(confirmMouseX, confirmMouseY, Global.getSettings().getMouseX(), Global.getSettings().getMouseY()) > 100f) {
                isConfirming = false;
                refreshButtonList();
            }
        }
    }

    @Override
    public boolean hasTooltip(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return true;
    }

    @Override
    public void addTooltip(TooltipMakerAPI tooltip, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        tooltip.addPara("Removes all S-Mods from this ship. Doesn't affect enhanced built-ins. Costs 1 Mastery Point.", 0f, Settings.masteryColor, "1 Mastery Point");
    }
    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return variant.getSMods().size() > 0;
    }

}
