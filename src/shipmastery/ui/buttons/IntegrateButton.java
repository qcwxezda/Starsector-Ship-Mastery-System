package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import shipmastery.hullmods.aicoreinterface.AICoreInterfacePlugin;
import shipmastery.config.Settings;
import shipmastery.ui.triggers.DialogDismissedListener;
import shipmastery.util.CampaignUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class IntegrateButton extends ButtonWithCost {

    private final List<ButtonAPI> coreButtons = new ArrayList<>();
    private String selectedCoreId;
    private final FleetMemberAPI member;
    private final ShipVariantAPI selectedVariant;
    private String existingIntegrated;
    private String cannotIntegrateOrRemoveReason = null;
    private LabelAPI cannotPerformLabel;

    public IntegrateButton(boolean useSP, FleetMemberAPI member, ShipVariantAPI selectedVariant) {
        super(useSP ? "graphics/icons/ui/sms_integrate_icon_green.png" : "graphics/icons/ui/sms_integrate_icon.png", useSP);
        this.member = member;
        this.selectedVariant = selectedVariant;
    }

    @Override
    protected String getCostDescriptionFormat() {
        return existingIntegrated != null ? Strings.MasteryPanel.aiInterfacePanelRemoveText : Strings.MasteryPanel.aiInterfacePanelText;
    }

    @Override
    protected String getUsedSPDescription() {
        if (existingIntegrated == null) {
            return String.format(Strings.MasteryPanel.aiInterfacePanelUsedSPText,
                    selectedCoreId == null ? "?" : Global.getSettings().getCommoditySpec(selectedCoreId).getName(),
                    member.getShipName(),
                    member.getHullSpec().getNameWithDesignationWithDashClass());
        }
        return String.format(Strings.MasteryPanel.aiInterfacePanelRemoveUsedSPText,
                Global.getSettings().getCommoditySpec(existingIntegrated).getName(),
                member.getShipName(),
                member.getHullSpec().getNameWithDesignationWithDashClass());
    }

    @Override
    protected boolean canApplyEffects() {
        return cannotIntegrateOrRemoveReason == null && (selectedCoreId != null || existingIntegrated != null);
    }

    @Override
    protected String getUsedSPSound() {
        return "ui_char_spent_story_point_technology";
    }

    @Override
    protected String getNormalSound() {
        return existingIntegrated != null ? "ui_char_reset" : "ui_neural_transfer_complete";
    }

    @Override
    protected String[] getCostDescriptionArgs() {
        String toShow = existingIntegrated != null ? existingIntegrated : selectedCoreId;
        return new String[] {
                toShow == null ? "" : Global.getSettings().getCommoditySpec(toShow).getName(),
                Misc.getDGSCredits(getModifiedCost())};
    }
    @Override
    protected float getBaseCost() {
        String toShow = existingIntegrated != null ? existingIntegrated : selectedCoreId;
        if (toShow == null) return 0f;
        var plugin = Global.getSettings().getHullModSpec(toShow + AICoreInterfacePlugin.INTEGRATED_SUFFIX).getEffect();
        if (!(plugin instanceof AICoreInterfacePlugin p)) return 0f;
        var cost = p.getIntegrationCost(member);
        return existingIntegrated != null ? cost / 2f : cost;
    }

    public class IntegratePanelPlugin extends BaseCustomUIPanelPlugin {

        private float x = 0f, y = 0f, w = 0f, h = 0f;

        @Override
        public void positionChanged(PositionAPI position) {
            x = position.getX();
            y = position.getY();
            w = position.getWidth();
            h = position.getHeight();
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (h < 300f) return;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);
            var color = Utils.mixColor(darkColor, Color.BLACK, 0.8f);
            GL11.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
            GL11.glVertex2f(x+20f, y+150f);
            GL11.glVertex2f(x+w-20f, y+150f);
            GL11.glVertex2f(x+w-20f, y+h-50f);
            GL11.glVertex2f(x+20f, y+h-50f);
            GL11.glEnd();
        }

        @Override
        public void buttonPressed(Object buttonId) {
            for (var button : coreButtons) {
                button.setChecked(Objects.equals(buttonId, button.getCustomData()));
                if (button.isChecked() && confirmButton != null) {
                    selectedCoreId = (String) buttonId;
                }
            }
            updateLabels();
        }
    }

    @Override
    public void onClick() {
        selectedCoreId = null;
        cannotIntegrateOrRemoveReason = null;
        coreButtons.clear();

        NavigableMap<CommoditySpecAPI, Integer> counts;
        if (existingIntegrated == null) {
            counts = CampaignUtils.getPlayerCommodityCounts(x -> Global.getSettings().getHullModSpec(x.getId() + AICoreInterfacePlugin.INTEGRATED_SUFFIX) != null);
        } else {
            counts = new TreeMap<>((x, y) -> 0);
            counts.put(Global.getSettings().getCommoditySpec(existingIntegrated), 1);
        }

        float width = counts.isEmpty() ? 400f : 600f, height = counts.isEmpty() ? 200f : 600f;

        var dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                width,
                height,
                new DialogDismissedListener() {
                    @Override
                    public void trigger(Object... args) {
                        if ((int) args[1] == 1 || !confirmButton.isEnabled()) return;
                        if (existingIntegrated != null) {
                            selectedVariant.removePermaMod(existingIntegrated + AICoreInterfacePlugin.INTEGRATED_SUFFIX);
                        } else if (selectedCoreId != null) {
                            selectedVariant.addPermaMod(selectedCoreId + AICoreInterfacePlugin.INTEGRATED_SUFFIX, false);
                        }
                        applyCosts();
                        finish();
                    }
                });

        if (dialogData == null) return;

        confirmButton = dialogData.confirmButton;
        confirmButton.setEnabled(false);
        if (isStoryOption) {
            ReflectionUtils.setButtonColor(confirmButton, Misc.getStoryDarkColor());
            ReflectionUtils.setButtonTextColor(confirmButton, Misc.getStoryOptionColor());
        }

        var plugin = new IntegratePanelPlugin();
        var panel = Global.getSettings().createCustom(width, height, plugin);
        var title = panel.createUIElement(width, 30f, false);
        title.setTitleFont(Fonts.ORBITRON_24AA);
        title.setTitleFontColor(isStoryOption ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor());
        title.addTitle(Strings.MasteryPanel.aiInterfaceButton).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(15f);

        if (counts.isEmpty()) {
            var info = panel.createUIElement(width-45f, 50f, false);
            info.addPara(Strings.Items.noneInCargo, Misc.getGrayColor(), 10f).setAlignment(Alignment.MID);
            panel.addUIElement(info).inMid();
        } else {
            var buttonList = panel.createUIElement(width-45f, 400f, true);
            counts.forEach((spec, num) -> addSectionForPseudocore(buttonList, spec, num, width - 50f, 90f));
            panel.addUIElement(buttonList).inTMid(50f).setXAlignOffset(5f);
            ReflectionUtils.invokeMethod(buttonList.getExternalScroller(), "setMaxShadowHeight", 0f);
        }

        TooltipMakerAPI ttm = addCostLabels(panel, width-75f, height-135f);
        cannotPerformLabel = ttm.addPara("", Settings.NEGATIVE_HIGHLIGHT_COLOR, 10f);
        updateLabels();
        dialogData.panel.addComponent(panel).inTMid(0f);
    }

    public void addSectionForPseudocore(TooltipMakerAPI tooltip, CommoditySpecAPI spec, int numInCargo, float width, float minHeight) {
        String id = spec.getId();
        var integrationPlugin = getIntegrationPlugin(id);
        if (integrationPlugin == null) return;

        float leftPad = 100f;
        float textWidth = width - leftPad - 20f;
        CustomPanelAPI tempPanel = Global.getSettings().createCustom(textWidth, 0f, null);
        TooltipMakerAPI tempTTM = tempPanel.createUIElement(textWidth, 0f, false);
        float h = tempTTM.getHeightSoFar();
        integrationPlugin.addIntegrationDescriptionToTooltip(tempTTM);
        float diff = tempTTM.getHeightSoFar() - h;
        float height = Math.max(minHeight, diff + 18f);

        Color darkerColor = Utils.mixColor(darkColor, Color.BLACK, 0.4f);

        tooltip.beginTable(baseColor, darkerColor, brightColor, height, spec.getName(), width-10f);
        tooltip.addRow(baseColor, "");
        tooltip.addTable("", 0, 0f);

        var button = tooltip.addAreaCheckbox("", id, darkerColor, darkerColor, brightColor, width-10f, height+8f, -height-4f);
        if (existingIntegrated != null) {
            button.setClickable(false);
            button.setChecked(true);
            updateLabels();
        }

        coreButtons.add(button);
        tooltip.addSpacer(-height+5f).getPosition().setXAlignOffset(leftPad);
        tooltip.setTextWidthOverride(textWidth);
        integrationPlugin.addIntegrationDescriptionToTooltip(tooltip);
//        tooltip.addPara(text, 0f);
        tooltip.addSpacer(Math.max(-5f, height-diff-5f)).getPosition().setXAlignOffset(-leftPad);

        tooltip.addImage(spec.getIconName(), 64f, 64f, -height/2f - 32f);
        tooltip.getPrev().getPosition().setXAlignOffset(20f);
        if (existingIntegrated == null) {
            tooltip.setParaInsigniaVeryLarge();
            tooltip.addPara("" + numInCargo, Misc.getGrayColor(), -82f);
            tooltip.getPrev().getPosition().setXAlignOffset(-13f);
            tooltip.setParaFontDefault();
        }
        tooltip.addSpacer(height/2f+52f);
        tooltip.getPrev().getPosition().setXAlignOffset(-7f);


        //ButtonAPI outline = tooltip.addAreaCheckbox("", id, baseColor, darkColor, brightColor, width, 200f, 10f);
        //ButtonAPI name = tooltip.addAreaCheckbox(spec.getName(), null, baseColor, darkColor, brightColor, width, 30f, -200f);
        //tooltip.addImage(spec.getIconName(), 32f, 32f, -50f);


    }

    @Override
    protected void updateLabels() {
        if (cannotPerformLabel != null) {
            String currentId = existingIntegrated != null ? existingIntegrated : selectedCoreId;
            var plugin = getIntegrationPlugin(currentId);
            if (plugin != null) {
                if (existingIntegrated != null) {
                    cannotIntegrateOrRemoveReason = plugin.getCannotRemoveReason(member);
                    cannotPerformLabel.setText(cannotIntegrateOrRemoveReason == null ? "" : Strings.MasteryPanel.aiInterfacePanelCannotRemove + plugin.getCannotRemoveReason(member));
                } else {
                    cannotIntegrateOrRemoveReason = plugin.getCannotIntegrateReason(member);
                    cannotPerformLabel.setText(cannotIntegrateOrRemoveReason == null ? "" : Strings.MasteryPanel.aiInterfacePanelCannotIntegrate + plugin.getCannotIntegrateReason(member));
                }
            }
        }
        super.updateLabels();

    }

    static AICoreInterfacePlugin getIntegrationPlugin(String coreId) {
        if (coreId == null) return null;
        var plugin = Global.getSettings().getHullModSpec(coreId + AICoreInterfacePlugin.INTEGRATED_SUFFIX).getEffect();
        if (!(plugin instanceof AICoreInterfacePlugin integrationPlugin)) return null;
        return integrationPlugin;
    }

    @Override
    protected boolean shouldShowCostLabelNow() {
        return selectedCoreId != null || existingIntegrated != null;
    }

    @Override
    protected boolean shouldShowSPCostLabelNow() {
        return shouldShowCostLabelNow();
    }

    @Override
    public void afterCreate() {
        existingIntegrated = AICoreInterfacePlugin.getIntegratedPseudocore(selectedVariant);
        thisButton.setChecked(existingIntegrated != null);
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.aiInterfaceButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        if (existingIntegrated == null) {
            tooltip.addPara(Strings.MasteryPanel.aiInterfaceTooltip, 10f);
        } else {
            AICoreInterfacePlugin.addIntegratedDescToTooltip(tooltip, existingIntegrated, 10f);
        }
    }
}
