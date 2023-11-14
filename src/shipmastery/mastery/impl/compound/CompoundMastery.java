package shipmastery.mastery.impl.compound;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompoundMastery extends BaseMasteryEffect {

    List<MasteryEffect> childEffects = new ArrayList<>();

    /**
     * {@code args} takes the following format: <br>
     * {@code [strength_0] {[count_1] [masteryId_1] [args_1]} {[count_2] [masteryId_2] [args_2]} ... <br>
     * All child effects are scaled by {@code strength_0}.
     */
    @Override
    public void init(String... args) {
        super.init(args);

        try {
            int count;
            for (int i = 1; i < args.length; i += 2 + count) {
                count = Integer.parseInt(args[i]) - 1;

                String id = args[i + 1];

                String[] subArgs = new String[count];
                System.arraycopy(args, i+2, subArgs, 0, count);

                Class<?> cls = ShipMastery.getEffectClass(id);
                MasteryEffect subEffect = (MasteryEffect) cls.newInstance();
                subEffect.init(subArgs);
                subEffect.setStrength(subEffect.getStrength() * getStrength());
                childEffects.add(subEffect);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyEffectsOnBeginRefit(final ShipHullSpecAPI spec, final String id) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.applyEffectsOnBeginRefit(spec, makeId(id, effect, index));
            }
        });
    }

    @Override
    public void unapplyEffectsOnEndRefit(final ShipHullSpecAPI spec, final String id) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.unapplyEffectsOnEndRefit(spec, makeId(id, effect, index));
            }
        });
    }

    @Override
    public boolean isAutoActivateWhenUnlocked(ShipHullSpecAPI spec) {
        for (MasteryEffect child : childEffects) {
            if (!child.isAutoActivateWhenUnlocked(spec)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canBeDeactivated() {
        for (MasteryEffect child : childEffects) {
            if (!child.canBeDeactivated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isUniqueEffect() {
        for (MasteryEffect child : childEffects) {
            if (child.isUniqueEffect()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats, final String id) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.applyEffectsBeforeShipCreation(hullSize, stats, makeId(id, effect, index));
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.applyEffectsAfterShipCreation(ship, makeId(id, effect, index));
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship, makeId(id, effect, index));
            }
        });
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        for (MasteryEffect child : childEffects) {
            if (!child.isApplicableToHull(spec)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Integer getSelectionTier() {
        int max = 0;
        for (MasteryEffect child : childEffects) {
            Integer i = child.getSelectionTier();
            if (i == null) {
                return null;
            }
            max = Math.max(i, max);
        }
        return max;
    }

    @Override
    public void advanceInCampaign(final FleetMemberAPI member, final float amount) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.advanceInCampaign(member, amount);
            }
        });
    }

    @Override
    public void advanceInCombat(final ShipAPI ship, final float amount) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.advanceInCombat(ship, amount);
            }
        });
    }

    @Override
    public void addPostDescriptionSection(final TooltipMakerAPI tooltip) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.addPostDescriptionSection(tooltip);
            }
        });
    }

    @Override
    public void addTooltip(final TooltipMakerAPI tooltip) {
        applyAllChildEffects(new CompoundMasteryAction() {
            @Override
            public void perform(MasteryEffect effect, int index) {
                effect.addTooltip(tooltip);
            }
        });
    }

    @Override
    public boolean hasTooltip() {
        for (MasteryEffect child : childEffects) {
            if (child.hasTooltip()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean alwaysShowDescription() {
        for (MasteryEffect child : childEffects) {
            if (!child.alwaysShowDescription()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MasteryDescription getDescription() {
        StringBuilder combined = new StringBuilder();
        List<Object> combinedParams = new ArrayList<>();
        List<Color> combinedColors = new ArrayList<>();
        for (int j = 0; j < childEffects.size(); j++) {
            MasteryEffect childEffect = childEffects.get(j);
            MasteryDescription subDescription = childEffect.getDescription();
            combined.append(subDescription.text);
            if (j < childEffects.size() - 1) {
                combined.append("\n\n");
            }
            combinedParams.addAll(Arrays.asList(subDescription.params));
            Color[] subColors = subDescription.colors;
            if (subColors == null) {
                for (int i = 0; i < subDescription.params.length; i++) {
                    combinedColors.add(Misc.getTextColor());
                }
            } else if (subColors.length == 1) {
                for (int i = 0; i < subDescription.params.length; i++) {
                    combinedColors.add(subDescription.colors[0]);
                }
            } else {
                combinedColors.addAll(Arrays.asList(subDescription.colors));
            }
        }
        return new MasteryDescription(
                combined.toString(),
                combinedParams.toArray(new Object[0]),
                combinedColors.toArray(new Color[0]));
    }

    void applyAllChildEffects(CompoundMasteryAction action) {
        for (int i = 0; i < childEffects.size(); i++) {
            action.perform(childEffects.get(i), i);
        }
    }

    interface CompoundMasteryAction {
        void perform(MasteryEffect effect, int index);
    }

    String makeId(String baseId, MasteryEffect effect, int index) {
        return baseId + "_" + ShipMastery.getId(effect.getClass()) + "_" + index;
    }
}
