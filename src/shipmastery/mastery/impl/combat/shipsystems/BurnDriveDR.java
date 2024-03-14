package shipmastery.mastery.impl.combat.shipsystems;

import shipmastery.util.Strings;

public class BurnDriveDR extends ShipSystemDR {

    @Override
    protected String getStatusTitle() {
        return Strings.Descriptions.BurnDriveDRTitle;
    }

    @Override
    protected boolean affectsFighters() {
        return true;
    }

    @Override
    public String getSystemSpecId() {
        return "burndrive";
    }
}
