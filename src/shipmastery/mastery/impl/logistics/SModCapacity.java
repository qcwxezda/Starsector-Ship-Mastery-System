package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.ShipMastery;
import shipmastery.backgrounds.HullTinkerer;
import shipmastery.config.Settings;
import shipmastery.data.MasteryGenerator;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;

import java.util.HashSet;

public class SModCapacity extends AdditiveMasteryEffect {

    @Override
    public MasteryEffect postInit(String... args) {
        boolean isTinkerer = (boolean) Global.getSector().getPersistentData().getOrDefault(HullTinkerer.IS_TINKERER_START, false);
        if (isTinkerer) {
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

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .init(Strings.Descriptions.SModCapacity)
                .params(getIncreasePlayer())
                .colors(Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getIncrease(stats));
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
