package shipmastery.mastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.util.VariantLookup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMasteryEffect implements MasteryEffect {

    /** Commander id -> strength modifier for that commander */
//    private final Map<String, MutableStat> strengthModifierMap = new SizeLimitedMap<>(Settings.MAX_CACHED_COMMANDERS);
    private float baseStrength = 1f;
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
    public void onActivate(PersonAPI commander) {}

    @Override
    public void onDeactivate(PersonAPI commander) {}

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) throw new RuntimeException("BaseMasteryEffect init called with null or 0 args");

        try {
            baseStrength = Float.parseFloat(args[0]);
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
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
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

//    @Override
//    public final void modifyStrengthMultiplicative(PersonAPI commander, float fraction, String sourceId) {
//        if (commander == null) return;
//        String id = commander.getId();
//        MutableStat modifier = strengthModifierMap.get(id);
//        if (modifier == null) {
//            modifier = new MutableStat(1f);
//            strengthModifierMap.put(id, modifier);
//        }
//        modifier.modifyMult(sourceId, fraction);
//    }
//
//    @Override
//    public final void modifyStrengthAdditive(PersonAPI commander, float fraction, String sourceId) {
//        if (commander == null) return;
//        String id = commander.getId();
//        MutableStat modifier = strengthModifierMap.get(id);
//        if (modifier == null) {
//            modifier = new MutableStat(1f);
//            strengthModifierMap.put(id, modifier);
//        }
//        modifier.modifyPercent(sourceId, 100f*(fraction - 1f));
//    }
//
//    @Override
//    public final void unmodifyStrength(PersonAPI commander, String sourceId) {
//        if (commander == null) return;
//        String id = commander.getId();
//        MutableStat modifier = strengthModifierMap.get(id);
//        if (modifier == null) return;
//        modifier.unmodify(sourceId);
//    }


    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {}

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {}

    @Override
    public final float getStrength(PersonAPI commander) {
        float strength = baseStrength;
        if (commander == null) return strength;
        // local mod can be multiplicative (if negative) or additive (if positive)
        strength = commander.getStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + getHullSpec().getHullId()).computeEffective(strength);
        // global mod is always additive
        strength += commander.getStats().getDynamic().getMod(GLOBAL_MASTERY_STRENGTH_MOD).computeEffective(baseStrength) - baseStrength;
        return strength;
/*        String id = commander.getId();
        MutableStat modifier = strengthModifierMap.get(id);
        if (modifier == null) return baseStrength;
        return baseStrength * modifier.getModifiedValue();*/
    }

    public final float getStrengthForPlayer() {
        return getStrength(Global.getSector().getPlayerPerson());
    }

    public final float getStrength(ShipAPI ship) {
        // ship.getFleetMember() may be null (for temporary ships, etc.) but stats.getFleetMember() isn't
        return getStrength(ship.getVariant());
    }

    public final float getStrength(ShipVariantAPI variant) {
        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        return info == null ? baseStrength : getStrength(info.commander);
    }

    public final float getStrength(MutableShipStatsAPI stats) {
        return getStrength(stats.getVariant());
    }

    public final float getStrength(FleetMemberAPI fm) {
        return getStrength(fm.getVariant());
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
