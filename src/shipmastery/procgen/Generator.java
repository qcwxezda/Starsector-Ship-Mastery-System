package shipmastery.procgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.procgen.AgeGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.ConditionGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.NameAssigner;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.BaseLocation;
import com.fs.starfarer.campaign.NascentGravityWell;
import com.fs.starfarer.campaign.StarSystem;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.backgrounds.BackgroundUtils;
import shipmastery.campaign.items.SuperconstructPlugin;
import shipmastery.plugin.CuratorOfficerPlugin;
import shipmastery.plugin.EmitterArrayPlugin;
import shipmastery.plugin.NucleusPlugin;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Generator {

    public static final float SECTOR_WIDTH = Global.getSettings().getFloat("sectorWidth");
    public static final float SECTOR_HEIGHT = Global.getSettings().getFloat("sectorHeight");
    public static final int NUM_STATIONS_HULLMOD = 3;
    public static final int NUM_STATIONS_ITEM = 2;
    public static final int NUM_PROBES_PER_STATION = 5;
    public static final Vector2f NUCLEUS_LOCATION = new Vector2f(60000f, 40000f);

    Random random = new Random(Global.getSector().getSeedString().hashCode());

    static class CustomSystemGenerator extends StarSystemGenerator {

        String ageGenId;

        public CustomSystemGenerator(CustomConstellationParams params, String ageGenId) {
            super(params);
            this.ageGenId = ageGenId;
        }

        @Override
        public Constellation generate() {
            constellationAgeData = (AgeGenDataSpec) Global.getSettings().getSpec(AgeGenDataSpec.class, ageGenId, false);
            return super.generate();
        }
    }

    public static final Set<String> TAGS_TO_SKIP = new HashSet<>();
    static {
        TAGS_TO_SKIP.add(Tags.THEME_CORE);
        TAGS_TO_SKIP.add(Tags.THEME_HIDDEN);
        TAGS_TO_SKIP.add(Tags.THEME_DERELICT_PROBES);
        TAGS_TO_SKIP.add(Tags.THEME_DERELICT_SURVEY_SHIP);
        TAGS_TO_SKIP.add(Tags.THEME_DERELICT_MOTHERSHIP);
    }

    public enum StationType{
        HULLMOD_1, HULLMOD_2, HULLMOD_3, HULLMOD_4, HULLMOD_5, SUPERCONSTRUCT_1, SUPERCONSTRUCT_2, CRYO_OFFICER
    }

    public void generate() {
        var eligibleSystems = new ArrayList<>(Global.getSector().getStarSystems().stream().filter(
                sys -> {
                    if (Math.abs(sys.getLocation().x) <= SECTOR_WIDTH / 6f
                            || Math.abs(sys.getLocation().y) <= SECTOR_HEIGHT / 6f) return false;
                    Set<String> blacklist = new HashSet<>(sys.getTags());
                    blacklist.retainAll(TAGS_TO_SKIP);
                    return blacklist.isEmpty();
                }
        ).toList());
        var stationSystems = generateStations(eligibleSystems);
        generateProbes(stationSystems, eligibleSystems);

        generateNucleusStar();
        generateRemoteSystem();
    }

    public void generateRemoteSystem() {

        Vector2f remoteBeaconLocation = new Vector2f(-SECTOR_WIDTH/2f-15500f, SECTOR_HEIGHT/2f-14500f);
        var system = Global.getSector().createStarSystem(Strings.Campaign.remoteBeacon);
        system.getLocation().set(remoteBeaconLocation.x, remoteBeaconLocation.y);
        Global.getSector().getMemoryWithoutUpdate().set(Strings.Campaign.REMOTE_BEACON_LOCATION, remoteBeaconLocation);

        var center = Global.getSettings().createLocationToken(0f, 0f);
        system.addEntity(center);
        var planet = system.addPlanet("sms_remote_beacon", center, Strings.Campaign.remoteBeacon, Planets.BARREN_CASTIRON, 0f, 300f, 0.01f, 100f);
        planet.setCustomDescriptionId("sms_remote_beacon");
        // add picked conditions to market
        MarketAPI market = Global.getFactory().createMarket(planet.getId(), planet.getName(), 1);
        market.setPlanetConditionMarketOnly(true);
        market.setPrimaryEntity(planet);
        market.setFactionId(Factions.NEUTRAL);
        planet.setMarket(market);

        market.getMemoryWithoutUpdate().set(Strings.Campaign.REMOTE_BEACON_HAS_SHIELD, true);
        market.getMemoryWithoutUpdate().set(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY, Strings.Campaign.COMMANDER_PREFIX + Misc.genUID() + "_" + Misc.random.nextInt());

        List<String> ids = Arrays.asList("dark", "high_gravity", "no_atmosphere", "very_cold");
        for (String cid : ids) {
            if (cid.endsWith(ConditionGenDataSpec.NO_PICK_SUFFIX)) continue;
            //planet.getMemory().set("$genCondition:" + condition, true);

            MarketConditionAPI mc = market.getSpecificCondition(market.addCondition(cid));

            ConditionGenDataSpec spec = (ConditionGenDataSpec) Global.getSettings().getSpec(ConditionGenDataSpec.class, cid, true);
            mc.setSurveyed(!spec.isRequiresSurvey());
        }
        market.reapplyConditions();
        system.setCenter(center);

        var locationToken = Global.getSettings().createLocationToken(remoteBeaconLocation.x, remoteBeaconLocation.y);
        Global.getSector().getHyperspace().addEntity(locationToken);
        NascentGravityWellAPI well = new NascentGravityWell(planet, 250f);
        well.setColorOverride(new Color(150, 255, 200));
        well.setOrbit(new StarSystem.UpdateFromSystemLocationOrbit(locationToken, Global.getSettings().createLocationToken(0f, 0f), planet, well, 0f));
        Global.getSector().getHyperspace().addEntity(well);

        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.SYSTEM_ABYSSAL);
        system.addTag(Tags.THEME_SPECIAL);

        float emitterDist = planet.getRadius() + 3000f;
        float emitterOrbitDays = 60f;
        List<SectorEntityToken> emitters = new ArrayList<>();
        for (float angle = 0f; angle < 360f; angle += 60f) {
            int i = ((int) (angle/60f));
            char c = (char) ('A' + i);
            String name = Global.getSettings().getCustomEntitySpec("sms_emitter_array").getDefaultName();
            var emitter = system.addCustomEntity(null, name + " " + c, "sms_emitter_array", Factions.NEUTRAL);
            emitter.setCircularOrbitPointingDown(planet, angle, emitterDist, emitterOrbitDays);
            emitter.setDiscoverable(true);
            emitter.setDiscoveryXP(1000f);
            emitter.setSensorProfile(1000f);
            if (i == 0) {
                emitter.getMemoryWithoutUpdate().set(EmitterArrayPlugin.KEY_IS_FIRST_EMITTER, true);
            }
            emitters.add(emitter);
        }

        for (int i = 0; i < emitters.size(); i++) {
            var emitter = emitters.get(i);
            var prevEmitter = emitters.get((i+5)%emitters.size());
            var nextEmitter = emitters.get((i+1)%emitters.size());
            var data = emitter.getMemoryWithoutUpdate();
            data.set(EmitterArrayPlugin.KEY_NEXT_EMITTER, nextEmitter);
            data.set(EmitterArrayPlugin.KEY_PREV_EMITTER, prevEmitter);
        }
    }

    private static class CuratorStationFleetManager extends RemnantStationFleetManager {

        public CuratorStationFleetManager(SectorEntityToken source, float thresholdLY, int minFleets, int maxFleets, float respawnDelay, int minPts, int maxPts) {
            super(source, thresholdLY, minFleets, maxFleets, respawnDelay, minPts, maxPts);
        }

        @Override
        protected CampaignFleetAPI spawnFleet() {
            if (source == null) return null;

            Random random = new Random();
            int combatPoints = minPts + random.nextInt(maxPts - minPts + 1);
            int bonus = totalLost * 4;
            if (bonus > maxPts) bonus = maxPts;
            combatPoints += bonus;
            String type = FleetTypes.PATROL_SMALL;
            if (combatPoints > 8) type = FleetTypes.PATROL_MEDIUM;
            if (combatPoints > 16) type = FleetTypes.PATROL_LARGE;
            combatPoints *= 8;

            FleetParamsV3 params = new FleetParamsV3(
                    source.getMarket(),
                    source.getLocationInHyperspace(),
                    "sms_curator",
                    2f,
                    type,
                    combatPoints, // combatPts
                    0f, // freighterPts
                    0f, // tankerPts
                    0f, // transportPts
                    0f, // linerPts
                    0f, // utilityPts
                    0f // qualityMod
            );
            params.random = random;
            switch (type) {
                case FleetTypes.PATROL_SMALL -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_BETA;
                case FleetTypes.PATROL_MEDIUM -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
                case FleetTypes.PATROL_LARGE -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_ALPHA;
            }

            CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
            if (fleet == null) return null;

            LocationAPI location = source.getContainingLocation();
            location.addEntity(fleet);

            RemnantSeededFleetManager.initRemnantFleetProperties(random, fleet, false);

            fleet.setLocation(source.getLocation().x, source.getLocation().y);
            fleet.setFacing(random.nextFloat() * 360f);

            fleet.addScript(new RemnantAssignmentAI(fleet, (StarSystemAPI) source.getContainingLocation(), source));
            fleet.getMemoryWithoutUpdate().set("$sourceId", source.getId());

            return fleet;
        }
    }

    public void generateNucleusStar() {
        float maxX = 0f, maxY = 0f;
        for (var sys : Global.getSector().getStarSystems()) {
            maxX = Math.max(maxX, sys.getLocation().x);
            maxY = Math.max(maxY, sys.getLocation().y);
        }

        // Try to pick a location not near other stars
        float finalMaxX = maxX;
        float finalMaxY = maxY;
        List<StarSystemAPI> toCheck = Global.getSector().getStarSystems()
                .stream()
                .filter(sys -> sys.getLocation().x >= finalMaxX - 30000f && sys.getLocation().y >= finalMaxY - 30000f && !sys.hasTag(Tags.SYSTEM_ABYSSAL))
                .toList();

        int tries = 10;
        float maxMinDist = 0f;
        Vector2f bestLoc = new Vector2f(SECTOR_WIDTH/2f - 10000f, SECTOR_HEIGHT/2f - 10000f);
        for (int i = 0; i < tries; i++) {
            Vector2f loc = new Vector2f(
                    Math.min(maxX + MathUtils.randBetween(-24000f, 6000f, random), SECTOR_WIDTH/2f - 10000f),
                    Math.min(maxY + MathUtils.randBetween(-24000f, 6000f, random), SECTOR_HEIGHT/2f - 10000f));
            float minDist = Float.MAX_VALUE;
            for (var sys : toCheck) {
                var dist = MathUtils.dist(sys.getLocation(), loc);
                if (dist < minDist) {
                    minDist = dist;
                }
            }

            if (minDist > maxMinDist) {
                maxMinDist = minDist;
                bestLoc = loc;
            }
        }

        StarSystemGenerator.CustomConstellationParams params = new StarSystemGenerator.CustomConstellationParams(StarAge.ANY);
        params.numStars = 1;
        params.starTypes = new ArrayList<>();
        params.starTypes.add(StarTypes.WHITE_DWARF);
        params.starTypes.add(StarTypes.WHITE_DWARF);
        params.systemTypes = new ArrayList<>();
        params.location = bestLoc;
        params.systemTypes.add(StarSystemGenerator.StarSystemType.BINARY_CLOSE);

        StarSystemGenerator gen = new CustomSystemGenerator(params, "sms_nucleus");
        var constellation = gen.generate();

        var system = constellation.getSystemWithMostPlanets();
        Global.getSector().getMemoryWithoutUpdate().set(Strings.Campaign.NUCLEUS_LOCATION, system.getLocation());

        // Remove the weirdness from the binary orbit
        var star1 = system.getStar();
        var star2 = system.getSecondary();

        // Apparently can't have two stars orbiting each other, so have both orbit (0, 0) instead...
        BaseLocation.LocationToken orbitFocus;
        float orbitDays = 20f;
        if (star1.getOrbitFocus() instanceof BaseLocation.LocationToken loc) {
            orbitFocus = loc;
        } else {
            orbitFocus = (BaseLocation.LocationToken) star2.getOrbitFocus();
        }
        orbitFocus.setLocation(0f, 0f);

        star1.setCircularOrbitPointingDown(orbitFocus, 0f, 500f + star1.getRadius(), orbitDays);
        star2.setCircularOrbitPointingDown(orbitFocus, 180f, 500f + star2.getRadius(), orbitDays);

        var themeGenerator = new RemnantThemeGenerator();
        system.addTag(Tags.THEME_UNSAFE);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.getAutogeneratedJumpPointsInHyper().forEach(loc -> loc.addTag(Tags.STAR_HIDDEN_ON_MAP));
        var systemData = BaseThemeGenerator.computeSystemData(system);
        themeGenerator.populateMain(systemData, RemnantThemeGenerator.RemnantSystemType.RESURGENT);
        List<CampaignFleetAPI> stations = themeGenerator.addBattlestations(systemData, 1f, 1, 1, themeGenerator.createStringPicker("remnant_station2_Standard", 10f));
        for (CampaignFleetAPI station : stations) {
            station.setFaction("sms_curator");
            station.setName(Strings.Campaign.convertedNexus);
            FleetParamsV3 stationParams = new FleetParamsV3();
            stationParams.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA;
            new CuratorOfficerPlugin().addCommanderAndOfficers(station, stationParams, StarSystemGenerator.random);
            CuratorStationFleetManager activeFleets = new CuratorStationFleetManager(
                    station, 1f, 0, 12, 20f, 8, 24);
            system.addScript(activeFleets);
        }

        if (!NameAssigner.isNameSpecial(system)) {
            NameAssigner.assignSpecialNames(system);
        }

        var entity = system.addCustomEntity(null, null, "sms_station_nucleus", Factions.NEUTRAL);
        var plugin = entity.getCustomPlugin();
        if (plugin instanceof NucleusPlugin nPlugin) {
            nPlugin.jumpPointsInHyper.addAll(system.getAutogeneratedJumpPointsInHyper());
        }
        entity.setCircularOrbitPointingDown(orbitFocus, 0f, 0.1f, orbitDays);
        entity.setDiscoverable(true);
        entity.setDiscoveryXP(5000f);
        entity.setSensorProfile(1000f);
        entity.getMemoryWithoutUpdate().set(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY, Strings.Campaign.COMMANDER_PREFIX + Misc.genUID() + "_" + Misc.random.nextInt());
        CargoAPI extra = Global.getFactory().createCargo(true);
        extra.addCommodity("alpha_core", 1 + Misc.random.nextInt(2));
        extra.addCommodity("beta_core", 2 + Misc.random.nextInt(2));
        extra.addCommodity("gamma_core", 3 + Misc.random.nextInt(3));
        extra.addCommodity("sms_fractured_gamma_core", 20 + Misc.random.nextInt(10));
        extra.addCommodity("sms_alpha_pseudocore", 1 + Misc.random.nextInt(2));
        extra.addCommodity("sms_beta_pseudocore", 2 + Misc.random.nextInt(2));
        extra.addCommodity("sms_gamma_pseudocore", 3 + Misc.random.nextInt(3));
        extra.addSpecial(new SpecialItemData("sms_superconstruct3", SuperconstructPlugin.ACTIVE_STRING), 1f);
        if (BackgroundUtils.isRejectHumanityStart()) {
            extra.addSpecial(new SpecialItemData("sms_pseudocore_uplink_mk2", null), 1f);
        } else {
            extra.addSpecial(new SpecialItemData("sms_pseudocore_uplink", null), 1f);
        }

        BaseSalvageSpecial.ExtraSalvage extraSalvage = new BaseSalvageSpecial.ExtraSalvage(extra);
        entity.getMemoryWithoutUpdate().set(BaseSalvageSpecial.EXTRA_SALVAGE, extraSalvage);
        entity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, StarSystemGenerator.random.nextLong());
    }

    public List<SectorEntityToken> generateStations(List<StarSystemAPI> eligibleSystems) {
        Collections.shuffle(eligibleSystems);
        int numStations = NUM_STATIONS_HULLMOD + NUM_STATIONS_ITEM;
        List<StarSystemAPI> selectedSystems = new ArrayList<>();
        Set<Vector2f> locationsToAvoid = new HashSet<>();
        locationsToAvoid.add(NUCLEUS_LOCATION);
        for (int i = 0; i < numStations; i++) {
            var system = getSumFarthestSystem(eligibleSystems, locationsToAvoid);
            selectedSystems.add(system);
            locationsToAvoid.add(system.getLocation());
        }

        List<StationType> stationTypes = new ArrayList<>();
        // Always add a station with an officer
        stationTypes.add(StationType.CRYO_OFFICER);
        // 50% chance of having either superconstruct
        if (random.nextFloat() < 0.5f) {
            stationTypes.add(StationType.SUPERCONSTRUCT_1);
        } else {
            stationTypes.add(StationType.SUPERCONSTRUCT_2);
        }

        // Finally, pick 3 out of 5 of the unique hullmods to add
        List<StationType> stationTypes2 = new ArrayList<>(Arrays.asList(StationType.HULLMOD_1, StationType.HULLMOD_2, StationType.HULLMOD_3, StationType.HULLMOD_4, StationType.HULLMOD_5));
        Collections.shuffle(stationTypes2);

        stationTypes.addAll(stationTypes2.subList(0, 3));
        Collections.shuffle(stationTypes);

        // Add the stations
        var globalMemory = Global.getSector().getMemoryWithoutUpdate();
        List<String> locations = new ArrayList<>();
        globalMemory.set(Strings.Campaign.BEACON_LOCATION_NAMES, locations);
        List<SectorEntityToken> addedEntities = new ArrayList<>();
        for (int i = 0; i < selectedSystems.size(); i++) {
            StarSystemAPI system = selectedSystems.get(i);
            var ly = Utils.toLightyears(system.getLocation());
            locations.add(system.getNameWithTypeIfNebula() + String.format(" (%.1f, %.1f)", ly.getX(), ly.getY()));
            BaseThemeGenerator.EntityLocation location = BaseThemeGenerator.pickHiddenLocationNotNearStar(random, system, 100f, null);
            BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addEntity(random, system, location, "sms_concealed_station",
                    Factions.NEUTRAL);
            processAddedItem(added.entity);
            var memory = added.entity.getMemoryWithoutUpdate();
            memory.set(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY, Strings.Campaign.COMMANDER_PREFIX + Misc.genUID());
            memory.set(Strings.Campaign.STATION_TYPE_KEY, stationTypes.get(i));
            memory.set(Strings.Campaign.BEACON_ID, i);
            addedEntities.add(added.entity);
            // strength data is overridden in ConcealedEntityDefenderPlugin
            DefenderDataOverride ddo = new DefenderDataOverride("sms_curator", 1f, 0f, 0f, 0);
            Misc.setDefenderOverride(added.entity, ddo);
            SalvageSpecialAssigner.assignSpecials(added.entity);
        }

        return addedEntities;
    }

    public void generateProbes(List<SectorEntityToken> stations, List<StarSystemAPI> eligibleSystems) {
        for (int i = 0; i < stations.size(); i++) {
            SectorEntityToken station = stations.get(i);
            var stationSystem = station.getStarSystem();
            WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(random);
            for (StarSystemAPI eligibleSystem : eligibleSystems) {
                float dist = MathUtils.dist(eligibleSystem.getLocation(), stationSystem.getLocation());
                if (dist > 36000f || station.getStarSystem() == eligibleSystem) continue;
                picker.add(eligibleSystem, 1f / (1f + dist));
            }
            if (picker.isEmpty()) continue;
            for (int j = 0; j < NUM_PROBES_PER_STATION; j++) {
                StarSystemAPI system = null;
                if (j == 0 && i == 0) {
                    system = Global.getSector().getStarSystem("tia");
                } else if (j == 0 && i == 1) {
                    system = Global.getSector().getStarSystem("penelope's star");
                }

                if (system == null) {
                    system = picker.pick();
                    // Why no pick with index??
                    List<StarSystemAPI> items = picker.getItems();
                    for (int k = 0; k < items.size(); k++) {
                        var sys = items.get(k);
                        if (sys == system) {
                            picker.setWeight(k, picker.getWeight(k) / 3f);
                        }
                    }
                }

                if (system == null) return;
                BaseThemeGenerator.EntityLocation location = BaseThemeGenerator.pickAnyLocation(random, system, 100f, Collections.singleton(station));
                BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addEntity(random, system, location, "sms_concealed_probe",
                        Factions.NEUTRAL);
                processAddedItem(added.entity);
                var memory = added.entity.getMemoryWithoutUpdate();
                memory.set(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY, Strings.Campaign.COMMANDER_PREFIX + Misc.genUID());
                memory.set(Strings.Campaign.PROBE_PARENT_STATION, station);
                // strength data is overridden in ConcealedEntityDefenderPlugin
                DefenderDataOverride ddo = new DefenderDataOverride("sms_curator", 1f, 0f, 0f, 0);
                Misc.setDefenderOverride(added.entity, ddo);
                SalvageSpecialAssigner.assignSpecials(added.entity);
            }
        }
    }

    public void processAddedItem(SectorEntityToken entity) {
        SectorEntityToken focus = entity.getOrbitFocus();
        if (focus instanceof PlanetAPI planet) {
            boolean nearStar = planet.isStar() && entity.getOrbit() != null && entity.getCircularOrbitRadius() < 5000;
            if (!planet.isStar() || nearStar) {
                BaseThemeGenerator.convertOrbitPointingDown(entity);
            }
        }
        SalvageSpecialAssigner.assignSpecials(entity);
    }

    public StarSystemAPI getSumFarthestSystem(List<StarSystemAPI> systems, Set<Vector2f> systemsToAvoid) {
        // Nothing to avoid, just pick a random system
        if (systemsToAvoid == null || systemsToAvoid.isEmpty()) {
            return systems.get(random.nextInt(systems.size()));
        }

        float maxDist = 0f;
        StarSystemAPI bestSystem = null;
        outer:
        for (var system : systems) {
            float curDist = 0f;
            for (var loc : systemsToAvoid) {
                float dist = MathUtils.dist(system.getLocation(), loc);
                // Too close
                if (dist <= 10000f) continue outer;
                curDist += dist;
            }
            if (curDist > maxDist) {
                if (random.nextFloat() < 0.5f) {
                    maxDist = curDist;
                    bestSystem = system;
                }
            }
        }

        // Didn't find anything, pick a random system
        if (bestSystem == null) {
            return systems.get(random.nextInt(systems.size()));
        }
        return bestSystem;
    }
}
