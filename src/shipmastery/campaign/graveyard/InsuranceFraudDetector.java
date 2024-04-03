package shipmastery.campaign.graveyard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import exerelin.campaign.intel.InsuranceIntelV2;
import exerelin.utilities.StringHelper;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.plugin.ModPlugin;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class InsuranceFraudDetector extends BaseCampaignEventListener {

    public static final float MIN_FRAUD_AMOUNT = 100000f;
    /**
     * Should be at least claims history length to avoid double-dipping on fraudulent claims
     */
    public static final float MIN_MONTHS_DELAY = 3f;
    public static final float CHANCE_PER_MONTH = 0.15f;
    public static final String FLEET_MEMORY_KEY = "$sms_InsuranceFleet";
    public static final String FLEET_PAID_KEY = "$sms_InsuranceFleetPaid";


    /**
     * Remember claims even past 90 day limit
     * Map from claimed fleet member id to amount paid
     */
    private final Map<String, Float> paidAmounts = new HashMap<>();
    private final List<FleetMemberAPI> snapshotMembers = new ArrayList<>();
    private final Set<Integer> seenClaimHashes = new HashSet<>();
    private float currentMonthsDelay = 0f;

    /**
     * Already pending sending a hunter fleet, paused waiting for said fleet to actually spawn
     */
    private boolean paused = false;

    /**
     * Points to same claimsHistory as in InsuranceIntelV2
     */
    private final List<InsuranceIntelV2.InsuranceClaim> claimsHistory;

    public InsuranceFraudDetector() {
        super(true);
        try {
            Class<?> cls = ModPlugin.classLoader.loadClass(
                    "shipmastery.campaign.graveyard.ClaimsHistoryGetter");
            // Can't cast directly as not using same classloader
            Object getter = cls.newInstance();
            //noinspection unchecked
            claimsHistory = (List<InsuranceIntelV2.InsuranceClaim>) MethodHandles.lookup()
                                                                                 .findVirtual(cls, "getClaimsHistory",
                                                                                              MethodType.methodType(
                                                                                                      List.class))
                                                                                 .invoke(getter);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public List<FleetMemberAPI> getSnapshotMembers() {
        return snapshotMembers;
    }

    public Map<String, Float> getPaidAmounts() {
        return paidAmounts;
    }

    public int makeClaimHash(InsuranceIntelV2.InsuranceClaim claim) {
        int hash = 17;
        hash += claim.premium * 31 + 17;
        hash += (int) (claim.payment * 31 + 17);
        if (claim.member != null) {
            hash += claim.member.getId().hashCode() * 31 + 17;
        }
        if (claim.officer != null) {
            hash += claim.officer.getPerson().getId().hashCode() * 31 + 17;
        }
        int[] date = claim.date;
        hash += date[0] * 31 + 17;
        hash += date[1] * 31 + 17;
        hash += date[2] * 31 + 17;
        return hash;
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (paused || claimsHistory == null) return;

        for (InsuranceIntelV2.InsuranceClaim claim : claimsHistory) {
            if (claim.member == null) continue;
            if (seenClaimHashes.contains(makeClaimHash(claim))) continue;
            // Only consider lost ships, not recovered ships
            if (!StringHelper.getString("nex_insurance", "entryDescLost").equals(claim.desc)) continue;

            Float existingAmount = paidAmounts.get(claim.member.getId());
            paidAmounts.put(
                    claim.member.getId(), existingAmount == null ? claim.payment : existingAmount + claim.payment);
            seenClaimHashes.add(makeClaimHash(claim));
        }

        float fraudAmount = 0f;
        List<FleetMemberAPI> snapshot = new ArrayList<>();
        List<FleetMemberAPI> playerMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : playerMembers) {
            if (paidAmounts.containsKey(member.getId())) {
                fraudAmount += paidAmounts.get(member.getId());
                snapshot.add(member);
            }
        }

        float itersPerMonth = Global.getSettings().getFloat("economyIterPerMonth");
        if (fraudAmount >= MIN_FRAUD_AMOUNT) {
            currentMonthsDelay += 1f / itersPerMonth;
            if (currentMonthsDelay >= MIN_MONTHS_DELAY && Misc.random.nextFloat() * itersPerMonth < CHANCE_PER_MONTH) {
                // Trigger and reset fraud detection
                float strength = Math.max(Global.getSector().getPlayerFleet().getEffectiveStrength() * 0.7f, 80f);
                if (fraudAmount >= 250000f) strength *= 1.3f;
                if (fraudAmount >= 500000f) strength *= 1.3f;
                if (fraudAmount >= 1000000f) strength *= 1.3f;
                Global.getSector().addScript(new InsuranceFleetSpawner(strength));
                snapshotMembers.clear();
                snapshotMembers.addAll(snapshot);
                paused = true;
            }
        }
    }

    @Override
    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (fleet == null || fleet.getMemoryWithoutUpdate() == null) return;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        Boolean insuranceFleet = (Boolean) memory.get(FLEET_MEMORY_KEY);
        if (insuranceFleet != null && insuranceFleet) {
             Boolean wasPaid = (Boolean) memory.get(FLEET_PAID_KEY);
            if (wasPaid == null) wasPaid = false;
            boolean wasDestroyed = reason == FleetDespawnReason.DESTROYED_BY_BATTLE;
            resolveSpawnedFleet(wasPaid || wasDestroyed);
        }
    }

    public void resolveSpawnedFleet(boolean paidOrDestroyed) {
        if (paidOrDestroyed) {
            paidAmounts.clear();
            seenClaimHashes.clear();
        }
        currentMonthsDelay = 0f;
        paused = false;
    }

    public static class InsuranceFleetSpawner implements EveryFrameScript {
        private final float fleetStrength;
        private boolean spawned = false;
        private float daysInSystem = 0f;
        private final IntervalUtil checkInterval = new IntervalUtil(0.1f, 0.1f);

        public InsuranceFleetSpawner(float fleetStrength) {
            this.fleetStrength = fleetStrength;
        }

        @Override
        public boolean isDone() {
            return spawned;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            checkInterval.advance(amount);
            if (checkInterval.intervalElapsed()) {
                float days = Misc.getDays(checkInterval.getIntervalDuration());
                CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

                if (playerFleet.getLocationInHyperspace().length() > 30000f) return;
                if (!playerFleet.isInHyperspace()) {
                    daysInSystem += days;
                    return;
                }

                if (daysInSystem <= 7f) {
                    daysInSystem = 0f;
                    return;
                }

                List<MarketAPI> indepMarkets = Misc.getFactionMarkets(Factions.INDEPENDENT);
                WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();

                for (MarketAPI market : indepMarkets) {
                    if (Misc.isMilitary(market)) {
                        picker.add(market, 1f);
                    } else {
                        picker.add(market, Float.MIN_NORMAL);
                    }
                }

                FleetParamsV3 params =
                        new FleetParamsV3(null, playerFleet.getLocationInHyperspace(), Factions.INDEPENDENT, 1f,
                                          FleetTypes.MERC_BOUNTY_HUNTER, fleetStrength, 0f, fleetStrength * 0.1f, 0f,
                                          0f, 0f, 1f);

                params.officerNumberBonus = 5;
                params.officerLevelBonus = 5;
                params.doctrineOverride = Global.getSector().getFaction(Factions.MERCENARY).getDoctrine().clone();
                params.doctrineOverride.setWarships(4);
                params.doctrineOverride.setCarriers(2);
                params.doctrineOverride.setPhaseShips(1);

                CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
                if (fleet.isEmpty()) return;

                fleet.addScript(new AutoDespawnScript(fleet));
                fleet.setName(Strings.Graveyard.insuranceFleetName);

                MemoryAPI memory = fleet.getMemoryWithoutUpdate();
                if (!picker.isEmpty()) {
                    memory.set(MemFlags.MEMORY_KEY_SOURCE_MARKET, picker.pick().getId());
                }
                memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                memory.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
                memory.set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
                memory.set(FLEET_MEMORY_KEY, true);

                Global.getSector().getHyperspace().addEntity(fleet);
                Vector2f spawnLoc = MathUtils.randomPointInRing(playerFleet.getLocationInHyperspace(), 1000f, 1000f);
                fleet.setLocation(spawnLoc.x, spawnLoc.y);
                fleet.getAI().addAssignmentAtStart(FleetAssignment.INTERCEPT, playerFleet, 1000f, null);
                Misc.giveStandardReturnToSourceAssignments(fleet, false);

                AbilityPlugin eb = fleet.getAbility(Abilities.EMERGENCY_BURN);
                if (eb != null) eb.activate();

                spawned = true;
            }
        }
    }
}
