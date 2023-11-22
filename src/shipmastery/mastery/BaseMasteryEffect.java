package shipmastery.mastery;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMasteryEffect implements MasteryEffect {

    private MutableStat strength;
    private final Set<String> tags = new HashSet<>();
    private int priority = 0;
    private ShipHullSpecAPI spec;
    private String ID = null;
    /** Same as id but can be accessed directly without a getter */
    protected String id = null;

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {}

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {}

    @Override
    public void onActivate() {}

    @Override
    public void onDeactivate() {}

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) throw new RuntimeException("BaseMasteryEffect init called with null or 0 args");

        try {
            strength = new MutableStat(Float.parseFloat(args[0]));
        } catch (NumberFormatException e) {
            throw new RuntimeException("First argument in mastery params list must be its strength", e);
        }
        id = ID;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {}

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {}

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship) {}

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
    public final int getPriority() {
        return priority;
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public final void modifyStrengthMultiplicative(float fraction) {
        strength.modifyMult(id, fraction);
    }

    @Override
    public final void unmodifyStrength() {
        strength.unmodify(id);
    }


    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {}

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {}

    @Override
    public final void modifyStrengthAdditive(float fraction) {
        strength.modifyPercent(id, 100f*(fraction - 1f));
    }

    @Override
    public final float getStrength() {
        return strength.getModifiedValue();
    }

    @Override
    public String getId() {
        return ID;
    }

    public final void setId(String id) {
        if (this.ID != null) {
            throw new RuntimeException("Changing the id of a mastery effect is not allowed");
        }
        this.ID = id;
    }
}
