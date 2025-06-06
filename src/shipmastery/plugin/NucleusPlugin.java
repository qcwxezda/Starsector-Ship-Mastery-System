package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import particleengine.Particles;
import shipmastery.fx.StarSiphonEmitter;
import shipmastery.util.Strings;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class NucleusPlugin extends BaseCustomEntityPlugin {

    StarSystemAPI containingLocation;
    boolean isHiddenOnMap;
    public final List<JumpPointAPI> jumpPointsInHyper = new ArrayList<>();
    transient List<Pair<PlanetAPI, StarSiphonEmitter>> siphoningStars = new ArrayList<>();
    transient SpriteAPI sprite;
    transient boolean inited = false;

    public void init() {
        // Note: the plugin's default init gets called *AS SOON AS THE ENTITY IS CREATED*,
        // so anything that's set after the constructor call is null!
        // Therefore, need to use our own custom init function.
        sprite = Global.getSettings().getSprite(entity.getCustomEntitySpec().getSpriteName());
        siphoningStars = new ArrayList<>();
        containingLocation = entity.getStarSystem();
        isHiddenOnMap = true;
        for (PlanetAPI star : containingLocation.getPlanets()) {
            if (star.isStar()) {
                var emitter = new StarSiphonEmitter(star, entity.getLocation());
                emitter.enableDynamicAnchoring();
                siphoningStars.add(new Pair<>(star, emitter));
            }
        }
        inited = true;
    }

    @Override
    public void advance(float amount) {
        if (!inited) {
            init();
        }
        var playerLocation = Global.getSector().getPlayerFleet().getContainingLocation();
        // Unhide the system this object is in from the map if the player reaches it
        if (isHiddenOnMap && playerLocation == containingLocation) {
            isHiddenOnMap = false;
            jumpPointsInHyper.forEach(jp -> jp.removeTag(Tags.STAR_HIDDEN_ON_MAP));
        }

        if (playerLocation == containingLocation && !Global.getSector().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.NUCLEUS_SHUT_DOWN)) {
            for (var pair : siphoningStars) {
                if (Misc.random.nextFloat() < 0.1f) {
                    Particles.burst(pair.two, 1);
                }
            }
        }
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (sprite == null || layer != CampaignEngineLayers.ABOVE) return;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        sprite.setAngle(entity.getFacing()-90f);
        sprite.setAlphaMult(0.5f*entity.getSensorContactFaderBrightness());
        sprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
