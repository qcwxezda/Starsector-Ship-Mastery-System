package shipmastery.stats;

public interface StatTags {
    /** Set if random selection should use flat mods instead of percent/mult mods */
    String TAG_MODIFY_FLAT = "modify_flat";
    /** If enabled, values will be rounded down, values in (0, 1] are set to 1, and values in [-1, 0) are set to -1. */
    String TAG_REQUIRE_INTEGER = "require_integer";
    /** Display flat modification as percentage. No effect on mult modifications as they are already displayed as percent. */
    String TAG_DISPLAY_AS_PERCENT = "display_as_percent";
}
