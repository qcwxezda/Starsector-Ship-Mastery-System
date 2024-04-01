package shipmastery.campaign.graveyard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.SectorManager;
import exerelin.campaign.intel.InsuranceIntelV2;
import exerelin.utilities.StringHelper;
import shipmastery.util.ReflectionUtils;

import java.util.*;

/** TODO: make this transient, since non-transient scripts don't maintain custom classloaders, and then move the non-transient stuff to a separate class, manually adding it to the sector's persistent data. */
public class InsuranceFraudDetector extends BaseCampaignEventListener {

    public static final float MIN_FRAUD_AMOUNT = 100000f;
    /** Should be at least claims history length to avoid double-dipping on fraudulent claims */
    public static final float MIN_MONTHS_DELAY = 1f;
    public static final float CHANCE_PER_MONTH = 10f;


    /** Remember claims even past 90 day limit
    * Map from claimed fleet member id to amount paid */
    private final Map<String, Float> paidAmounts = new HashMap<>();
    private final Set<Integer> seenClaimHashes = new HashSet<>();
    private float currentMonthsDelay = 0f;

    /** Already sent a hunter fleet, paused waiting for it to resolve */
    private boolean paused = false;

    /** Points to same claimsHistory as in InsuranceIntelV2 */
    private transient List<InsuranceIntelV2.InsuranceClaim> claimsHistory;


    public InsuranceFraudDetector() {
        super(true);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (claimsHistory == null) {
            InsuranceIntelV2 insurance = SectorManager.getManager().getInsurance();
            //noinspection unchecked
            claimsHistory =
                    (List<InsuranceIntelV2.InsuranceClaim>) ReflectionUtils.getFieldWithClass(InsuranceIntelV2.class, insurance, "claimsHistory");
            return;
        }
        if (paused) return;

        for (InsuranceIntelV2.InsuranceClaim claim : claimsHistory) {
            if (claim.member == null) continue;
            if (seenClaimHashes.contains(claim.hashCode())) continue;
            // Only consider lost ships, not recovered ships
            if (!StringHelper.getString("nex_insurance", "entryDescLost").equals(claim.desc)) continue;

            Float existingAmount = paidAmounts.get(claim.member.getId());
            paidAmounts.put(claim.member.getId(), existingAmount == null ? claim.payment : existingAmount + claim.payment);
            seenClaimHashes.add(claim.hashCode());
        }

        float fraudAmount = 0f;
        List<FleetMemberAPI> playerMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : playerMembers) {
            if (paidAmounts.containsKey(member.getId())) {
                fraudAmount += paidAmounts.get(member.getId());
            }
        }

        float itersPerMonth = Global.getSettings().getFloat("economyIterPerMonth");
        if (fraudAmount >= MIN_FRAUD_AMOUNT) {
            currentMonthsDelay += 1f / itersPerMonth;
            if (currentMonthsDelay >= MIN_MONTHS_DELAY && Misc.random.nextFloat() * itersPerMonth < CHANCE_PER_MONTH) {
                // Trigger and reset fraud detection
                Global.getSector().getCampaignUI().showMessageDialog("Insurance fraud detected! Sending audit fleet");
                currentMonthsDelay = 0f;
                paidAmounts.clear();
                seenClaimHashes.clear();
            }
        }

    }
}
