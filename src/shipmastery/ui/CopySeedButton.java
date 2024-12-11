package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.plugin.ModPlugin;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.util.ReflectionUtils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CopySeedButton implements CustomUIElement {
    @Override
    public void create(TooltipMakerAPI tooltip) {
        ButtonAPI button = tooltip.addButton("Copy seed", null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, CutStyle.NONE, 100f, 25f, 5f);
        ReflectionUtils.setButtonListener(button, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String seed = (String) Global.getSector().getPersistentData().get(ModPlugin.GENERATION_SEED_KEY);
                clipboard.setContents(new StringSelection(seed == null ? Global.getSector().getPlayerPerson().getId() : seed), null);
            }
        });
    }
}
