package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.opengl.GL14;
import particleengine.Particles;
import particleengine.Utils;
import shipmastery.fx.ParticleBurstEmitter;

import java.awt.Color;

@SuppressWarnings("unused")
public class MirrorArrayPlugin extends BaseCustomEntityPlugin {

    public static final String spritePath = "graphics/stations/sms_mirror_array_active1.png";
    private SpriteAPI sprite;
    float time = 0f;
    boolean added = false;
    private IntervalUtil streamInterval = new IntervalUtil(10f, 10f);

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        sprite = Global.getSettings().getSprite(spritePath);
        this.entity = entity;
        if (sprite.getTextureId() <=  0) {
            try {
                Global.getSettings().loadTexture(spritePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (streamInterval == null) {
            streamInterval = new IntervalUtil(10f, 10f);
        }
        streamInterval.advance(amount);
        var emitter = new ParticleBurstEmitter(Global.getSector().getPlayerFleet().getLocation()) {
            @Override
            public SpriteAPI getSprite() {
                return Utils.getLoadedSprite("graphics/ships/aurora_ca.png");
            }
        };
        var emitter2 = new ParticleBurstEmitter(Global.getSector().getPlayerFleet().getLocation()) {
            @Override
            public int getBlendFunc() {
                return GL14.GL_FUNC_REVERSE_SUBTRACT;
            }
        };

        if (streamInterval.intervalElapsed()) {
            emitter2.enableDynamicAnchoring();
            emitter.size = emitter2.size = 50f;
            emitter.life = emitter2.life = 10f;
            emitter.radius = emitter2.radius = 100f;
            emitter.alpha = emitter2.alpha = 1f;
            emitter2.color = Color.RED;
            Particles.stream(emitter, 1, 1, 5f);
            Particles.stream(emitter2, 1, 1, 5f);
        }
        var emitter4 = Particles.initialize(Global.getSector().getPlayerFleet().getLocation());
        emitter4.fadeTime(0.5f, 0.5f, 0.5f, 0.5f);
        emitter4.life(3f, 5f);
        emitter4.size(10f, 25f);
        emitter4.hueShift(-40f, 40f);
        emitter4.color(0.5f, 1f, 0.75f, 1f);
        emitter4.circleOffset(20f, 50f);
        emitter4.revolutionRate(-20f, 20f);
        Particles.burst(emitter4, 4);
    }

    @Override
    public float getRenderRange() {
        return Float.MAX_VALUE;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        // if (layer != CampaignEngineLayers.ABOVE_STATIONS) return;
//        if (sprite == null) return;
//        sprite.setAngle(entity.getFacing()-90f);
//        sprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
//        sprite.setAlphaMult(0.5f);
//        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
    }
}
