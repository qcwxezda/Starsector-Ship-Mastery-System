package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.MasteryUtils;

public class ShipDisplay implements CustomUIElement {
    final ShipHullSpecAPI spec;
    final float size;

    public ShipDisplay(ShipHullSpecAPI spec, float size) {
        this.spec = spec;
        this.size = size;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        String spriteName = spec.getSpriteName();
        SpriteAPI sprite = Global.getSettings().getSprite(spriteName);
        float imageSize = Math.max(sprite.getWidth(), sprite.getHeight());
        imageSize = Math.min(imageSize, size - 10f);
        tooltip.setParaOrbitronVeryLarge();
        int enhanceCount = MasteryUtils.getEnhanceCount(spec);
        String enhanceStr = String.format("(+%s)", enhanceCount);
        String nameStr = (enhanceCount <= 0 ? "" : enhanceStr) + " " + spec.getHullName();
        LabelAPI test = Global.getSettings().createLabel(nameStr, Fonts.ORBITRON_24AABOLD);
        float width = test.computeTextWidth(nameStr);
        if (width+15f > size) {
            test = Global.getSettings().createLabel(nameStr, Fonts.ORBITRON_20AABOLD);
            tooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
            width = test.computeTextWidth(nameStr);
            if (width+15f > size) {
                test = Global.getSettings().createLabel(nameStr, Fonts.ORBITRON_16);
                tooltip.setParaFont(Fonts.ORBITRON_16);
                width = test.computeTextWidth(nameStr);
                if (width+15f > size) {
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                }
            }
        }

        tooltip.addPara(nameStr, -10f, Misc.getStoryBrightColor(), enhanceStr).setAlignment(Alignment.MID);
        ButtonAPI outline = tooltip.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                        Misc.getBrightPlayerColor(), size - 5f,
                                                        size - 5f, 10f);
        outline.setClickable(false);
        outline.setGlowBrightness(0.3f);
        outline.setMouseOverSound(null);
        tooltip.addImage(
                spec.getSpriteName(), size, imageSize, -imageSize - (size - imageSize) / 2f);
    }
}
