package shipmastery.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.function.Consumer;

public class ButtonWithIcon implements CustomUIElement {

    private final String spriteName;
    private final float width, height;

    private final Color baseColor;
    private final Color brightColor;
    private final Color darkColor;
    public boolean isCheckbox = false;

    private UIComponentAPI image;
    private ButtonAPI button;
    private String disabledReason;
    private TooltipMakerAPI tooltip;

    public ButtonWithIcon(String spriteName, float width, float height, boolean useStoryColors) {
        this.spriteName = spriteName;
        this.width = width;
        this.height = height;
        if (!useStoryColors) {
            darkColor = new Color(39, 125, 143);
            baseColor = Misc.getBasePlayerColor();
            brightColor = Misc.getBrightPlayerColor();
        } else {
            darkColor = Utils.mixColor(Misc.getStoryDarkColor(), Misc.getStoryOptionColor(), 0.5f);
            baseColor = Misc.getStoryOptionColor();
            brightColor = Misc.getStoryBrightColor();
        }
    }

    public void onClick(Action onClick) {
        if (onClick != null) {
            ReflectionUtils.setButtonListener(button, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    onClick.perform();
                    if (!isCheckbox) {
                        button.setChecked(false);
                    }
                }
            });
        }
    }

    public void setButtonTooltip(Consumer<TooltipMakerAPI> createButtonTooltip) {
        if (createButtonTooltip != null && tooltip != null) {
            tooltip.addTooltipTo(new TooltipMakerAPI.TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) {
                    return false;
                }

                @Override
                public float getTooltipWidth(Object tooltipParam) {
                    return 500f;
                }

                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    createButtonTooltip.accept(tooltip);
                }
            }, button, TooltipMakerAPI.TooltipLocation.ABOVE);
        }
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        button = tooltip.addAreaCheckbox("", null, baseColor, darkColor, brightColor, width, height, 0f);
        var panel = ReflectionUtils.invokeMethod(button, "getAreaCheckboxButtonPanel");
        ReflectionUtils.invokeMethod(panel, "setBorderThickness", 2f);
        tooltip.addImage(spriteName, width, height, -height);
        image = tooltip.getPrev();
        this.tooltip = tooltip;
    }

    public void setEnabled(boolean enabled, @Nullable String disabledReason) {
        if (enabled) {
            button.setEnabled(true);
            image.setOpacity(1f);
        } else {
            button.setEnabled(false);
            image.setOpacity(0.3f);
            this.disabledReason = disabledReason;
        }
    }

    void showDisabledReasonIfDisabled(TooltipMakerAPI tooltip) {
        if (disabledReason != null && !button.isEnabled()) {
            tooltip.addPara(disabledReason, Misc.getNegativeHighlightColor(), 10f);
        }
    }

    void makeUpgradeTooltip(TooltipMakerAPI tooltip) {
        tooltip.setTitleFont(Fonts.ORBITRON_20AA);
        tooltip.addTitle("Level Up");
        tooltip.addPara("Advance your mastery of this ship class and select a perk to acquire.", 10f);
        showDisabledReasonIfDisabled(tooltip);
    }

    void makeRerollTooltip(TooltipMakerAPI tooltip) {
        tooltip.setTitleFont(Fonts.ORBITRON_20AA);
        tooltip.setTitleFontColor(Misc.getStoryBrightColor());
        tooltip.addTitle("Refresh Masteries");
        tooltip.addPara("Generate new mastery perks for each level with randomized perks. Only levels for which you do not have an active perk are affected.\n\nThe resulting changes affect NPC fleets as well as your own.\n\nClick for more info.", 10f);
        showDisabledReasonIfDisabled(tooltip);
    }

    void makeConstructTooltip(TooltipMakerAPI tooltip) {
        tooltip.setTitleFont(Fonts.ORBITRON_20AA);
        tooltip.addTitle("Mastery Sharing");
        String active = button.isChecked() ? "active" : "inactive";
        Color hc = Settings.POSITIVE_HIGHLIGHT_COLOR;
        tooltip.addPara("Reduce XP gained by this ship class by half. Whenever this ship class gains 200 XP, generate a blank knowledge construct worth 50 XP and send it directly to your cargo holds.\n\nBlank knowledge constructs are compatible with every class of ship.\n\nCurrently %s. Click to toggle.", 10f, hc, active);
        showDisabledReasonIfDisabled(tooltip);
    }

    void makeConfirmTooltip(TooltipMakerAPI tooltip) {
        tooltip.setTitleFont(Fonts.ORBITRON_20AA);
        tooltip.addTitle("Confirm Changes");
        tooltip.addPara("Confirm the pending changes to your active mastery perks.", 10f);
        showDisabledReasonIfDisabled(tooltip);
    }

    void makeCancelTooltip(TooltipMakerAPI tooltip) {
        tooltip.setTitleFont(Fonts.ORBITRON_20AA);
        tooltip.addTitle("Cancel Changes");
        tooltip.addPara("Cancel the pending changes to your active mastery perks.", 10f);
        showDisabledReasonIfDisabled(tooltip);
    }
}
