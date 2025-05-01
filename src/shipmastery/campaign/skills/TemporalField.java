package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class TemporalField implements ShipSkillEffect {
    @Override
    public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

    }

    @Override
    public String getEffectDescription(float level) {
        return "";
    }

    @Override
    public String getEffectPerLevelDescription() {
        return "";
    }

    @Override
    public ScopeDescription getScopeDescription() {
        return null;
    }
}
