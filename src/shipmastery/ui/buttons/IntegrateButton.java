package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.BaseKCorePlugin;
import shipmastery.campaign.items.PseudocoreIntegrationPlugin;
import shipmastery.util.CampaignUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class IntegrateButton extends ButtonWithIcon {

    public IntegrateButton(boolean useSP) {
        super(useSP ? "graphics/icons/ui/sms_integrate_icon_green.png" : "graphics/icons/ui/sms_integrate_icon.png", useSP);
    }

    @Override
    public void onClick() {

        float width = 600f, height = 600f;

        var dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                width,
                height,
                null);

        if (dialogData == null) return;

        var confirmButton = dialogData.confirmButton;
        confirmButton.setEnabled(false);
        if (isStoryOption) {
            ReflectionUtils.setButtonColor(confirmButton, Misc.getStoryDarkColor());
            ReflectionUtils.setButtonTextColor(confirmButton, Misc.getStoryOptionColor());
        }

        var panel = Global.getSettings().createCustom(width, height, null);
        var title = panel.createUIElement(width, 30f, false);
        title.setTitleFont(Fonts.ORBITRON_24AA);
        title.setTitleFontColor(isStoryOption ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor());
        title.addTitle(Strings.MasteryPanel.integrationButton).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(15f);

        var buttonList = panel.createUIElement(width-45f, 500f, true);
        var counts = CampaignUtils.getPlayerCommodityCounts(x -> x.hasTag(BaseKCorePlugin.IS_K_CORE_TAG));

        counts.forEach((spec, num) -> addSectionForPseudocore(buttonList, spec, num,width-50f, 100f));
        panel.addUIElement(buttonList).inTMid(50f).setXAlignOffset(5f);
        dialogData.panel.addComponent(panel).inTMid(0f);
    }

    public void addSectionForPseudocore(TooltipMakerAPI tooltip, CommoditySpecAPI spec, int numInCargo, float width, float minHeight) {

        String id = spec.getId();
        var plugin = Misc.getAICoreOfficerPlugin(id);
        if (!(plugin instanceof PseudocoreIntegrationPlugin)) plugin = new BaseKCorePlugin();
        PseudocoreIntegrationPlugin integrationPlugin = (PseudocoreIntegrationPlugin) plugin;

        float leftPad = 100f;
        float textWidth = width - leftPad;
        CustomPanelAPI tempPanel = Global.getSettings().createCustom(textWidth, 0f, null);
        TooltipMakerAPI tempTTM = tempPanel.createUIElement(textWidth, 0f, false);
        float h = tempTTM.getHeightSoFar();
        integrationPlugin.addDescriptionToTooltip(tempTTM);
        float diff = tempTTM.getHeightSoFar() - h;
        float height = Math.max(minHeight, diff + 10f);

        tooltip.beginTable(baseColor, darkColor, brightColor, height, spec.getName(), width-10f);
        tooltip.addRow(baseColor, "");
        tooltip.addTable("", 0, 0f);

        tooltip.addAreaCheckbox("", null, baseColor, darkColor, brightColor, width-10f, height+8f, -height-4f);
        tooltip.addSpacer(-height).getPosition().setXAlignOffset(leftPad);
        tooltip.setTextWidthOverride(width - leftPad);
        integrationPlugin.addDescriptionToTooltip(tooltip);
//        tooltip.addPara(text, 0f);
        tooltip.addSpacer(Math.max(0f, height - diff)).getPosition().setXAlignOffset(-leftPad);

        tooltip.addImage(spec.getIconName(), 64f, 64f, -height/2f - 32f);
        tooltip.getPrev().getPosition().setXAlignOffset(20f);
        tooltip.setParaInsigniaVeryLarge();
        tooltip.addPara("" + numInCargo, Misc.getGrayColor(), -82f);
        tooltip.getPrev().getPosition().setXAlignOffset(-13f);
        tooltip.setParaFontDefault();
        tooltip.addSpacer(height/2f+52f);
        tooltip.getPrev().getPosition().setXAlignOffset(-7f);


        //ButtonAPI outline = tooltip.addAreaCheckbox("", id, baseColor, darkColor, brightColor, width, 200f, 10f);
        //ButtonAPI name = tooltip.addAreaCheckbox(spec.getName(), null, baseColor, darkColor, brightColor, width, 30f, -200f);
        //tooltip.addImage(spec.getIconName(), 32f, 32f, -50f);


    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.integrationButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.integrationTooltip, 10f);
    }
}
