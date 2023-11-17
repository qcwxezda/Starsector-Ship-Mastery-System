package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMasteryEffect implements MasteryEffect {

    private MutableStat strength;
    private final Set<String> tags = new HashSet<>();
    private int tier = 1;
    private final int priority = 0;


    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onActivate(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onDeactivate(ShipHullSpecAPI spec, String id) {}

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) throw new RuntimeException("BaseMasteryEffect called with null or 0 args");

        try {
            strength = new MutableStat(Float.parseFloat(args[0]));
        } catch (NumberFormatException e) {
            throw new RuntimeException("First argument in mastery params list must be its strength", e);
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {}

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {}

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        return 1f;
    }

    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public void addTooltipIfHasTooltipTag(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public final void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    @Override
    public final void removeTags(String... tags) {
        for (String tag : tags) {
            this.tags.remove(tag);
        }
    }

    @Override
    public final boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    @Override
    public final int getTier() {
        return tier;
    }

    @Override
    public final void setTier(int tier) {
        this.tier = tier;
    }

    @Override
    public final int getPriority() {
        return priority;
    }

    @Override
    public final void modifyStrengthMultiplicative(String id, float fraction) {
        strength.modifyMult(id, fraction);
    }

    @Override
    public final void unmodifyStrength(String id) {
        strength.unmodify(id);
    }

    @Override
    public final void modifyStrengthAdditive(String id, float fraction) {
        strength.modifyPercent(id, 100f*(fraction - 1f));
    }

    @Override
    public final float getStrength() {
        return strength.getModifiedValue();
    }
}
