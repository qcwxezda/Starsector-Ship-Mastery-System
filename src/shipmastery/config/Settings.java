package shipmastery.config;

import java.awt.*;

public abstract class Settings {
    public static Color MASTERY_COLOR = new Color(96, 192, 255);
    public static float DOUBLE_CLICK_INTERVAL = 0.75f;

    /** From 0-1, roughly the percentage of ships in NPC fleets that will have masteries */
    public static float NPC_MASTERY_DENSITY = 1f;

    /** From 0-1, defines the mastery level distribution among hulls with masteries (0 = all level 1, 1 = all max level) */
    public static float NPC_MASTERY_QUALITY = 1f;

    /** By default, the maximum mastery level any ship hull can have for an NPC fleet is the officer level of the commander */
    public static int NPC_MASTERY_MAX_LEVEL_MODIFIER = 0;

    /** Modifies the max possible mastery level of the NPC flagship's hull spec by this much */
    public static int NPC_MASTERY_FLAGSHIP_BONUS = 3;

    public static int MAX_CACHED_COMMANDERS = 100;
}
