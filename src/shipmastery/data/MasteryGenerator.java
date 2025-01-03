package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.impl.random.RandomMastery;
import shipmastery.util.MasteryUtils;

import java.util.Set;

public class MasteryGenerator {
    public Class<? extends MasteryEffect> effectClass;
    public String[] params;
    public Set<String> tags;
    public float defaultStrength;
    public int priority;

    public MasteryGenerator(
            MasteryInfo info,
            String[] params) {
        effectClass = info.effectClass;
        this.params = params;
        tags = info.tags;
        priority = info.priority;
        defaultStrength = info.defaultStrength;
        if (this.params == null || this.params.length == 0) {
            this.params = new String[] {"" + defaultStrength};
        }
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

    public MasteryEffect generate(ShipHullSpecAPI spec, int level, int index, boolean isOption2, int seedPrefix, Set<Class<?>> avoidWhenGeneratingRandom)
            throws InstantiationException, IllegalAccessException {
        MasteryEffect effect = generateDontInit(spec, level, index, isOption2);

        if (effect instanceof RandomMastery) {
            ((RandomMastery) effect).setSeedPrefix(seedPrefix);
            ((RandomMastery) effect).setAvoidWhenGenerating(avoidWhenGeneratingRandom);
        }

        return effect.init(params);
    }
}
