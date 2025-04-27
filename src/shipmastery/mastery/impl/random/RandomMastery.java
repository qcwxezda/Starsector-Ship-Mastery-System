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
import shipmastery.plugin.ModPlugin;
import shipmastery.util.MasteryUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomMastery extends BaseMasteryEffect {

    protected int seedPrefix = 0;
    // So that the regenerate option avoids picking the same things as previous.
    protected Set<Class<?>> avoidWhenGenerating = new HashSet<>();
    protected Set<String> paramsToAvoidWhenGenerating = new HashSet<>();

    @Override
    public MasteryEffect postInit(String... args) {
        long seed = makeSeed();
        try {
            return postInit(seed, args);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
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

    protected final MasteryEffect postInit(long seed, String... args)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException {
        ShipHullSpecAPI spec = getHullSpec();
        Set<Class<?>> uniqueDontRepeat = new HashSet<>();
        Set<Class<?>> seenNotUnique = new HashSet<>();
        int maxTier = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            List<MasteryEffect> effects = new ArrayList<>();
            for (String id : ShipMastery.getMasteryOptionIds(spec, i)) {
                effects.addAll(ShipMastery.getMasteryEffects(spec, i, id));
            }
            for (MasteryEffect effect : effects) {
                // Don't want same effects in a single level even if not unique
                if (effect != null && (effect.hasTag(MasteryTags.UNIQUE) || (i == level && !effect.hasTag(MasteryTags.VARYING)))) {
                    uniqueDontRepeat.add(effect.getClass());
                } else if (effect != null && !effect.hasTag(MasteryTags.VARYING)) {
                    seenNotUnique.add(effect.getClass());
                }
            }
            // Check the generators for masteries that haven't yet been generated
            List<MasteryGenerator> generators = new ArrayList<>();
            for (String id : ShipMastery.getMasteryOptionIds(spec, i)) {
                generators.addAll(ShipMastery.getGenerators(spec, i, id));
            }
            for (MasteryGenerator generator : generators) {
                Set<String> tags = generator.tags;
                if ((i == level && !tags.contains(MasteryTags.VARYING)) || tags.contains(MasteryTags.UNIQUE)) {
                    uniqueDontRepeat.add(generator.effectClass);
                } else if (!tags.contains(MasteryTags.VARYING)) {
                    seenNotUnique.add(generator.effectClass);
                }
            }
        }

        WeightedRandomPicker<MasteryInfo> effectPicker = new WeightedRandomPicker<>();
        effectPicker.setRandom(new Random(seed));
        for (String str : ShipMastery.getAllMasteryNames()) {
            MasteryInfo info = ShipMastery.getMasteryInfo(str);
            if (uniqueDontRepeat.contains(info.effectClass)) continue;
            if (info.tier > maxTier) continue;
            if (info.tags.contains(MasteryTags.COMBAT) && spec.isCivilianNonCarrier()) continue;

            Float weight = ShipMastery.getCachedSelectionWeight(str, spec);
            Boolean randomMode = (Boolean) Global.getSector().getPersistentData().get(ModPlugin.RANDOM_MODE_KEY);
            if (randomMode == null) randomMode = false;
            if (weight != null && (weight > 0f || randomMode)) {
                // try to prioritize higher tier masteries, if they are applicable
                float tier = info.tags.contains(MasteryTags.SCALE_SELECTION_WEIGHT) ? maxTier : info.tier;
                float tierMult = tier * tier;
                // strongly reduce weight if effect has been seen before, even if not unique
                if (seenNotUnique.contains(info.effectClass)) weight *= 0.01f;
                if (avoidWhenGenerating.contains(info.effectClass) && !info.tags.contains(MasteryTags.VARYING)) weight *= 0.000001f;
                // strongly avoid low-tier stuff
                if (maxTier - tier >= 2) tierMult *= 0.01f;
                effectPicker.add(info, randomMode ? Math.max(1f, weight) : weight * tierMult);
            }
        }

        MasteryEffect effect;
        List<String> additionalParams = null;
        List<String> params = new ArrayList<>();

        int tries = 10;
        do {
            // Fallback if there's literally nothing to pick
            if (effectPicker.isEmpty()) {
                MasteryGenerator generator = new MasteryGenerator(ShipMastery.getMasteryInfo("ModifyStatsMult"), new String[] {"0.1", "FluxCapacity"});
                return generator.generate(getHullSpec(), getLevel(), getIndex(), getOptionId(), 0, new HashSet<>(), new HashSet<>());
            }
            tries++;

            MasteryInfo selected = effectPicker.pickAndRemove();

            params.clear();
            params.add("" + getStrength((PersonAPI) null) * selected.defaultStrength);

            MasteryGenerator generator = new MasteryGenerator(selected, null);
            effect = generator.generateDontInit(getHullSpec(), getLevel(), getIndex(), getOptionId());
            // Try a few times to get something not in the seen params list
            for (int i = 0; i < 4; i++) {
                additionalParams = effect.generateRandomArgs(getHullSpec(), maxTier, makeSeed() + 12335231*i);
                if (additionalParams == null) break;
                boolean notSeen = true;
                for (String param : paramsToAvoidWhenGenerating) {
                    if (additionalParams.contains(param)) {
                        notSeen = false;
                        break;
                    }
                }
                if (notSeen) break;
            }
        } while (additionalParams == null && tries > 0);

        // Fallback if there's literally nothing to pick
        if (additionalParams == null) {
            MasteryGenerator generator = new MasteryGenerator(ShipMastery.getMasteryInfo("ModifyStatsMult"), new String[] {"0.1", "FluxCapacity"});
            return generator.generate(getHullSpec(), getLevel(), getIndex(), getOptionId(), 0, new HashSet<>(), new HashSet<>());
        }

        params.addAll(additionalParams);
        return effect.init(params.toArray(new String[0]));
    }

    public void setAvoidWhenGenerating(Set<Class<?>> classesToAvoid) {
        avoidWhenGenerating = classesToAvoid;
    }

    public void setParamsToAvoidWhenGenerating(Set<String> paramsToAvoid) {
        paramsToAvoidWhenGenerating = paramsToAvoid;
    }

    public void setSeedPrefix(int prefix) {
        seedPrefix = prefix;
    }

    long makeSeed() {
        return ((seedPrefix*17) + "_" +
                getId() + "_" +
                level + "_" +
                index + "_" +
                getOptionId() + "_" +
                getHullSpec().getHullId() + "_" + MasteryUtils.getRandomMasterySeed()).hashCode();
    }
}
