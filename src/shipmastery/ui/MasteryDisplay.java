package shipmastery.ui;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.ui.triggers.CancelMasteryChangesPressed;
import shipmastery.ui.triggers.ConfirmMasteryChangesPressed;
import shipmastery.ui.triggers.MasteryEffectButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class MasteryDisplay implements CustomUIElement {

    final float w, h;
    final NavigableMap<Integer, String> activeLevels;
    final NavigableMap<Integer, String> selectedLevels;
    final ShipAPI selectedModule, rootShip;
    final ShipHullSpecAPI rootSpec;
    final FleetMemberAPI rootFleetMember;
    final MasteryPanel parentPanel;
    final Action onButtonClick;
    final float paddingBetweenLevels;
    float totalHeight;
    int levelToScrollTo;
    final boolean resetScrollbar;
    float savedScrollerHeight = 0f;
    TooltipMakerAPI savedTooltip;

    static final float MIN_DESC_HEIGHT = 80f;
    static final float BUTTON_WIDTH = 35f;

    final Map<Integer, Map<String, ButtonAPI>> buttonsByLevelAndId = new HashMap<>();
    final List<Float> scrollerHeights = new ArrayList<>();
    final List<Float> contentHeights = new ArrayList<>();

    public MasteryDisplay(MasteryPanel parentPanel, ShipAPI selectedModule, ShipAPI rootShip, float width, float height, float pad, boolean resetScrollbar, Action onButtonClick) {
        w = width;
        h = height;
        this.parentPanel = parentPanel;
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

    public int getLevelToScrollTo() {
        return levelToScrollTo;
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

    public void selectMasteryItem(int level, String id) {
        selectedLevels.put(level, id);
        // If another button is checked, need to uncheck it
        for (var entry : buttonsByLevelAndId.get(level).entrySet()) {
            if (!entry.getKey().equals(id)) {
                entry.getValue().setChecked(false);
                entry.getValue().setHighlightBrightness(0.25f);
            }
        }
        onButtonClick.perform();
    }

    public void deselectMasteryItem(int level) {
        selectedLevels.remove(level);
        onButtonClick.perform();
    }

    public NavigableMap<Integer, String> getSelectedLevels() {
        return selectedLevels;
    }

    public NavigableMap<Integer, String> getActiveLevels() {
        return activeLevels;
    }

    private float getScrollerPositionForLevel(int level) {
        level = Math.max(level, 1);
        level = Math.min(level, ShipMastery.getMaxMasteryLevel(rootSpec));
        // Edge case where max level is 0
        if (level <= 0) return 0f;
        return scrollerHeights.get(level-1)-h/2f+Math.min(contentHeights.get(level-1)/2f,h/2f-100f)-100f;
    }

    public void scrollToLevel(int level) {
        ReflectionUtils.invokeMethod(savedTooltip.getExternalScroller(), "scrollToY", getScrollerPositionForLevel(level));
    }

    public void snapToNearestNextLevel() {
        if (savedTooltip == null) return;
        float scrollHeight = savedTooltip.getExternalScroller().getYOffset();
        int snapTo = ShipMastery.getMaxMasteryLevel(rootSpec);
        for (int i = 0; i < scrollerHeights.size(); i++) {
            if (getScrollerPositionForLevel(i+1)> scrollHeight) {
                snapTo = i+1;
                break;
            }
        }
        scrollToLevel(snapTo);
    }

    public void snapToNearestPrevLevel() {
        if (savedTooltip == null) return;
        float scrollHeight = savedTooltip.getExternalScroller().getYOffset();
        int snapTo = 1;
        for (int i = scrollerHeights.size()-1; i >= 0; i--) {
            if (getScrollerPositionForLevel(i+1) < scrollHeight) {
                snapTo = i+1;
                break;
            }
        }
        scrollToLevel(snapTo);
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        int maxMastery = ShipMastery.getMaxMasteryLevel(rootSpec);
        tooltip.addSpacer(paddingBetweenLevels);
        totalHeight += paddingBetweenLevels;
        for (int i = 1; i <= maxMastery; i++) {
            List<String> optionIds = ShipMastery.getMasteryOptionIds(rootSpec, i);
            boolean singleOption = optionIds.size() == 1;
            float height = 0f;
            for (String optionId : optionIds) {
                CustomPanelAPI descriptionPanel = Global.getSettings().createCustom(w + 50f, MIN_DESC_HEIGHT, null);
                TooltipMakerAPI description = descriptionPanel.createUIElement(w - 30f, MIN_DESC_HEIGHT, false);
                tooltip.addComponent(descriptionPanel).inTL(0f, totalHeight + height);
                height += addEffectsDisplay(ShipMastery.getMasteryEffects(rootSpec, i, optionId), i, optionId, descriptionPanel, description, !singleOption);
            }
            CustomPanelAPI titlePanel = Global.getSettings().createCustom(w + 50f, 50f, null);
            TooltipMakerAPI titleMaker = titlePanel.createUIElement(w-8f, 50f, false);
            titleMaker.setTitleFont(Fonts.ORBITRON_24AABOLD);
            titleMaker.addTitle(Strings.MasteryPanel.levelText + " " + i, i > ShipMastery.getPlayerMasteryLevel(rootSpec) ? Misc.getGrayColor() : Misc.getBasePlayerColor()).setAlignment(Alignment.MID);
            titlePanel.addUIElement(titleMaker).inTR(0f, 0f);
            tooltip.addComponent(titlePanel).inTR(-3f, totalHeight-30f);
            totalHeight += paddingBetweenLevels + height;
            contentHeights.add(height);
            scrollerHeights.add(totalHeight-height);
        }
        tooltip.addSpacer(paddingBetweenLevels);
        totalHeight += paddingBetweenLevels;
        savedTooltip = tooltip;
    }

    void addMasteryDescriptions(List<MasteryEffect> effects, TooltipMakerAPI tooltip) {
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            MasteryDescription effectDescription = effect.getDescription(selectedModule, rootFleetMember);
            if (effectDescription != null) {
                if (effect.hasTag(MasteryTags.PREFIX_FLAGSHIP_ONLY)) {
                    effectDescription.addLabelWithPrefix(tooltip, Strings.Misc.flagshipOnly, Misc.getBasePlayerColor());
                } else {
                    effectDescription.addLabel(tooltip);
                }
            }
            tooltip.setParaFontDefault();
            tooltip.addSpacer(5f);
            effect.addPostDescriptionSection(tooltip, selectedModule, rootFleetMember);
            if (!rootFleetMember.equals(selectedModule.getFleetMember()) && effect.hasTag(
                    MasteryTags.DOESNT_AFFECT_MODULES)) {
                tooltip.addPara(Strings.Misc.doesntAffectModules, Settings.NEGATIVE_HIGHLIGHT_COLOR, 5f);
            }
            tooltip.addSpacer(i == effects.size() - 1 ? 20f : 5f);
        }
    }

    /** Returns the final height of the description. */
    float addEffectsDisplay(final List<MasteryEffect> effects, int level, String optionId, CustomPanelAPI innerPanel, TooltipMakerAPI innerTooltip, boolean showOptionLetter) {
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
        ButtonAPI descOutline = descriptionButton.addAreaCheckbox(hidden ? Strings.MasteryPanel.unknownMastery : "", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                                  Misc.getGrayColor(), w - 5f, descH, 0f);
        descOutline.setClickable(level <= currentMastery);
        descOutline.setGlowBrightness(level <= currentMastery ? 0.8f : 0.15f);
        if (level > currentMastery) {
            descOutline.setButtonPressedSound(null);
        }

        buttonsByLevelAndId.computeIfAbsent(level, k -> new HashMap<>()).put(optionId, descOutline);
        ReflectionUtils.setButtonListener(descOutline, new MasteryEffectButtonPressed(this, rootSpec, level, optionId));

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
        ButtonAPI levelButton = levelButtonTTM.addAreaCheckbox(level + (showOptionLetter ? optionId : ""), null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
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

        if (activeLevels.containsKey(level) && Objects.equals(optionId, activeLevels.get(level))) {
            descOutline.setChecked(true);
            descOutline.setHighlightBrightness(0f);
            levelButton.setChecked(true);
        }

        if (level <= currentMastery) {
            levelToScrollTo = level;
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

        // Instantly set the highlight strength to get rid of unwanted flickering when refreshing the panel
        ((Fader) ReflectionUtils.invokeMethod(descOutline, "getHighlightFader")).forceIn();
        ((Fader) ReflectionUtils.invokeMethod(levelButton, "getHighlightFader")).forceIn();

        return descH;
    }

    class MasteryDisplayPlugin implements CustomUIPanelPlugin {
        @Override
        public void positionChanged(PositionAPI position) {}

        @Override
        public void renderBelow(float alphaMult) {}

        @Override
        public void render(float alphaMult) {}

        @Override
        public void advance(float amount) {}

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                if (event.isMouseScrollEvent()) {
                    if (event.getEventValue() < 0f) {
                        snapToNearestNextLevel();
                    } else {
                        snapToNearestPrevLevel();
                    }
                }
                if (event.isKeyDownEvent()) {
                    int val = event.getEventValue();
                    if (val == Keyboard.KEY_DOWN) {
                        snapToNearestNextLevel();
                    }
                    else if (val == Keyboard.KEY_UP) {
                        snapToNearestPrevLevel();
                    }
                    else if (val >= Keyboard.KEY_1 && val <= Keyboard.KEY_0) {
                        scrollToLevel(val-1);
                    }
                    else if (val == Keyboard.KEY_SPACE || val == Keyboard.KEY_G) {
                        if (!activeLevels.equals(selectedLevels)) {
                            event.consume();
                            new ConfirmMasteryChangesPressed(parentPanel, rootSpec).trigger();
                        }
                    }
                    else if (val == Keyboard.KEY_ESCAPE) {
                        if (!activeLevels.equals(selectedLevels)) {
                            event.consume();
                            new CancelMasteryChangesPressed(parentPanel).trigger();
                        }
                    }
                }
            }
        }

        @Override
        public void buttonPressed(Object buttonId) {}
    }
}
