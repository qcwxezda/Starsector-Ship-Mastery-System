
package shipmastery.hullmods.npc;

import com.fs.starfarer.api.combat.BaseHullMod;
import shipmastery.config.Settings;

import java.awt.*;

public class NPCIndicator6 extends BaseHullMod {
    @Override
    public Color getNameColor() {
        return Settings.MASTERY_COLOR;
    }
}
