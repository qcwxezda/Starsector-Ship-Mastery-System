package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.ui.FontException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import shipmastery.ShipMastery;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.ui.LazyFont;

public class FleetPanelHandler implements EveryFrameScript, CoreTabListener {

    private boolean insideFleetPanel = false;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    private boolean isAlreadyInjected(Object fleetItem) {
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(fleetItem, "getChildrenNonCopy");
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof CustomPanelAPI panel && panel.getPlugin() instanceof GenericFleetPanelUIPlugin) {
                return true;
            }
        }
        return false;
    }

    // Used to check if a panel is already injected by checking if it has a child with a CustomPanelAPI with this plugin
    public abstract static class GenericFleetPanelUIPlugin implements CustomUIPanelPlugin {
        @Override
        public void positionChanged(PositionAPI position) {
        }

        @Override
        public void renderBelow(float alphaMult) {
        }

        @Override
        public void render(float alphaMult) {
        }

        @Override
        public void advance(float amount) {
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
        }

        @Override
        public void buttonPressed(Object buttonId) {
        }
    }

    public static class FleetPanelItemUIPlugin extends GenericFleetPanelUIPlugin {
        private float x;
        private float y;
        private float width;
        private float height;
        private final float xPad = 5f;
        private final float yPad = 35f;
        private final FaderUtil flashFader = new FaderUtil(1f, 0.5f, 0.5f, true, true);
        public static final SpriteAPI masteryBarSprite = Global.getSettings().getSprite("ui", "sms_mastery_bar");
        public boolean forceNoFlash = false;
        public float enhanceFrac = 0f;
        public float progress = 0f;
        public float extraYOffset = 0f;
        public float extraXOffset = 0f;
        public float extraAlphaMult = 0.6f;
        public boolean showSmallText = false;
        private final PositionAPI position;
        private final LazyFont font;
        private LazyFont.DrawableString text;

        public record MasteryData(int level, int maxLevel, int enhances, float curPts, float reqPts) {}

        public MasteryData updateFromSpec(ShipHullSpecAPI spec) {
            int level = ShipMastery.getPlayerMasteryLevel(spec);
            int maxLevel = ShipMastery.getMaxMasteryLevel(spec);
            int enhances = MasteryUtils.getEnhanceCount(spec);

            float curPts =  ShipMastery.getPlayerMasteryPoints(spec);
            float reqPts = enhances >= MasteryUtils.MAX_ENHANCES ? 0f : level >= maxLevel ? MasteryUtils.getEnhanceMPCost(spec) : MasteryUtils.getUpgradeCost(spec);
            progress = reqPts <= 0f ? 1f : curPts / reqPts;
            enhanceFrac = (float) MasteryUtils.getEnhanceCount(spec) / MasteryUtils.MAX_ENHANCES;

            if (enhances >= MasteryUtils.MAX_ENHANCES) {
                forceNoFlash = true;
            }


            text = font.createText("" + level, Color.WHITE, 20f);

            return new MasteryData(level, maxLevel, enhances, curPts, reqPts);
        }

        private void updatePosition(PositionAPI position) {
            width = 12f;
            height = position.getHeight() / 3.7f;
            x = position.getX() + position.getWidth() - width - extraXOffset - xPad;
            y = position.getY() + position.getHeight() - height - extraYOffset - yPad;
            flashFader.fadeOut();
        }

        public FleetPanelItemUIPlugin(PositionAPI position) {
            this.position = position;
            updatePosition(position);

            try {
                font = LazyFont.loadFont("graphics/fonts/orbitron20aabold.fnt");
            } catch (FontException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void positionChanged(PositionAPI position) {
            updatePosition(this.position);
        }

        @Override
        public void advance(float amount) {
            flashFader.advance(amount);
        }

        private void drawRect(float x, float y, float width, float height, float fractionFilledStart, float fractionFilledEnd, Color color, float alpha) {
            float texX = 0.625f, texY = 0.157f;
            float[] comps = color.getRGBComponents(null);
            GL11.glColor4f(comps[0], comps[1], comps[2], alpha);
            GL11.glTexCoord2f(0f, texY * fractionFilledStart);
            GL11.glVertex2f(x, y + height * fractionFilledStart);
            GL11.glTexCoord2f(texX, texY * fractionFilledStart);
            GL11.glVertex2f(x + width, y + height * fractionFilledStart);
            GL11.glTexCoord2f(texX, texY * fractionFilledEnd);
            GL11.glVertex2f(x + width, y + height * fractionFilledEnd);
            GL11.glTexCoord2f(0f, texY * fractionFilledEnd);
            GL11.glVertex2f(x, y + height * fractionFilledEnd);
        }

        @Override
        public void render(float alphaMult) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            masteryBarSprite.bindTexture();

            boolean fullProgress = progress >= 1f && !forceNoFlash;

            float clampedProgress = Math.min(1f, Math.max(0f, progress));
            float greenFilled = Math.min(clampedProgress, enhanceFrac);
            GL11.glBegin(GL11.GL_QUADS);
            Color gray = new Color(0.2f, 0.2f, 0.2f);
            drawRect(x, y, width, height, 0f,1f, gray, alphaMult * extraAlphaMult);
            Color darkEnhance = Misc.getStoryDarkColor();
            drawRect(x, y, width, height, 0f, Math.min(1f, enhanceFrac), darkEnhance, alphaMult * extraAlphaMult);
            Color enhance = Misc.getStoryOptionColor();
            drawRect(x, y, width, height, 0f, greenFilled, enhance, alphaMult * extraAlphaMult * (fullProgress ? 1f - 0.75f*flashFader.getBrightness() : 1f));
            Color mastery = Settings.MASTERY_COLOR;
            drawRect(x, y, width, height, greenFilled, clampedProgress, mastery, alphaMult * extraAlphaMult * (fullProgress ? 1f - 0.75f*flashFader.getBrightness() : 1f));

            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);

            if (showSmallText) {
                text.setBaseColor(new Color(0.85f, 1f, 1f, alphaMult));
                text.draw(x-1f, y+height/2f+10f);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (!insideFleetPanel || !Settings.SHOW_MP_AND_LEVEL_IN_REFIT) {
            return;
        }
        var coreUI = ReflectionUtils.getCoreUI();
        var currentTab = ReflectionUtils.invokeMethod(coreUI, "getCurrentTab");
        UIPanelAPI fleetPanel = (UIPanelAPI) ReflectionUtils.invokeMethod(currentTab, "getFleetPanel");

        UIPanelAPI fleetPanelList = (UIPanelAPI) ReflectionUtils.invokeMethod(fleetPanel, "getList");
        UIPanelAPI fleetPanelListScroller = (UIPanelAPI) ReflectionUtils.invokeMethod(fleetPanelList, "getScroller");
        List<?> fleetPanelItems = (List<?>) ReflectionUtils.invokeMethod(fleetPanelList, "getItems");

        if (!isAlreadyInjected(ReflectionUtils.invokeMethod(fleetPanelListScroller, "getContentContainer"))) {
            CustomPanelAPI custom = Global.getSettings().createCustom(0f, 0f, new GenericFleetPanelUIPlugin() {
                @Override
                public void positionChanged(PositionAPI position) {
                    DeferredActionPlugin.performLater(() -> {
                        for (Object item : fleetPanelItems) {
                            // make sure the injector doesn't duplicate stuff
                            if (isAlreadyInjected(item)) continue;
                            var member = (FleetMemberAPI) ReflectionUtils.invokeMethod(item, "getMember");
                            var spec = member.getHullSpec();

                            var pos = ((UIComponentAPI) item).getPosition();
                            var plugin = new FleetPanelItemUIPlugin(pos);
                            var data = plugin.updateFromSpec(spec);

                            CustomPanelAPI custom = Global.getSettings().createCustom(pos.getWidth(), pos.getHeight(), plugin);
                            float w = plugin.width, h = plugin.height;
                            TooltipMakerAPI outline = custom.createUIElement(w + 2f, h + 1f, false);
                            var box = outline.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w + 2f, h + 1f, 0f);
                            box.setEnabled(false);
                            box.setButtonDisabledPressedSound(null);
                            box.setMouseOverSound(null);
                            outline.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                                @Override
                                public boolean isTooltipExpandable(Object tooltipParam) {
                                    return false;
                                }

                                @Override
                                public float getTooltipWidth(Object tooltipParam) {
                                    return 150f;
                                }

                                @Override
                                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                                    tooltip.addPara(
                                            Strings.RefitScreen.mpToNextLevel,
                                            0f,
                                            Misc.getTextColor(),
                                            "" + (int) data.curPts,
                                            "" + (int) data.reqPts).setAlignment(Alignment.MID);
                                }
                            }, TooltipMakerAPI.TooltipLocation.RIGHT);
                            if (data.level <= 9) {
                                outline.setParaFont(Fonts.ORBITRON_20AABOLD);
                                outline.addPara("" + data.level, Settings.MASTERY_COLOR, 0f).setOpacity(0.75f);
                            } else {
                                LabelAPI temp = Global.getSettings().createLabel("" + data.level, Fonts.ORBITRON_12);
                                var textWidth = temp.computeTextWidth(temp.getText());
                                outline.setTextWidthOverride(textWidth);
                                outline.setParaFont(Fonts.ORBITRON_12);
                                var levelLabel = outline.addPara("" + data.level, Settings.MASTERY_COLOR, 0f);
                                levelLabel.getPosition().setXAlignOffset(-textWidth + 18f);
                                levelLabel.setOpacity(0.75f);
                            }

                            custom.addUIElement(outline).inTR(plugin.xPad + 4f, plugin.yPad);
                            ((UIPanelAPI) item).addComponent(custom).inMid();
                        }
                    }, 0.01f);
                }
            });
            fleetPanelListScroller.addComponent(custom);
        }

    }

    @Override
    public void onCoreTabOpened(CoreUITabId id) {
        insideFleetPanel = id == CoreUITabId.FLEET;
    }

    @Override
    public void onCoreUIDismissed() {
        insideFleetPanel = false;
    }
}
