package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import shipmastery.ShipMastery;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.stats.ShipStat;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ModifyStatsEffect extends BaseMasteryEffect {
    Map<ShipStat, Float> amounts = new LinkedHashMap<>();
    @Override
    public void init(String... args) {
        super.init(args);
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
        }
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
            params.add(item.one.name);
            params.add(getAmountString(item.one, item.two));
            colors.add(null);
            colors.add(!(item.one.defaultAmount < 0)? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
        }

        for (Pair<ShipStat, Float> item : negativeAmounts) {
            descriptionListNeg.add(Strings.Descriptions.StatListItem);
            params.add(item.one.name);
            params.add(getAmountString(item.one, item.two));
            colors.add(null);
            colors.add(item.one.defaultAmount < 0? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
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
}
