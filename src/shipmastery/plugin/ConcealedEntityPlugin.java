package shipmastery.plugin;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import particleengine.Utils;

@SuppressWarnings("unused")
public class ConcealedEntityPlugin extends BaseCustomEntityPlugin {

    FaderUtil fader = new FaderUtil(Misc.random.nextFloat(), 10f, 10f, true, true);
    public static final float maxAlpha = 0.25f;
    transient SpriteAPI sprite;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        String spriteName = entity.getCustomEntitySpec().getSpriteName();
        String prefix = spriteName.substring(0, spriteName.indexOf('.'));
        String fullAlphaName = prefix + "_fullalpha.png";
        sprite = Utils.getLoadedSprite(fullAlphaName);
        fader.fadeIn();

        if (sprite == null) {
            Logger.getLogger(ConcealedEntityPlugin.class).warn(String.format("Failed to load full alpha sprite at [%s]", fullAlphaName));
        }
    }

    @Override
    public void advance(float amount) {
        fader.advance(amount);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (sprite == null) return;
        sprite.setAngle(entity.getFacing()-90f);
        sprite.setSize(entity.getCustomEntitySpec().getSpriteWidth(), entity.getCustomEntitySpec().getSpriteHeight());
        sprite.setAlphaMult(fader.getBrightness() * maxAlpha);
        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
    }
}
