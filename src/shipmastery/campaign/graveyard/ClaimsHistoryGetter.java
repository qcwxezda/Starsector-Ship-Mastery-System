package shipmastery.campaign.graveyard;

import exerelin.campaign.SectorManager;
import exerelin.campaign.intel.InsuranceIntelV2;
import shipmastery.util.ReflectionUtils;

import java.util.List;

public class ClaimsHistoryGetter {
    @SuppressWarnings({"unchecked", "unused"})
    public List<InsuranceIntelV2.InsuranceClaim> getClaimsHistory() {
        return (List<InsuranceIntelV2.InsuranceClaim>) ReflectionUtils.getFieldWithClass(
                InsuranceIntelV2.class, SectorManager.getManager().getInsurance(), "claimsHistory");
    }
}
