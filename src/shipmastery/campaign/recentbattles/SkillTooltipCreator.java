package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.loading.SkillSpec;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SkillTooltipCreator {
    public static Method addTooltipAboveMethod;
    public static Logger logger = Logger.getLogger(SkillTooltipCreator.class);

    @SuppressWarnings("unused")
    public static void addSkillTooltip(UIComponentAPI component, SkillSpecAPI skillSpec, PersonAPI officer) {
        if (addTooltipAboveMethod == null) {
            for (Method method : StandardTooltipV2Expandable.class.getDeclaredMethods()) {
                if ("addTooltipAbove".equals(method.getName()) && method.getParameterTypes().length == 2) {
                    addTooltipAboveMethod = method;
                    break;
                }
            }
        }

        if (addTooltipAboveMethod == null) {
            return;
        }

        try {
            addTooltipAboveMethod.invoke(null, component, StandardTooltipV2.createSkillTooltip(
                    (SkillSpec) skillSpec,
                    (CharacterStats) officer.getStats(),
                    800f,
                    300f,
                    true,
                    false,
                    0,
                    null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("Failed to add skill tooltip", e);
        }
    }
}
