package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.ShipMastery;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.aicoreinterface.AICoreInterfacePlugin;
import shipmastery.ui.LevelUpDialog;
import shipmastery.util.MasteryUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.List;

import shipmastery.util.Utils;

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

    private boolean needsInjection(Object fleetItem) {
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(fleetItem, "getChildrenNonCopy");
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof CustomPanelAPI panel && panel.getPlugin() instanceof GenericFleetPanelUIPlugin) {
                return false;
            }
        }
        return true;
    }

    // Used to check if a panel is already injected by checking if it has a child with a CustomPanelAPI with this plugin
    public abstract static class GenericFleetPanelUIPlugin extends BaseCustomUIPanelPlugin {}

    public static class FleetPanelItemUIPlugin extends GenericFleetPanelUIPlugin {
        float x;
        float y;
        private float width;
        private float height;
        private final ShipVariantAPI variant;
        private final FleetMemberAPI member;
        private final ShipHullSpecAPI spec;
        public Float widthOverride = null;
        public Float heightOverride = null;
        public float numBars = 40;
        private final float xPad = 5f;
        private final float yPad = 35f;
        public static final SpriteAPI masteryBarSprite = Global.getSettings().getSprite("ui", "sms_mastery_bar");
        public boolean forceNoFlash = false;
        public float enhanceFrac = 0f;
        public float progress = 0f;
        public float extraYOffset = 0f;
        public float extraXOffset = 0f;
        public float extraAlphaMult = 0.75f;
        private final PositionAPI position;
        public static final Color grayColor = new Color(0.2f, 0.2f, 0.2f);
        public Color brightMasteryColor = Utils.mixColor(Settings.MASTERY_COLOR, Color.WHITE, 0.3f);
        public Color brighterMasteryColor = Utils.mixColor(Settings.MASTERY_COLOR, Color.WHITE, 0.7f);
        public Color brightHighlightColor = Utils.mixColor(Settings.POSITIVE_HIGHLIGHT_COLOR, Color.WHITE, 0.4f);
        public Color brightEnhanceColor = Utils.mixColor(Misc.getStoryOptionColor(), Color.WHITE, 0.3f);
        private boolean hasEngineeringOverride = false;
        public boolean showIcons = true;

        public record MasteryData(ShipHullSpecAPI spec, int level, int maxLevel, int enhances, float curPts,
                                  float reqPts) {
        }

        public MasteryData data;
        private final Action onLevelUp;
        private final IntervalUtil updateInterval = new IntervalUtil(0.2f, 0.2f);

        public void update() {
            int level = ShipMastery.getPlayerMasteryLevel(spec);
            int maxLevel = ShipMastery.getMaxMasteryLevel(spec);
            int enhances = MasteryUtils.getEnhanceCount(spec);

            float curPts = ShipMastery.getPlayerMasteryPoints(spec);
            float reqPts = enhances >= MasteryUtils.MAX_ENHANCES ? 0f : level >= maxLevel ? MasteryUtils.getEnhanceMPCost(spec) : MasteryUtils.getUpgradeCost(spec);
            progress = reqPts <= 0f ? 1f : curPts / reqPts;
            enhanceFrac = (float) MasteryUtils.getEnhanceCount(spec) / MasteryUtils.MAX_ENHANCES;
            brightMasteryColor = Utils.mixColor(Settings.MASTERY_COLOR, Color.WHITE, 0.6f);
            brightHighlightColor = Utils.mixColor(Settings.POSITIVE_HIGHLIGHT_COLOR, Color.WHITE, 0.4f);

            hasEngineeringOverride = variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE);

            if (level >= maxLevel || member.getFleetCommander() == null || !member.getFleetCommander().isPlayer()) {
                forceNoFlash = true;
            }

            data = new MasteryData(spec, level, maxLevel, enhances, curPts, reqPts);
        }

        private void updatePosition(PositionAPI position) {
            width = widthOverride == null ? 12f : widthOverride;
            height = heightOverride == null ? position.getHeight() / 2.75f : heightOverride;
            x = position.getX() + position.getWidth() - width - extraXOffset - xPad;
            y = position.getY() + position.getHeight() - height - extraYOffset - yPad;
        }

        public FleetPanelItemUIPlugin(ShipVariantAPI rootVariant, FleetMemberAPI member, ShipHullSpecAPI spec, PositionAPI position) {
            this(rootVariant, member, spec, position, null);
        }

        public FleetPanelItemUIPlugin(ShipVariantAPI rootVariant, FleetMemberAPI member, ShipHullSpecAPI spec, PositionAPI position, Action onLevelUp) {
            this.position = position;
            this.variant = rootVariant;
            this.member = member;
            this.onLevelUp = onLevelUp;
            this.spec = spec;
            updatePosition(position);
            update();
        }

        @Override
        public void positionChanged(PositionAPI position) {
            updatePosition(this.position);
        }

        @Override
        public void advance(float amount) {
            updateInterval.advance(amount);
            if (updateInterval.intervalElapsed()) {
                update();
            }
        }

        private void drawRect(float x, float y, float width, float height, float fractionFilledStart, float fractionFilledEnd, Color color, float alpha) {
            float texXStart = 0.1f, texYStart = 0f;
            float texXEnd = 0.525f, texYEnd = 0.00785f * numBars;
            float[] comps = color.getRGBComponents(null);
            GL11.glColor4f(comps[0], comps[1], comps[2], alpha);
            if (height > width) {
                GL11.glTexCoord2f(texXStart, texYStart + (texYEnd - texYStart) * fractionFilledStart);
                GL11.glVertex2f(x, y + height * fractionFilledStart);
                GL11.glTexCoord2f(texXEnd, texYStart + (texYEnd - texYStart) * fractionFilledStart);
                GL11.glVertex2f(x + width, y + height * fractionFilledStart);
                GL11.glTexCoord2f(texXEnd, texYStart + (texYEnd - texYStart) * fractionFilledEnd);
                GL11.glVertex2f(x + width, y + height * fractionFilledEnd);
                GL11.glTexCoord2f(texXStart, texYStart + (texYEnd - texYStart) * fractionFilledEnd);
                GL11.glVertex2f(x, y + height * fractionFilledEnd);
            } else {
                GL11.glTexCoord2f(texXStart, texYStart + (texYEnd - texYStart) * fractionFilledStart);
                GL11.glVertex2f(x + width * fractionFilledStart, y);
                GL11.glTexCoord2f(texXEnd, texYStart + (texYEnd - texYStart) * fractionFilledStart);
                GL11.glVertex2f(x + width * fractionFilledStart, y + height);
                GL11.glTexCoord2f(texXEnd, texYStart + (texYEnd - texYStart) * fractionFilledEnd);
                GL11.glVertex2f(x + width * fractionFilledEnd, y + height);
                GL11.glTexCoord2f(texXStart, texYStart + (texYEnd - texYStart) * fractionFilledEnd);
                GL11.glVertex2f(x + width * fractionFilledEnd, y);
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void drawLineWithWidth(Vector2f start, Vector2f end, float width, Color color, float alphaMult) {
            Vector2f diff = Vector2f.sub(end, start, null);
            //noinspection SuspiciousNameCombination
            Vector2f perp = MathUtils.safeNormalize(new Vector2f(-diff.y, diff.x));
            perp.scale(width);
            Vector2f negPerp = new Vector2f(-perp.x, -perp.y);
            Vector2f p1 = Vector2f.add(start, perp, null);
            Vector2f p2 = Vector2f.add(start, negPerp, null);
            Vector2f p3 = Vector2f.add(end, negPerp, null);
            Vector2f p4 = Vector2f.add(end, perp, null);

            GL11.glBegin(GL11.GL_QUADS);
            float[] colors = color.getRGBComponents(null);
            GL11.glColor4f(colors[0], colors[1], colors[2], colors[3] * alphaMult);
            GL11.glVertex2f(p4.x, p4.y);
            GL11.glVertex2f(p3.x, p3.y);
            GL11.glVertex2f(p2.x, p2.y);
            GL11.glVertex2f(p1.x, p1.y);
            GL11.glEnd();
        }

        @Override
        public void renderBelow(float alphaMult) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            masteryBarSprite.bindTexture();

            boolean fullProgress = progress >= 1f && !forceNoFlash;

            float clampedProgress = Math.min(1f, Math.max(0f, progress));
            float greenFilled = Math.min(clampedProgress, enhanceFrac);
            var flashBrightness = Global.getSector().getCampaignUI().getSharedFader().getBrightness();
            GL11.glBegin(GL11.GL_QUADS);
            drawRect(x, y, width, height, 0f, 1f, grayColor, alphaMult * extraAlphaMult);
            Color darkEnhance = Misc.getStoryDarkColor();
            if (enhanceFrac > 0f)
                drawRect(x, y, width, height, 0f, Math.min(1f, enhanceFrac), darkEnhance, alphaMult * extraAlphaMult);
            Color enhance = progress >= 1f ? brightEnhanceColor : Misc.getStoryOptionColor();
            if (greenFilled > 0f)
                drawRect(x, y, width, height, 0f, greenFilled, enhance, alphaMult * extraAlphaMult * (fullProgress ? 1f - 0.75f * flashBrightness : 1f));
            Color mastery = progress >= 1f ? brightMasteryColor : Settings.MASTERY_COLOR;
            if (clampedProgress > greenFilled)
                drawRect(x, y, width, height, greenFilled, clampedProgress, mastery, alphaMult * extraAlphaMult * (fullProgress ? 1f - 0.75f * flashBrightness: 1f));
            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            if (hasEngineeringOverride && showIcons) {
                drawLineWithWidth(new Vector2f(x, y), new Vector2f(x + width, y + height), 2.5f, Color.RED, alphaMult * extraAlphaMult * 0.75f);
                drawLineWithWidth(new Vector2f(x + width, y), new Vector2f(x, y + height), 2.5f, Color.RED, alphaMult * extraAlphaMult * 0.75f);
                GL11.glEnd();
            }

            GL11.glDisable(GL11.GL_BLEND);
        }

        @Override
        public void buttonPressed(Object buttonId) {
            new LevelUpDialog(member, spec, onLevelUp).show();
        }

        public void makeOutline(CustomPanelAPI panel, boolean smallText, boolean showIcons) {
            TooltipMakerAPI outline = panel.createUIElement(width + 2f, height + 2f, false);
            var spec = member.getHullSpec();
            var box = outline.addAreaCheckbox("", null, Color.WHITE, brighterMasteryColor, Color.WHITE, width + 2f, height + 2f, -1f);
            box.setEnabled(MasteryUtils.getEnhanceCount(spec) < MasteryUtils.MAX_ENHANCES
                    && progress >= 1f
                    && member.getFleetCommander() != null
                    && member.getFleetCommander().isPlayer());
            box.setButtonDisabledPressedSound(null);
            box.setMouseOverSound(null);
            box.setOpacity(0.15f);

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
            }, TooltipMakerAPI.TooltipLocation.RIGHT, false);

            String font = smallText ? Fonts.ORBITRON_12 : Fonts.ORBITRON_20AA;
            outline.setParaFont(font);
            LabelAPI temp = Global.getSettings().createLabel("" + data.level, font);
            var textWidth = temp.computeTextWidth(temp.getText());
            outline.setTextWidthOverride(textWidth);
            var levelLabel = outline.addPara("" + data.level, MasteryUtils.getPlayerUnassignedCount(data.spec) > 0 ? brightHighlightColor : brighterMasteryColor, -height / 2f - (smallText ? 8f : 11f));
            levelLabel.getPosition().setXAlignOffset(-textWidth / 2f + 1f + width / 2f);

            panel.addUIElement(outline).inTR(xPad + 4f + extraXOffset, yPad + extraYOffset);

            if (MasterySharingHandler.isMasterySharingActive(spec) && showIcons) {
                float size = smallText ? 20f : 32f;
                TooltipMakerAPI icon = panel.createUIElement(size, size, false);
                icon.addImage("graphics/icons/ui/sms_construct_icon.png", size, size, 0f);
                icon.getPrev().setOpacity(0.5f);
                icon.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                    @Override
                    public boolean isTooltipExpandable(Object tooltipParam) {
                        return false;
                    }

                    @Override
                    public float getTooltipWidth(Object tooltipParam) {
                        return 200f;
                    }

                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara(Utils.asInt(MasterySharingHandler.getCurrentMasterySharingMP(spec)) + " / " + Utils.asInt(MasterySharingHandler.SHARED_MASTERY_MP_REQ)
                                + " %s", 0f, Misc.getGrayColor(), Strings.Misc.stored + " " + Strings.Misc.XP).setAlignment(Alignment.MID);
                    }
                }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
                panel.addUIElement(icon).leftOfMid(outline, smallText ? 0f : 2f).setYAlignOffset(size / 2f);
            }

            String integrated = AICoreInterfacePlugin.getIntegratedPseudocore(member.getVariant());
            if (integrated != null && showIcons) {
                float size = smallText ? 20f : 32f;
                TooltipMakerAPI icon = panel.createUIElement(size, size, false);
                icon.addImage("graphics/icons/ui/sms_integrate_icon.png", size, size, 0f);
                icon.getPrev().setOpacity(0.5f);
                icon.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
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
                        AICoreInterfacePlugin.addIntegratedDescToTooltip(tooltip, integrated, 0f);
                    }
                }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
                panel.addUIElement(icon).leftOfMid(outline, smallText ? 0f : 2f).setYAlignOffset(-size / 2f);
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

        if (needsInjection(ReflectionUtils.invokeMethod(fleetPanelListScroller, "getContentContainer"))) {
            CustomPanelAPI custom = Global.getSettings().createCustom(0f, 0f, new GenericFleetPanelUIPlugin() {

                boolean injectedFirstFrame = false;

                @Override
                public void advance(float amount) {
                    if (!injectedFirstFrame) {
                        injectedFirstFrame = true;
                        injectIfNeeded();
                    }
                }

                void injectIfNeeded() {
                    fleetPanelItems.stream()
                            .filter(FleetPanelHandler.this::needsInjection)
                            .forEach(item -> {
                                var member = (FleetMemberAPI) ReflectionUtils.invokeMethod(item, "getMember");
                                var pos = ((UIComponentAPI) item).getPosition();
                                var plugin = new FleetPanelItemUIPlugin(member.getVariant(), member, member.getHullSpec(), pos, () -> {
                                    var scroller = (ScrollPanelAPI) fleetPanelListScroller;
                                    float yOffset = scroller.getYOffset();
                                    ReflectionUtils.invokeMethod(fleetPanel, "recreateUI", false);
                                    UIPanelAPI fleetPanelList = (UIPanelAPI) ReflectionUtils.invokeMethod(fleetPanel, "getList");
                                    ScrollPanelAPI fleetPanelListScroller = (ScrollPanelAPI) ReflectionUtils.invokeMethod(fleetPanelList, "getScroller");
                                    fleetPanelListScroller.setYOffset(yOffset);
                                });
                                CustomPanelAPI custom = Global.getSettings().createCustom(pos.getWidth(), pos.getHeight(), plugin);
                                plugin.makeOutline(custom, false, true);
                                ((UIPanelAPI) item).addComponent(custom).inMid();
                            });
                }

                @Override
                public void positionChanged(PositionAPI position) {
                    DeferredActionPlugin.performLater(() -> injectedFirstFrame = false, 0f);
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
