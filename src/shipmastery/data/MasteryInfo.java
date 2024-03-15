package shipmastery.data;

import shipmastery.mastery.MasteryEffect;

public class MasteryInfo {
    public String[] tags;
    public float defaultStrength;
    public int priority;
    public int tier;
    public Class<? extends MasteryEffect> effectClass;
}
