package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;

import java.util.Set;

public class MasteryGenerator {
    public Class<? extends MasteryEffect> effectClass;
    public String[] params;
    public Set<String> tags;
    public float defaultStrength;
    public int priority;

    public MasteryGenerator(
            Class<? extends MasteryEffect> effectClass,
            String[] params,
            Set<String> tags,
            float defaultStrength,
            int priority) {
        this.effectClass = effectClass;
        this.params = params;
        if (this.params == null || this.params.length == 0) {
            this.params = new String[] {"" + defaultStrength};
        }
        this.tags = tags;
        this.priority = priority;
        this.defaultStrength = defaultStrength;
    }

    public MasteryEffect generateDontInit(ShipHullSpecAPI spec, int level, int index, boolean isOption2)
            throws InstantiationException, IllegalAccessException {
        BaseMasteryEffect effect = (BaseMasteryEffect) effectClass.newInstance();
        effect.setIsOption2(isOption2);
        effect.setPriority(priority);
        effect.setHullSpec(spec);
        effect.setId(MasteryUtils.makeEffectId(effect, level, index));
        effect.setLevel(level);
        effect.setIndex(index);
        effect.setTags(tags);
        return effect;
    }

    public MasteryEffect generate(ShipHullSpecAPI spec, int level, int index, boolean isOption2)
            throws InstantiationException, IllegalAccessException {
        MasteryEffect effect = generateDontInit(spec, level, index, isOption2);
        return effect.init(params);
    }
}
