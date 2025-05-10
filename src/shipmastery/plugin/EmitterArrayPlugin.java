package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Utils;
import shipmastery.campaign.industries.BluePlanetaryShield;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class EmitterArrayPlugin extends BaseCustomEntityPlugin {
    public static final String KEY_NEXT_EMITTER = "$sms_nextEmitter";
    public static final String KEY_PREV_EMITTER = "$sms_prevEmitter";
    public static final String KEY_IS_FIRST_EMITTER = "$sms_isFirstEmitter";
    public static final String BEAM_FRINGE_PATH = "graphics/fx/beamfringed.png";
    public static final String BEAM_CORE_PATH = "graphics/fx/beam_laser_core.png";
    public static final String GLOW_PATH = "graphics/fx/particlealpha64sq.png";
    public static final float TRANSITION_SECONDS = 3f;
    public static final String ACTIVE_LEFT_PATH = "graphics/stations/sms_emitter_array_active3.png";
    public static final String ACTIVE_CENTER_PATH = "graphics/stations/sms_emitter_array_active2.png";
    public static final String ACTIVE_RIGHT_PATH = "graphics/stations/sms_emitter_array_active1.png";
    public static final float TEXTURE_SCROLL_SPEED = 500f; // In map units per second
    public static final Color INITIAL_COLOR = new Color(255, 100, 100);
    public static final Color FINAL_COLOR = new Color(150, 255, 200);
    private transient SpriteAPI activeLeftSprite;
    private transient SpriteAPI activeCenterSprite;
    private transient SpriteAPI activeRightSprite;
    private transient SpriteAPI beamFringeSprite;
    private transient SpriteAPI beamCoreSprite;
    private transient SpriteAPI glowSprite;
    private transient SectorEntityToken center;
    private transient SectorEntityToken nextEmitter;
    private transient SectorEntityToken prevEmitter;
    private transient float texOffset = 0f;
    private transient boolean inited = false;
    FaderUtil leftFader = new FaderUtil(0f, TRANSITION_SECONDS);
    FaderUtil centerFader = new FaderUtil(0f, TRANSITION_SECONDS);
    FaderUtil rightFader = new FaderUtil(0f, TRANSITION_SECONDS);
    FaderUtil colorFader = new FaderUtil(0f, 3f*TRANSITION_SECONDS);
    FaderUtil shieldFader = new FaderUtil(0f, TRANSITION_SECONDS);
    int leftPower = 0, centerPower = 0, rightPower = 0;
    float leftPowerRender = 0f, centerPowerRender = 0f, rightPowerRender = 0f;

    public void init() {
        nextEmitter = (SectorEntityToken) entity.getMemoryWithoutUpdate().get(KEY_NEXT_EMITTER);
        prevEmitter = (SectorEntityToken) entity.getMemoryWithoutUpdate().get(KEY_PREV_EMITTER);
        activeLeftSprite = Utils.getLoadedSprite(ACTIVE_LEFT_PATH);
        activeCenterSprite = Utils.getLoadedSprite(ACTIVE_CENTER_PATH);
        activeRightSprite = Utils.getLoadedSprite(ACTIVE_RIGHT_PATH);
        beamFringeSprite = Utils.getLoadedSprite(BEAM_FRINGE_PATH);
        beamCoreSprite = Utils.getLoadedSprite(BEAM_CORE_PATH);
        glowSprite = Utils.getLoadedSprite(GLOW_PATH);
        center = entity.getOrbitFocus();
        inited = true;
    }

    @Override
    public float getRenderRange() {
        return 6000f;
    }

    public void setLeftOutput(int power) {
        if (power > 0) {
            leftFader.fadeIn();
        } else {
            leftFader.fadeOut();
        }
        leftPower = Math.min(3, power);
    }

    public int getLeftOutput() {
        return leftPower;
    }

    public void setCenterOutput(int power) {
        if (power > 0) {
            centerFader.fadeIn();
        } else {
            centerFader.fadeOut();
        }
        centerPower = Math.min(3, power);
    }

    public int getCenterOutput() {
        return centerPower;
    }

    public void setRightOutput(int power) {
        if (power > 0) {
            rightFader.fadeIn();
        } else {
            rightFader.fadeOut();
        }
        rightPower = Math.min(3, power);
    }

    public int getRightOutput() {
        return rightPower;
    }

    public void checkIfSolved() {
        List<EmitterArrayPlugin> plugins = new ArrayList<>();
        List<Integer> edgeAmounts = new ArrayList<>();
        plugins.add(this);
        edgeAmounts.add(0);
        var curPlugin = this;
        while (true) {
            var next = (EmitterArrayPlugin) curPlugin.nextEmitter.getCustomPlugin();
            if (plugins.contains(next)) break;
            plugins.add(next);
            edgeAmounts.add(0);
            curPlugin = next;
        }

        for (int i = 0; i < plugins.size(); i++) {
            var plugin = plugins.get(i);
            edgeAmounts.set(i, edgeAmounts.get(i) + plugin.rightPower);
            edgeAmounts.set(i, edgeAmounts.get(i) + plugins.get((i+1)%plugins.size()).leftPower);
        }

        boolean solved = true;
        for (int i = 0, j=(i+1)%plugins.size(); i < plugins.size(); i++, j=(j+1)%plugins.size()) {
            int m = plugins.get(i).centerPower;
            int n = plugins.get(j).centerPower;
            if (m != 1 && m != 3) {
                solved = false;
                break;
            }
            if (n != 1 && n != 3) {
                solved = false; break;
            }
            if (m == n) {
                solved = false; break;
            }
            if (edgeAmounts.get(i) != 2) {
                solved = false; break;
            }
        }

        if (solved) {
            Global.getSector().getMemoryWithoutUpdate().set(Strings.Campaign.EMITTER_SOLVED, true);
            for (var plugin : plugins) {
                plugin.colorFader.fadeIn();
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (!inited) {
            init();
        }
        // Core and fringe have same width, so can share texOffset
        texOffset = (texOffset + amount * TEXTURE_SCROLL_SPEED / beamFringeSprite.getWidth()) % 1f;
        leftFader.advance(amount);
        centerFader.advance(amount);
        rightFader.advance(amount);
        colorFader.advance(amount);
        shieldFader.advance(amount);
        if (leftPowerRender < leftPower) {
            leftPowerRender = Math.min(leftPower, leftPowerRender + amount / TRANSITION_SECONDS);
        }
        if (leftPowerRender > leftPower) {
            leftPowerRender = Math.max(leftPower, leftPowerRender - amount / TRANSITION_SECONDS);
        }
        if (centerPowerRender < centerPower) {
            centerPowerRender = Math.min(centerPower, centerPowerRender + amount / TRANSITION_SECONDS);
        }
        if (centerPowerRender > centerPower) {
            centerPowerRender = Math.max(centerPower, centerPowerRender - amount / TRANSITION_SECONDS);
        }
        if (rightPowerRender < rightPower) {
            rightPowerRender = Math.min(rightPower, rightPowerRender+ amount / TRANSITION_SECONDS);
        }
        if (rightPowerRender > rightPower) {
            rightPowerRender = Math.max(rightPower, rightPowerRender - amount / TRANSITION_SECONDS);
        }
        float sum = leftPowerRender + rightPowerRender + centerPowerRender;
        if (sum > 0f) {
            Global.getSoundPlayer().playLoop("phase_beam_loop", entity, 2f, Math.min(1f, sum/3f), entity.getLocation(), entity.getVelocity());
        }
        if (colorFader.getBrightness() >= 1f) {
            shieldFader.fadeIn();
        }
        if (entity.getMemoryWithoutUpdate().getBoolean(KEY_IS_FIRST_EMITTER)) {
            if (shieldFader.getBrightness() < 1f) {
                BluePlanetaryShield.applyVisuals((PlanetAPI) center, 1f - shieldFader.getBrightness());
            }
            if (shieldFader.getBrightness() >= 1f && center.getMarket().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.REMOTE_PYLON_HAS_SHIELD)) {
                Global.getSoundPlayer().playSound("ui_shutdown_industry", 1f, 1f, Global.getSector().getPlayerFleet().getLocation(), new Vector2f());
                center.setCustomDescriptionId("sms_remote_pylon_no_shield");
                center.getMarket().getMemoryWithoutUpdate().unset(Strings.Campaign.REMOTE_PYLON_HAS_SHIELD);
            }
        }
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.ABOVE) return;
        Vector2f emitPt1 = Vector2f.add(entity.getLocation(), Misc.rotateAroundOrigin(new Vector2f(-15f, -40f), entity.getFacing(), new Vector2f()), null);
        Vector2f destPt1 = Vector2f.add(prevEmitter.getLocation(), Misc.rotateAroundOrigin(new Vector2f(-15f, 40f), prevEmitter.getFacing(), new Vector2f()), null);
        Vector2f emitPt2 = Vector2f.add(entity.getLocation(), Misc.rotateAroundOrigin(new Vector2f(-15f, 40f), entity.getFacing(), new Vector2f()), null);
        Vector2f destPt2 = Vector2f.add(nextEmitter.getLocation(), Misc.rotateAroundOrigin(new Vector2f(-15f, -40f), nextEmitter.getFacing(), new Vector2f()), null);
        Vector2f emitPt3 = Vector2f.add(entity.getLocation(), Misc.rotateAroundOrigin(new Vector2f(-43f, 0f), entity.getFacing(), new Vector2f()), null);
        float nextPower = ((EmitterArrayPlugin) nextEmitter.getCustomPlugin()).leftPowerRender;
        float prevPower = ((EmitterArrayPlugin) prevEmitter.getCustomPlugin()).rightPowerRender;
        float combinedLeft = Math.min(3f, prevPower + leftPowerRender);
        float combinedRight = Math.min(3f, nextPower + rightPowerRender);
        Vector2f planetOffset = Misc.getUnitVectorAtDegreeAngle(entity.getFacing());
        planetOffset.scale(shieldFader.getBrightness() >= 1f ? center.getRadius() : center.getRadius()*1.196f);
        Vector2f destPt3 = Vector2f.add(center.getLocation(), planetOffset, null);
        Color color = shipmastery.util.Utils.mixColorHSVA(INITIAL_COLOR, FINAL_COLOR, colorFader.getBrightness());
        Color brightColor = shipmastery.util.Utils.mixColor(color, Color.WHITE, 0.5f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        beamFringeSprite.bindTexture();
        if (leftPowerRender > 0f) {
            renderLineWithWidth(emitPt1, destPt1, 10f * combinedLeft, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (55f * leftPowerRender)), prevEmitter.getSensorContactFaderBrightness());
        }
        if (centerPowerRender > 0f) {
            renderLineWithWidth(emitPt3, destPt3, 10f * centerPowerRender, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (55f * centerPowerRender)), 1f);

        }
        if (rightPowerRender > 0f) {
            renderLineWithWidth(emitPt2, destPt2, 10f * combinedRight, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (55f * rightPowerRender)), nextEmitter.getSensorContactFaderBrightness());
        }
        beamCoreSprite.bindTexture();
        if (combinedLeft > 0f) {
            renderLineWithWidth(emitPt1, destPt1, 10f * combinedLeft, new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), (int) (55f * leftPowerRender)), prevEmitter.getSensorContactFaderBrightness());
        }
        if (centerPowerRender > 0f) {
            renderLineWithWidth(emitPt3, destPt3, 10f * centerPowerRender, new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), (int) (55f * centerPowerRender)), 1f);
        }
        if (rightPowerRender > 0f) {
            renderLineWithWidth(emitPt2, destPt2, 10f * combinedRight, new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), (int) (55f * rightPowerRender)), nextEmitter.getSensorContactFaderBrightness());
        }
        if (leftPowerRender > 0f) {
            activeLeftSprite.setAngle(entity.getFacing()-90f);
            activeLeftSprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
            activeLeftSprite.setAlphaMult(leftPowerRender / 3f);
            activeLeftSprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        }
        if (centerPowerRender > 0f) {
            activeCenterSprite.setAngle(entity.getFacing()-90f);
            activeCenterSprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
            activeCenterSprite.setAlphaMult(centerPowerRender / 3f);
            activeCenterSprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
            glowSprite.setColor(brightColor);
            glowSprite.setSize(64f*centerPowerRender, 64f*centerPowerRender);
            glowSprite.setAlphaMult(Math.min(1f, centerPowerRender));
            glowSprite.renderAtCenter(destPt3.x, destPt3.y);
        }
        if (rightPowerRender > 0f) {
            activeRightSprite.setAngle(entity.getFacing()-90f);
            activeRightSprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
            activeRightSprite.setAlphaMult(rightPowerRender / 3f);
            activeRightSprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        }
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void renderLineWithWidth(Vector2f start, Vector2f end, float width, Color color, float endAlphaMult) {
        Vector2f diff = Vector2f.sub(end, start, null);
        float dist = diff.length();
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
        GL11.glColor4f(colors[0], colors[1], colors[2], colors[3]*endAlphaMult);
        GL11.glTexCoord2f(texOffset, 0f);
        GL11.glVertex2f(p4.x, p4.y);
        GL11.glTexCoord2f(texOffset, beamFringeSprite.getTexHeight());
        GL11.glVertex2f(p3.x, p3.y);
        GL11.glColor4f(colors[0], colors[1], colors[2], colors[3]);
        GL11.glTexCoord2f(beamFringeSprite.getTexWidth()*dist/ beamFringeSprite.getWidth() + texOffset, beamFringeSprite.getTexHeight());
        GL11.glVertex2f(p2.x, p2.y);
        GL11.glTexCoord2f(beamFringeSprite.getTexWidth()*dist/ beamFringeSprite.getWidth() + texOffset, 0f);
        GL11.glVertex2f(p1.x, p1.y);
        GL11.glEnd();
    }
}
