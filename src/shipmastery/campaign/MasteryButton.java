package shipmastery.campaign;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import lunalib.lunaRefit.BaseRefitButton;
import shipmastery.Settings;

import java.awt.*;

public class MasteryButton extends BaseRefitButton {

    ButtonAPI upgradeButton;

    @Override
    public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) {
        return "Show Mastery";
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return "graphics/hullmods/integrated_targeting_unit.png";
    }

    @Override
    public int getOrder(FleetMemberAPI member, ShipVariantAPI variant) {
        return 0;
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return true;
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {

        float width = getPanelWidth(member, variant);
        float height = getPanelHeight(member, variant);

        TooltipMakerAPI element = backgroundPanel.createUIElement(width, height, false);
        backgroundPanel.addUIElement(element);
        element.getPosition().inTL(0f, 0f);

        ButtonAPI upgradeButton = element.addButton("Increase Mastery", null, Color.WHITE, Settings.masteryColor, 120f, 20f, 0f);
        this.upgradeButton = upgradeButton;
        upgradeButton.getPosition().inTL(20f, 20f);
    }

    @Override
    public void advance(FleetMemberAPI member, ShipVariantAPI variant, Float amount, MarketAPI market) {
        if (upgradeButton != null) {
            System.out.println(upgradeButton.isChecked() + ", " + upgradeButton.isEnabled() + ", " + upgradeButton.isHighlighted());
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
        return true;
    }
}
