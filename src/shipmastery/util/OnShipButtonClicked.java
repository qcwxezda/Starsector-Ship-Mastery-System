package shipmastery.util;

import com.fs.starfarer.api.fleet.FleetMemberAPI;

public interface OnShipButtonClicked {
    void onClicked(FleetMemberAPI fm, Object... args);
}
