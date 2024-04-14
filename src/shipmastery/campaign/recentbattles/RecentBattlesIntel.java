package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CargoData;
import com.fs.starfarer.campaign.fleet.FleetData;
import org.apache.log4j.Logger;
import shipmastery.plugin.ModPlugin;
import shipmastery.util.Strings;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecentBattlesIntel extends BaseIntelPlugin {
    public static final int MAX_SIZE = 10;
    public static final String REPLAY_BUTTON_ID = "sms_ReplayBattle";
    public static final String SAVE_BUTTON_ID = "sms_SaveBattle";
    public static final Logger logger = Logger.getLogger(RecentBattlesIntel.class);
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final FleetDataAPI fleetData;
    private final boolean isStationMode;
    private final String name;
    private final FactionAPI faction;
    private final LocationAPI location;
    private final String dateString;
    private final Object fleetInflaterParams;
    private final boolean preciseMode;
    private transient CampaignFleetAPI fleet;

    public RecentBattlesIntel(boolean preciseMode, CampaignFleetAPI fleet, Object fleetInflaterParams, LocationAPI location) {
        this.preciseMode = preciseMode;
        fleetData = fleet.getFleetData();
        // Cargo is written to save, so clear it as it's not needed
        ((FleetData) fleetData).setCargo(new CargoData(false));
        isStationMode = fleet.isStationMode();
        name = fleet.getNameWithFactionKeepCase();
        faction = fleet.getFaction();
        this.fleetInflaterParams = fleetInflaterParams;
        this.location = location;
        CampaignClockAPI clock = Global.getSector().getClock();
        dateString = clock.getShortDate();
    }

    public FleetDataAPI getFleetData() {return fleetData;}
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        return new HashSet<>(Collections.singleton(Strings.RecentBattles.tagName));
    }

    @Override
    public String getIcon() {
        return faction.getCrest();
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(Strings.RecentBattles.title, 0f,
                     new Color[] {faction.getBaseUIColor(), getTitleColor(mode), getTitleColor(mode)},
                     getName(), dateString, location.getName());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean autoAddCampaignMessage() {
        return false;
    }

    @Override
    public String getSortString() {
        String str = "";
        if (isImportant()) str += " ";
        str += getPlayerVisibleTimestamp();
        return str;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void reportPlayerClickedOn() {
        super.reportPlayerClickedOn();
        if (fleet == null) {
            fleet = Global.getFactory().createEmptyFleet(faction.getId(), name, true);
            for (FleetMemberAPI fm : fleetData.getMembersListCopy()) {
                fleet.getFleetData().addFleetMember(fm);
            }
            fleet.setCommander(fleetData.getCommander());
            fleet.setStationMode(isStationMode);
            if (!preciseMode) {
                fleet.setInflater(Misc.getInflater(fleet, fleetInflaterParams));
                fleet.inflateIfNeeded();
            }
        }
    }



    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        TooltipMakerAPI membersList = panel.createUIElement(width, height * 0.6f, true);
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float size = 60f;
        int numPerRow = Math.max(1, (int) (width / size));
        membersList.addShipList(numPerRow, (members.size() + numPerRow - 1) / numPerRow, size, faction.getBaseUIColor(), members, 10f);
        panel.addUIElement(membersList).inTMid(10f);

        TooltipMakerAPI buttons = panel.createUIElement(width / 2f, 300f, false);
        buttons.addButton(Strings.RecentBattles.replayButton, REPLAY_BUTTON_ID, 100f, 30f, 10f);
        buttons.addButton(isImportant() ? Strings.RecentBattles.pinButton : Strings.RecentBattles.unpinButton, SAVE_BUTTON_ID, 100f, 30f, -30f).getPosition().setXAlignOffset(125f);
        buttons.addPara(Strings.RecentBattles.pinDesc,
                        10f,
                        Misc.getHighlightColor(),
                        "" + MAX_SIZE);
        panel.addUIElement(buttons).inTMid(20f + height * 0.6f);
    }


    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (REPLAY_BUTTON_ID.equals(buttonId)) {
            try {
                Class<?> cls =
                        ModPlugin.classLoader.loadClass("shipmastery.campaign.recentbattles.RecentBattlesReplay");
                lookup.findStatic(cls, "replayBattle", MethodType.methodType(void.class, CampaignFleetAPI.class))
                      .invoke(fleet);
            }
            catch (Throwable e) {
                logger.error("Failed to replay battle: ", e);
            }
        }
        else if (SAVE_BUTTON_ID.equals(buttonId)) {
            setImportant(!isImportant());
        }
    }
}
