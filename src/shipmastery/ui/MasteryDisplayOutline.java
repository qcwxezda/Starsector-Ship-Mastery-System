package shipmastery.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class MasteryDisplayOutline implements CustomUIElement {

    final float w, h;

    public MasteryDisplayOutline(float width, float height) {
        w = width;
        h = height;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        ButtonAPI containerOutline =
                tooltip.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                 Misc.getBrightPlayerColor(), w - 5f, h - 5f, 0f);
        containerOutline.setClickable(false);
        containerOutline.setGlowBrightness(0f);
        containerOutline.setMouseOverSound(null);
    }
}
