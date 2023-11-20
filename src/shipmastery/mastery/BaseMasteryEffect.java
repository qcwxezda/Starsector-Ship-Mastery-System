package shipmastery.mastery;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMasteryEffect implements MasteryEffect {

    private MutableStat strength;
    private final Set<String> tags = new HashSet<>();
    private int tier = 1;
    private int priority = 0;
    private ShipHullSpecAPI spec;


    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {}

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {}

    @Override
    public void onActivate(String id) {}

    @Override
    public void onDeactivate(String id) {}

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) throw new RuntimeException("BaseMasteryEffect init called with null or 0 args");

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
    public Float getSelectionWeight() {
        return 1f;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {}

    @Override
    public void addTooltipIfHasTooltipTag(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {}

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
    public final ShipHullSpecAPI getHullSpec() {
        return spec;
    }

    public final void setHullSpec(ShipHullSpecAPI spec) {
        if (this.spec != null) {
            throw new RuntimeException("Changing the hull spec of a mastery effect is not allowed");
        }
        this.spec = spec;
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

    public final void setPriority(int priority) {
        this.priority = priority;
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
