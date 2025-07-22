package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.backgrounds.BackgroundUtils;
import shipmastery.data.MasteryGenerator;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;

import java.util.HashSet;

public class SModCapacityAsFractionOfMax extends BaseMasteryEffect {

    public static final String ROUND_UP = "ROUND_UP";
    public static final String ROUND_DOWN = "ROUND_DOWN";
    boolean isRoundUp = false;

    @Override
    public MasteryEffect postInit(String... args) {
        if (args.length >= 2) {
            String round = args[1].toUpperCase();
            if (ROUND_UP.equals(round)) {
                isRoundUp = true;
            } else if (ROUND_DOWN.equals(round)) {
                isRoundUp = false;
            } else {
                throw new IllegalArgumentException("Second argument of SModCapacityAsFractionOfMax must be either ROUND_DOWN or ROUND_UP, got: " + round);
            }
        }

        if (BackgroundUtils.isTinkererStart()) {
            MasteryGenerator generator = new MasteryGenerator(ShipMastery.getMasteryInfo("EmptyMastery"), new String[] {"1"});
            try {
                return generator.generate(getHullSpec(), getLevel(), getIndex(), getOptionId(), 0, new HashSet<>(), new HashSet<>());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException("Failed to generate mastery", e);
            }
        } else {
            return this;
        }
    }

    public int getIncrease() {
        if (isRoundUp) return (int) Math.ceil(getBaseStrength() * Misc.MAX_PERMA_MODS);
        return (int) Math.floor(getBaseStrength() * Misc.MAX_PERMA_MODS);
    }


    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .init(Strings.Descriptions.SModCapacity)
                .params(getIncrease())
                .colors(Misc.getTextColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getIncrease());
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 9999999f; // Always pick if possible
    }
}
