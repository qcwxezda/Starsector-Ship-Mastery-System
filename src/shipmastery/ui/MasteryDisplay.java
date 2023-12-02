package shipmastery.ui;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.deferred.Action;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.ui.triggers.MasteryEffectButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.*;

public class MasteryDisplay implements CustomUIElement {

    final float w, h;
    final NavigableMap<Integer, Boolean> activeLevels;
    final NavigableMap<Integer, Boolean> selectedLevels;
    final ShipAPI selectedModule, rootShip;
    final ShipHullSpecAPI rootSpec;
    final FleetMemberAPI rootFleetMember;
    final Action onButtonClick;
    final float paddingBetweenLevels;
    float totalHeight, scrollToHeight;
    final boolean resetScrollbar;
    float savedScrollerHeight = 0f;
    TooltipMakerAPI savedTooltip;

    static final float MIN_DESC_HEIGHT = 80f;
    static final float BUTTON_WIDTH = 35f;

    Map<Integer, ButtonAPI> option1Buttons = new HashMap<>();
    Map<Integer, ButtonAPI> option2Buttons = new HashMap<>();

    public MasteryDisplay(ShipAPI selectedModule, ShipAPI rootShip, float width, float height, float pad, boolean resetScrollbar, Action onButtonClick) {
        w = width;
        h = height;
        this.selectedModule = selectedModule;
        this.rootShip = rootShip;
        rootSpec = Utils.getRestoredHullSpec(rootShip.getHullSpec());
        rootFleetMember = rootShip.getFleetMember();
        this.onButtonClick = onButtonClick;
        activeLevels = ShipMastery.getPlayerActiveMasteriesCopy(rootSpec);
        selectedLevels = new TreeMap<>(activeLevels);
        this.resetScrollbar = resetScrollbar;
        paddingBetweenLevels = pad;
    }

    public float getTotalHeight() {
        return totalHeight;
    }

    public float getScrollToHeight() {
        return scrollToHeight;
    }

    public float getSavedScrollerHeight() {
        return savedScrollerHeight;
    }

    public void saveScrollerHeight() {
        savedScrollerHeight = savedTooltip == null ? 0f : savedTooltip.getExternalScroller().getYOffset();
    }

    public void scrollToHeight(float height) {
        if (savedTooltip != null) {
            savedTooltip.getExternalScroller().setYOffset(height);
        }
    }

    public void selectMasteryItem(int level, boolean isOption2) {
        selectedLevels.put(level, isOption2);
        // If the other button is checked, need to uncheck it
        if (isOption2 && option1Buttons.containsKey(level)) {
            ButtonAPI button = option1Buttons.get(level);
            button.setChecked(false);
            button.setHighlightBrightness(0.25f);
        }
        else if (option2Buttons.containsKey(level)) {
            ButtonAPI button = option2Buttons.get(level);
            button.setChecked(false);
            button.setHighlightBrightness(0.25f);
        }
        onButtonClick.perform();
    }

    public void deselectMasteryItem(int level) {
        selectedLevels.remove(level);
        onButtonClick.perform();
    }

    public NavigableMap<Integer, Boolean> getSelectedLevels() {
        return selectedLevels;
    }

    public NavigableMap<Integer, Boolean> getActiveLevels() {
        return activeLevels;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        int maxMastery = ShipMastery.getMaxMasteryLevel(rootSpec);

        for (int i = 1; i <= maxMastery; i++) {
            CustomPanelAPI descriptionPanel1 = Global.getSettings().createCustom(w + 50f, MIN_DESC_HEIGHT, null);
            TooltipMakerAPI description1 = descriptionPanel1.createUIElement(w - 30f, MIN_DESC_HEIGHT, false);
            List<MasteryEffect> effects1 = ShipMastery.getMasteryEffects(rootSpec, i, false);
            List<MasteryEffect> effects2 = ShipMastery.getMasteryEffects(rootSpec, i, true);
            float height = addEffectsDisplay(effects1, i, false, descriptionPanel1, description1, !effects2.isEmpty());
            tooltip.addComponent(descriptionPanel1).inTL(0f, totalHeight);
            if (!effects2.isEmpty()) {
                CustomPanelAPI descriptionPanel2 = Global.getSettings().createCustom(w + 50f, MIN_DESC_HEIGHT, null);
                TooltipMakerAPI description2 = descriptionPanel2.createUIElement(w - 30f, MIN_DESC_HEIGHT, false);
                tooltip.addComponent(descriptionPanel2).inTL(0f, totalHeight + height - 2f);
                height += addEffectsDisplay(effects2, i, true, descriptionPanel2, description2, true);
            }
            totalHeight += paddingBetweenLevels + height;
        }
        savedTooltip = tooltip;
    }

