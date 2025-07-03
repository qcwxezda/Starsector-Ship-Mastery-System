package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.achievements.ManyRerolls;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.data.MasteryGenerator;
import shipmastery.mastery.impl.random.RandomMastery;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RerollButton extends ButtonWithItemSelection<RerollButton.RerollItem> {

    protected ShipHullSpecAPI spec;

    public record RerollItem(int level, boolean isRandomized, boolean isActive) implements Item<RerollItem> {
        @Override
        public String getId() {
            return "" + level;
        }

        @Override
        public String getDisplayName() {
            return "" + level;
        }

        @Override
        public RerollItem getItem() {
            return this;
        }
    }

    public RerollButton(ShipHullSpecAPI spec) {
        super("graphics/icons/ui/sms_reroll_icon_green.png", true);
        this.spec = spec;
    }

    @Override
    public void onClick() {
        super.onClick();

        // Disable buttons that do not correspond to randomized or unselected masteries
        buttons.stream()
                .filter(x -> {
                    var item = (RerollItem) x.getCustomData();
                    return !item.isRandomized() || item.isActive();
                })
                .forEach(x -> x.setEnabled(false));

        // Enable all by default
        selectAllButton.onClick();
    }

    @Override
    protected Collection<Item<RerollItem>> getEligibleItems() {
        Set<Integer> inactiveLevels = new HashSet<>();
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            inactiveLevels.add(i);
        }
        inactiveLevels.removeAll(ShipMastery.getPlayerActiveMasteriesCopy(spec).keySet());

        Set<Integer> randomizedLevels = new HashSet<>();
        for (int i  = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            List<String> optionIds = ShipMastery.getMasteryOptionIds(spec, i);
            List<MasteryGenerator> gens = new ArrayList<>();
            for (String id : optionIds) {
                gens.addAll(ShipMastery.getGenerators(spec, i, id));
            }
            boolean affected = false;
            for (MasteryGenerator gen : gens) {
                if (RandomMastery.class.isAssignableFrom(gen.effectClass)) {
                    affected = true;
                    break;
                }
            }
            if (affected) {
                randomizedLevels.add(i);
            }
        }

        return IntStream.rangeClosed(1, ShipMastery.getMaxMasteryLevel(spec))
                .boxed()
                .map(x -> new RerollItem(x, randomizedLevels.contains(x), !inactiveLevels.contains(x)))
                .collect(Collectors.toList());
    }

    @Override
    protected TooltipMakerAPI.TooltipCreator getPerItemTooltipCreator(Item<RerollItem> item) {
        boolean active = item.getItem().isActive();
        boolean randomized = item.getItem().isRandomized();
        if (!active && randomized) return null;

        return new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 500f;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (!item.getItem().isRandomized()) {
                    tooltip.addPara(Strings.MasteryPanel.cantRerollNotRandomized, 10f);
                }
                else if (item.getItem().isActive()) {
                    tooltip.addPara(Strings.MasteryPanel.cantRerollActive, 10f);
                }
            }
        };
    }

    @Override
    protected String getTitle() {
        return Strings.MasteryPanel.rerollMasteries;
    }

    @Override
    protected Color getButtonTextColor() {
        return Misc.getButtonTextColor();
    }

    @Override
    protected void applyEffects() {
        Set<Integer> levels = selectedItems.stream().map(x -> x.getItem().level).collect(Collectors.toSet());
        // Increment reroll count
        //noinspection unchecked
        Map<String, List<Set<Integer>>> rerollMap = (Map<String, List<Set<Integer>>>) Global.getSector().getPersistentData().get(ShipMastery.REROLL_SEQUENCE_MAP);
        if (rerollMap == null) {
            rerollMap = new HashMap<>();
            Global.getSector().getPersistentData().put(ShipMastery.REROLL_SEQUENCE_MAP, rerollMap);
        }
        List<Set<Integer>> rerollSequence = rerollMap.computeIfAbsent(spec.getHullId(), k -> new LinkedList<>());

        rerollSequence.add(levels);
        ShipMastery.addRerolledSpecThisSave(spec);

        try {
            ShipMastery.generateMasteries(spec, levels, rerollSequence.size(), true);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Check for achievement completion
        if (rerollSequence.size() >= ManyRerolls.REROLLS_NEEDED) {
            UnlockAchievementAction.unlockWhenUnpaused(ManyRerolls.class);
        }
    }

    @Override
    protected boolean hasCostLabel() {
        return false;
    }

    @Override
    protected float getBxpFraction() {
        return 0f;
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.rerollMasteries;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.rerollTooltip, 10f);
    }

    @Override
    protected String getCostDescriptionFormat() {
        return null;
    }

    @Override
    protected String[] getCostDescriptionArgs() {
        return null;
    }

    @Override
    protected String getUsedSPDescription() {
        return null;
    }

    @Override
    protected float getBaseCost() {
        return 0f;
    }
}
