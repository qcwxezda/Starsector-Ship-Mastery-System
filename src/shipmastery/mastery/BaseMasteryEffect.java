package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMasteryEffect implements MasteryEffect {

    private float strength = 1f;
    private final Set<String> tags = new HashSet<>();
    private int tier = 1;
    private float weight = 1f;
    private int priority = 0;


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
        if (args == null || args.length == 0) return;

        try {
            strength = Float.parseFloat(args[0]);
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
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return true;
    }

    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public void addTooltip(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public final Set<String> getTags() {
        return tags;
    }

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
    public final float getWeight() {
        return weight;
    }

    @Override
    public final int getPriority() {
        return priority;
    }

    @Override
    public final void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public final void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public final float getStrength() {
        return strength;
    }

    @Override
    public final void setStrength(float strength) {
        this.strength = strength;
    }
}