    void addMasteryDescriptions(List<MasteryEffect> effects, TooltipMakerAPI tooltip) {
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            MasteryDescription effectDescription = effect.getDescription(selectedModule, rootFleetMember);
            if (effectDescription != null) {
                if (effect.hasTag(MasteryTags.PREFIX_FLAGSHIP_ONLY)) {
                    effectDescription.addLabelWithPrefix(tooltip, Strings.FLAGSHIP_ONLY, Misc.getBasePlayerColor());
                } else {
                    effectDescription.addLabel(tooltip);
                }
            }
            tooltip.setParaFontDefault();
            tooltip.addSpacer(5f);
            effect.addPostDescriptionSection(tooltip, selectedModule, rootFleetMember);
            if (!rootFleetMember.equals(selectedModule.getFleetMember()) && effect.hasTag(
                    MasteryTags.DOESNT_AFFECT_MODULES)) {
                tooltip.addPara(Strings.DOESNT_AFFECT_MODULES, Misc.getNegativeHighlightColor(), 5f);
            }
            tooltip.addSpacer(i == effects.size() - 1 ? 20f : 5f);
        }
    }

    /** Returns the final height of the description. */
    float addEffectsDisplay(final List<MasteryEffect> effects, int level, boolean isOption2, CustomPanelAPI innerPanel, TooltipMakerAPI innerTooltip, boolean showOptionLetter) {
        int currentMastery = ShipMastery.getPlayerMasteryLevel(rootSpec);
        boolean alwaysShow = true;
        for (MasteryEffect effect : effects) {
            if (!MasteryUtils.alwaysShowDescription(effect)) {
                alwaysShow = false;
                break;
            }
        }
        boolean hidden = !alwaysShow && level > currentMastery + 1;

        if (!hidden) {
            innerTooltip.addSpacer(20f);
            addMasteryDescriptions(effects, innerTooltip);
        }
        else {
            innerTooltip.addPara("", 0f);
        }

        float descH = Math.max(MIN_DESC_HEIGHT, innerTooltip.getHeightSoFar());
        innerPanel.getPosition().setSize(w - 15f, descH);

        TooltipMakerAPI descriptionButton = innerPanel.createUIElement(w, descH, false);
        ButtonAPI descOutline = descriptionButton.addAreaCheckbox(hidden ? Strings.UNKNOWN_EFFECT_STR : "", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                                  Misc.getGrayColor(), w - 5f, descH, 0f);
        descOutline.setClickable(level <= currentMastery);
        descOutline.setGlowBrightness(level <= currentMastery ? 0.8f : 0.15f);
        if (level > currentMastery) {
            descOutline.setButtonPressedSound(null);
        }
        if (isOption2) {
            option2Buttons.put(level, descOutline);
        }
        else {
            option1Buttons.put(level, descOutline);
        }
        ReflectionUtils.setButtonListener(descOutline, new MasteryEffectButtonPressed(this, rootSpec, level, isOption2));

        boolean hasTooltip = false;
        for (MasteryEffect effect : effects) {
            if (MasteryUtils.hasTooltip(effect)) {
                hasTooltip = true;
                break;
            }
        }

        if (hasTooltip && !hidden) {
            descriptionButton.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object o) {
                    return false;
                }

                @Override
                public float getTooltipWidth(Object o) {
                    return 500f;
                }

                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    for (MasteryEffect effect : effects) {
                        effect.addTooltipIfHasTooltipTag(tooltip, selectedModule, rootFleetMember);
                    }
                }
            }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
        }

        TooltipMakerAPI levelButtonTTM = innerPanel.createUIElement(BUTTON_WIDTH, descH, false);
        String optionLetter = isOption2 ? "B" : "A";
        ButtonAPI levelButton = levelButtonTTM.addAreaCheckbox("" + level + (showOptionLetter ? optionLetter : ""), null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                               Misc.getBrightPlayerColor(), BUTTON_WIDTH, descH, 0f);
        levelButton.setClickable(false);
        levelButton.setGlowBrightness(0.3f);
        levelButton.setMouseOverSound(null);

        levelButton.highlight();

        if (level <= currentMastery) {
            descOutline.highlight();
            levelButton.setHighlightBrightness(0.5f);
            descOutline.setHighlightBrightness(0.25f);
        }
        else {
            levelButton.setHighlightBrightness(0.25f);
            descOutline.setHighlightBrightness(0f);
            levelButton.setEnabled(false);
        }

        // Instantly set the highlight strength to get rid of unwanted flickering when refreshing the panel
        ((Fader) ReflectionUtils.invokeMethod(descOutline, "getHighlightFader")).forceIn();
        ((Fader) ReflectionUtils.invokeMethod(levelButton, "getHighlightFader")).forceIn();

        if (activeLevels.containsKey(level) && activeLevels.get(level) == isOption2) {
            descOutline.setChecked(true);
            descOutline.setHighlightBrightness(0f);
            levelButton.setChecked(true);
        }

        if (level <= currentMastery) {
            scrollToHeight = totalHeight;
        }

        // Cheap trick: to simulate darken effect, render the outline area checkbox above the text
        innerPanel.addUIElement(levelButtonTTM).inLMid(11f);
        if (level > currentMastery) {
            innerPanel.addUIElement(innerTooltip).inLMid(60f);
            innerPanel.addUIElement(descriptionButton).inLMid(45f);
        } else {
            innerPanel.addUIElement(descriptionButton).inLMid(45f);
            innerPanel.addUIElement(innerTooltip).inLMid(60f);
        }

        return descH;
    }
}
