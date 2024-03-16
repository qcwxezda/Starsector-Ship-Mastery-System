package shipmastery.data;

import shipmastery.mastery.MasteryEffect;

import java.util.Set;

public class MasteryInfo {
    public Set<String> tags;
    public float defaultStrength;
    public int priority;
    public int tier;
    public Class<? extends MasteryEffect> effectClass;
}
