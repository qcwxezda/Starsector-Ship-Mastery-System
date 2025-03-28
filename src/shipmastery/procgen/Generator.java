package shipmastery.procgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public abstract class Generator {
    public final static float WEIGHT_PENALTY_PER_STATION = 0.9f;
    public final static float STARS_PER_STATION = 25f;
    public final static int MIN_STATIONS = 4;

    public static final Set<String> tagsToSkip = new HashSet<>();
    static {
        tagsToSkip.add(Tags.THEME_CORE);
        tagsToSkip.add(Tags.THEME_HIDDEN);
    }

    public static void generate() {
        WeightedRandomPicker<StarSystemAPI> picker = getSystemPicker(Misc.random);
        float numStations = picker.getItems().size() / STARS_PER_STATION;
        numStations = Math.max(numStations, MIN_STATIONS);

        for (int i = 0; i < numStations; i++) {
            if (picker.isEmpty()) return;
            StarSystemAPI system = picker.pickAndRemove();
            BaseThemeGenerator.EntityLocation location = BaseThemeGenerator.pickHiddenLocationNotNearStar(Misc.random, system, 100f, null);
            BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addEntity(Misc.random, system, location, "sms_concealed_station",
                                                             Factions.NEUTRAL);
            SectorEntityToken focus = added.entity.getOrbitFocus();
            if (focus instanceof PlanetAPI planet) {
                boolean nearStar = planet.isStar() && added.entity.getOrbit() != null && added.entity.getCircularOrbitRadius() < 5000;
                if (!planet.isStar() || nearStar) {
                    BaseThemeGenerator.convertOrbitPointingDown(added.entity);
                }
            }
            DefenderDataOverride ddo = new DefenderDataOverride(Factions.REMNANTS, 1f, 120f, 180f, 1000);
            Misc.setDefenderOverride(added.entity, ddo);
            SalvageSpecialAssigner.assignSpecials(added.entity);
        }
    }

    public static WeightedRandomPicker<StarSystemAPI> getSystemPicker(Random random) {
        WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            Set<String> blacklist = new HashSet<>(tagsToSkip);
            blacklist.retainAll(system.getTags());
            if (!blacklist.isEmpty()) {
                continue;
            }

            float weight = 1f;
            if (system.hasTag(Tags.THEME_INTERESTING)) {
                weight *= 2f;
            }
            else if (system.hasTag(Tags.THEME_INTERESTING_MINOR)) {
                weight *= 1.5f;
            }
            if (system.hasTag(Tags.THEME_REMNANT_MAIN)) {
                weight *= 2f;
            }
            else if (system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {
                weight *= 1.5f;
            }

            PlanetAPI primaryStar = system.getStar();
            // Nebula
            if (primaryStar == null) {
                weight *= 1.5f;
            }
            // Black hole
            else if (primaryStar.getSpec().isBlackHole()) {
                weight *= 1.5f;
            }
            // Pulsar
            else if (primaryStar.getSpec().isPulsar()) {
                weight *= 2f;
            }

            // Check for existing research stations, 90% penalty for each one
            for (SectorEntityToken entity : system.getCustomEntities()) {
                String id = entity.getCustomEntitySpec().getId();
                if ("station_research".equals(id) || "station_research_remnant".equals(id)) {
                    weight *= (1f - WEIGHT_PENALTY_PER_STATION);
                }
            }

            // Favor worlds with fewer existing custom entities in general
            weight *= 1f / Math.max(1f, (float) Math.sqrt(system.getCustomEntities().size()));

            systemPicker.add(system, weight);
        }
        return systemPicker;
    }

    public static void findStations() {
        int count1 = 0, count2 = 0, stars = 0;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!system.hasTag(Tags.THEME_HIDDEN) && !system.hasTag(Tags.THEME_CORE)) stars++;
            for (SectorEntityToken entity : system.getAllEntities()) {
                if (entity.getCustomEntitySpec() != null && "sms_concealed_station".equals(entity.getCustomEntitySpec().getId())) {
                    System.out.println(system.getName() + ": " + entity.getLocation());
                }
                if (entity.getCustomEntitySpec() != null && "station_research_remnant".equals(entity.getCustomEntitySpec().getId())) {
                    count1++;
                }
                if (entity.getCustomEntitySpec() != null && "station_research".equals(entity.getCustomEntitySpec().getId())) {
                    count2++;
                }
            }
        }
        System.out.println("station_research_remnant:" + count1 + ", " + ((float) count1/stars));
        System.out.println("station_research:" + count2 + ", " + ((float) count2/stars));
        System.out.println("number stars:" + stars);
    }
}
