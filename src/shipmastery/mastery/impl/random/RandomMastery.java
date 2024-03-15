package shipmastery.mastery.impl.random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.ShipMastery;
import shipmastery.data.MasteryGenerator;
import shipmastery.data.MasteryInfo;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;

import java.util.*;

public class RandomMastery extends BaseMasteryEffect {
    public MasteryEffect init(String... args) {
        long seed = makeSeed();
        try {
            return init(seed, args);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return null;
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return null;
    }

    protected final MasteryEffect init(long seed, String... args)
            throws InstantiationException, IllegalAccessException {
        super.init(args);
        ShipHullSpecAPI spec = getHullSpec();
        Set<Class<?>> uniqueDontRepeat = new HashSet<>();
        int maxTier = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            List<MasteryEffect> effects = new ArrayList<>(ShipMastery.getMasteryEffects(spec, i, false));
            effects.addAll(ShipMastery.getMasteryEffects(spec, i, true));
            for (MasteryEffect effect : effects) {
                // Don't want same effects in a single level even if not unique
                if (effect != null && (effect.hasTag(MasteryTags.UNIQUE) || i == level)) {
                    uniqueDontRepeat.add(effect.getClass());
                }
            }
            // Check the generators for masteries that haven't yet been generated
            List<MasteryGenerator> generators = new ArrayList<>(ShipMastery.getGenerators(spec, i, false));
            generators.addAll(ShipMastery.getGenerators(spec, i ,true));
            for (MasteryGenerator generator : generators) {
                Set<String> tags = new HashSet<>(Arrays.asList(generator.tags));
                if (i == level || tags.contains(MasteryTags.UNIQUE)) {
                    uniqueDontRepeat.add(generator.effectClass);
                }
            }
        }

        WeightedRandomPicker<MasteryInfo> effectPicker = new WeightedRandomPicker<>();
        effectPicker.setRandom(new Random(seed));
        for (String str : ShipMastery.getAllMasteryNames()) {
            MasteryInfo info = ShipMastery.getMasteryInfo(str);
            if (uniqueDontRepeat.contains(info.effectClass)) continue;
            if (info.tier > maxTier) continue;
            // TODO: optimize this by storing selection weights in a map somewhere
            MasteryGenerator dummyGenerator = new MasteryGenerator(
                    info.effectClass,
                    null,
                    info.tags,
                    info.defaultStrength,
                    info.priority
            );
            MasteryEffect dummy = dummyGenerator.generateDontInit(spec, level, index, false);
            Float weight = dummy.getSelectionWeight(spec);
            if (weight != null && weight > 0f) {
                effectPicker.add(info, dummy.getSelectionWeight(spec));
            }
        }

        MasteryEffect effect;
        List<String> additionalParams;
        List<String> params = new ArrayList<>();

        do {
            MasteryInfo selected = effectPicker.pickAndRemove();

            params.add("" + getStrength((PersonAPI) null) * selected.defaultStrength);

            MasteryGenerator generator = new MasteryGenerator(
                    selected.effectClass,
                    null,
                    selected.tags,
                    selected.defaultStrength,
                    selected.priority);
            effect = generator.generateDontInit(getHullSpec(), getLevel(), getIndex(), isOption2());
            additionalParams = effect.generateRandomArgs(getHullSpec(), maxTier, makeSeed());
        } while (additionalParams == null);

        params.addAll(additionalParams);
        effect.init(params.toArray(new String[0]));
        return effect;
    }

    long makeSeed() {
        return (getId() + "_" + level + "_" + index + "_" + getHullSpec().getHullId() + "_" + Global.getSector().getPlayerPerson().getId()).hashCode();
    }
}
