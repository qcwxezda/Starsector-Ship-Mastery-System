package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.ui.CustomUIElement;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public abstract class ButtonWithIcon implements CustomUIElement {

    protected final String spriteName;
    public final float width, height;

    protected final Color baseColor;
    protected final Color brightColor;
    protected final Color darkColor;
    protected final Color darkHighlightColor = Utils.mixColor(Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getGrayColor(), 0.4f);
    public boolean isCheckbox = false;

    protected UIComponentAPI image;
    protected ButtonAPI button;
    protected String disabledReason;
    protected TooltipMakerAPI tooltip;
    protected final boolean useStoryColors;
    private final List<Action> onFinish = new ArrayList<>();

    public ButtonWithIcon(String spriteName, boolean useStoryColors) {
        this(spriteName, 32f, 32f, useStoryColors);
    }

    public ButtonWithIcon(String spriteName, float width, float height, boolean useStoryColors) {
        this.spriteName = spriteName;
        this.width = width;
        this.height = height;
        this.useStoryColors = useStoryColors;
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

    public void setChecked(boolean isChecked) {
        if (button != null) {
            button.setChecked(isChecked);
        }
    }

    public boolean isEnabled() {
        return button != null && button.isEnabled();
    }

    public final void onFinish(Action action) {
        onFinish.add(action);
    }

    protected final void finish() {
        onFinish.forEach(Action::perform);
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        button = tooltip.addAreaCheckbox("", null, baseColor, darkColor, brightColor, width, height, 0f);
        tooltip.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {return false;}

            @Override
            public float getTooltipWidth(Object tooltipParam) {return 500f;}

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                String title = getTooltipTitle();
                if (title != null) {
                    tooltip.setTitleFont(Fonts.ORBITRON_20AA);
                    if (useStoryColors) {
                        tooltip.setTitleFontColor(Misc.getStoryBrightColor());
                    }
                    tooltip.addTitle(title);
                }
                appendToTooltip(tooltip);
                showDisabledReasonIfDisabled(tooltip);
            }
        }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
        ReflectionUtils.setButtonListener(button, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                onClick();
                if (!isCheckbox) {
                    button.setChecked(false);
                }
            }
        });
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

    public abstract void onClick();
    public abstract String getTooltipTitle();
    public abstract void appendToTooltip(TooltipMakerAPI tooltip);

    public void showDisabledReasonIfDisabled(TooltipMakerAPI tooltip) {
        if (disabledReason != null && !button.isEnabled()) {
            tooltip.addPara(disabledReason, Misc.getNegativeHighlightColor(), 10f);
        }
    }
}
