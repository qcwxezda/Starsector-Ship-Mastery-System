package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.plugin.ModPlugin;
import shipmastery.stats.ShipStat;
import shipmastery.stats.StatTags;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.*;

public abstract class ModifyStatsEffect extends BaseMasteryEffect {
    final Map<ShipStat, Float> amounts = new LinkedHashMap<>();
    @Override
    public MasteryEffect postInit(String... args) {
        for (int i = 1; i < args.length; i++) {
            String id = args[i];
            ShipStat stat = ShipMastery.getStatParams(id);
            if (stat == null) {
                throw new RuntimeException("Unknown stat id: " + id);
            }
            float amount;
            try {
                amount = Float.parseFloat(args[i + 1]);
                i++;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                amount = stat.defaultAmount;
            }
            Float existing = amounts.get(stat);
            amounts.put(stat, existing == null ? amount : existing + amount);
            addTags(stat.tags.toArray(new String[0]));
        }
        return this;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        List<Pair<ShipStat, Float>> positiveAmounts = new ArrayList<>();
        List<Pair<ShipStat, Float>> negativeAmounts = new ArrayList<>();

        for (Map.Entry<ShipStat, Float> entry : amounts.entrySet()) {
            ShipStat stat = entry.getKey();
            float amount = getModifiedAmount(stat, getStrengthForPlayer() * entry.getValue());
            if (amount < 0f) {
                negativeAmounts.add(new Pair<>(stat, amount));
            }
            else {
                positiveAmounts.add(new Pair<>(stat, amount));
            }
        }

        List<String> descriptionListPos = new ArrayList<>();
        List<String> descriptionListNeg = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        List<Color> colors = new ArrayList<>();

        for (Pair<ShipStat, Float> item : positiveAmounts) {
            descriptionListPos.add(Strings.Descriptions.StatListItem);
            params.add(item.one.description);
            params.add(getAmountString(item.one, item.two));
            colors.add(null);
            colors.add(!(item.one.defaultAmount < 0)? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR);
        }

        for (Pair<ShipStat, Float> item : negativeAmounts) {
            descriptionListNeg.add(Strings.Descriptions.StatListItem);
            params.add(item.one.description);
            params.add(getAmountString(item.one, item.two));
            colors.add(null);
            colors.add(item.one.defaultAmount < 0? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR);
        }

        StringBuilder sb = new StringBuilder();

        if (!positiveAmounts.isEmpty()) {
            sb.append(Strings.Descriptions.StatIncrease);
            sb.append(Utils.joinStringList(descriptionListPos));
            sb.append(".");
        }

        if (!negativeAmounts.isEmpty()) {
            if (!positiveAmounts.isEmpty()) {
                sb.append("\n");
            }
            sb.append(Strings.Descriptions.StatDecrease);
            sb.append(Utils.joinStringList(descriptionListNeg));
            sb.append(".");
        }
        return MasteryDescription.init(sb.toString()).params(params.toArray(new Object[0])).colors(colors.toArray(new Color[0]));
    }

    abstract float getModifiedAmount(ShipStat stat, float amount);
    abstract String getAmountString(ShipStat stat, float modifiedAmount);

    protected final List<String> generateRandomArgs(ShipHullSpecAPI spec, int maxTier, long seed, boolean modifyFlat) {
        WeightedRandomPicker<ShipStat> picker = new WeightedRandomPicker<>();
        picker.setRandom(new Random(seed));
        outer:
        for (String name : ShipMastery.getAllStatNames()) {
            ShipStat stat = ShipMastery.getStatParams(name);
            if (modifyFlat && !stat.tags.contains(StatTags.TAG_MODIFY_FLAT)) continue;
            if (!modifyFlat && stat.tags.contains(StatTags.TAG_MODIFY_FLAT)) continue;
            if (stat.tier > maxTier) continue;
            List<String[]> usedArgs = getAllUsedArgs();
            for (String[] args : usedArgs) {
                // Avoid duplicate stat buffs
                if (args.length >= 2 && args[1].equals(stat.id)) {
                    continue outer;
                }
            }

            Float weight = stat.getSelectionWeight(spec);
            boolean randomMode = (boolean) Global.getSector().getPersistentData().get(ModPlugin.RANDOM_MODE_KEY);
            if (weight != null && (weight > 0f || randomMode)) {
                // try to prioritize higher tier stats, if applicable
                picker.add(stat, randomMode ? Math.max(1f, weight) : weight * stat.tier * stat.tier);
            }
        }

        if (picker.isEmpty()) return null;
        ShipStat selected = picker.pickAndRemove();
        return Collections.singletonList(selected.id);
    }
}
