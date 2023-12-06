package shipmastery.mastery.impl.combat.shipsystems;

import shipmastery.util.Strings;

public class ManeuveringJetsDR extends ShipSystemDR {
    @Override
    protected String getStatusTitle() {
        return Strings.Descriptions.ManeuveringJetsDRTitle;
    }

    @Override
    protected boolean affectsFighters() {
        return true;
    }

    @Override
    protected String getSystemId() {
        return "maneuveringjets";
    }
}
