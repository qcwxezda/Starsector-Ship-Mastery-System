package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CargoData;
import com.fs.starfarer.campaign.fleet.FleetData;
import org.apache.log4j.Logger;
import shipmastery.plugin.ModPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class RecentBattlesIntel extends BaseIntelPlugin {
    public static final int MAX_SIZE = 10;
    public static final String REPLAY_BUTTON_ID = "sms_ReplayBattle";
    public static final String SAVE_BUTTON_ID = "sms_SaveBattle";
    public static final Logger logger = Logger.getLogger(RecentBattlesIntel.class);
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static MethodHandle replayBattle;
    private static MethodHandle addSkillsTooltip;

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
        // TODO: What if a retreating fleet merges with a larger one, causing two different recent battle intel entries
        // to have the same officer? Is cloning officers necessary due to this?
        if (fleet == null) {
            fleet = Global.getFactory().createEmptyFleet(faction.getId(), name, true);
            for (FleetMemberAPI fm : fleetData.getMembersListCopy()) {
                fleet.getFleetData().addFleetMember(fm);
                if (fm.getCaptain() != null && !fm.getCaptain().isDefault()) {
                    fleet.getFleetData().addOfficer(fm.getCaptain());
                }
            }
            fleet.setCommander(fleetData.getCommander());
            fleet.setStationMode(isStationMode);
        }
        if (!preciseMode && !fleet.isInflated()) {
            fleet.setInflater(Misc.getInflater(fleet, fleetInflaterParams));
            fleet.inflateIfNeeded();
        }
    }

    private boolean hasAdmiralSkill(PersonAPI commander) {
        for (MutableCharacterStatsAPI.SkillLevelAPI level : commander.getStats().getSkillsCopy()) {
            if (level.getSkill().isAdmiralSkill()) {
                return true;
            }
        }
        return false;
    }

    /** Returns the total offset caused by this method */
    @SuppressWarnings("SameParameterValue")
    private float addSkillImageWithTooltip(TooltipMakerAPI tooltip, SkillSpecAPI skillSpec, PersonAPI officer, float size, float pad) {
        tooltip.addImage(skillSpec.getSpriteName(), size, size, -size);
        try {
            if (addSkillsTooltip == null) {
                Class<?> cls =
                        ModPlugin.classLoader.loadClass("shipmastery.campaign.recentbattles.SkillTooltipCreator");
                addSkillsTooltip = lookup.findStatic(cls, "addSkillTooltip",
                                                     MethodType.methodType(void.class, UIComponentAPI.class,
                                                                           SkillSpecAPI.class, PersonAPI.class));
            }
            addSkillsTooltip.invoke(tooltip.getPrev(), skillSpec, officer);
        }
        catch (Throwable e) {
            logger.error("Failed to add skill tooltip", e);
        }
        tooltip.getPrev().getPosition().setXAlignOffset(size + pad);
        if (officer.getStats().getSkillLevel(skillSpec.getId()) > 1f) {
            tooltip.addImage(Utils.eliteSkillIcons.get(skillSpec.getGoverningAptitudeId()), size+3f, size+3f, -size-3f);
            tooltip.getPrev().getPosition().setXAlignOffset(-3f);
            return -3f;
        }
        return 0f;
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float shipsPanelWidth = width - 350f;
        CustomPanelAPI shipsPanel = panel.createCustomPanel(shipsPanelWidth, height, null);
        TooltipMakerAPI membersList = shipsPanel.createUIElement(shipsPanelWidth, height, true);
        List<FleetMemberAPI> members = fleetData.getMembersListCopy();
        Collections.sort(members, Utils.byDPComparator);
        float size = 60f;
        int numPerRow = Math.max(1, (int) ((shipsPanelWidth / size)));
        membersList.addSectionHeading(Strings.RecentBattles.fleetDataHeader, Alignment.MID, 0f);
        membersList.addShipList(numPerRow, (members.size() + numPerRow - 1) / numPerRow, size, faction.getBaseUIColor(), members, 10f);

        boolean addedOfficerHeading = false;
        for (final FleetMemberAPI member : members) {
            PersonAPI captain = member.getCaptain();
            if (captain.isDefault()) continue;
            boolean isCommanderWithAdmiralSkills = captain == fleetData.getCommander() && hasAdmiralSkill(captain);

            if (!addedOfficerHeading) {
                membersList.addSectionHeading(Strings.RecentBattles.officerDataHeader, Alignment.MID, 0f);
                addedOfficerHeading = true;
            }

            List<FleetMemberAPI> singleton = Collections.singletonList(member);
            membersList.addShipList(1, 1, 80f, faction.getBaseUIColor(), singleton, 10f);
            float totalOffset = 0f, offset = 42f, centerOffset = isCommanderWithAdmiralSkills ? 40f : 24f;
            membersList.addSpacer(-centerOffset).getPosition().setXAlignOffset(offset);
            totalOffset += offset;
            for (String id : Global.getSettings().getSortedSkillIds()) {
                SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(id);
                if (captain.getStats().getSkillLevel(id) <= 0f) continue;
                if (!skillSpec.isCombatOfficerSkill()) continue;
                totalOffset += addSkillImageWithTooltip(membersList, skillSpec, captain, 36f, offset-36f);
                totalOffset += offset;
            }
            if (isCommanderWithAdmiralSkills) {
                membersList.addSpacer(offset).getPosition().setXAlignOffset(-totalOffset + offset);
                float totalCommanderOffset = 0f;
                for (String id : Global.getSettings().getSortedSkillIds()) {
                    SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(id);
                    if (captain.getStats().getSkillLevel(id) <= 0f) continue;
                    if (!skillSpec.isAdmiralSkill()) continue;
                    totalOffset += addSkillImageWithTooltip(membersList, skillSpec, captain, 36f, offset-36f);
                    totalCommanderOffset += offset;
                }
                membersList.addSpacer(-offset).getPosition().setXAlignOffset(totalOffset - totalCommanderOffset - offset);
            }
            membersList.addSpacer(centerOffset).getPosition().setXAlignOffset(-totalOffset);
        }

        shipsPanel.addUIElement(membersList).inMid();
        panel.addComponent(shipsPanel).inLMid(10f);

        CustomPanelAPI buttonsPanel = panel.createCustomPanel(350f, height, null);

        float fleetPoints = fleetData.getFleetPointsUsed();
        float deployPoints = 0f;
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            deployPoints += member.getDeploymentPointsCost();
        }

        TooltipMakerAPI buttons = buttonsPanel.createUIElement(325f, height, false);
        buttons.addSectionHeading(Strings.RecentBattles.summaryHeader, Alignment.MID, 0f);
        buttons.addPara(Strings.RecentBattles.deployPoints, 20f, Misc.getHighlightColor(), "" + (int) deployPoints).setAlignment(Alignment.MID);
        buttons.addPara(Strings.RecentBattles.fleetPoints, 20f, Misc.getHighlightColor(), "" + (int) fleetPoints).setAlignment(Alignment.MID);
        buttons.addSectionHeading(Strings.RecentBattles.battleOptionsHeader, Alignment.MID, 20f);
        buttons.addButton(Strings.RecentBattles.replayButton, REPLAY_BUTTON_ID, 300f, 25f, 40f).setShortcut(20, true);
        buttons.addPara(Strings.RecentBattles.replayDesc, 10f);
        buttons.addButton(isImportant() ? Strings.RecentBattles.unpinButton : Strings.RecentBattles.pinButton, SAVE_BUTTON_ID, 300f, 25f, 40f).setShortcut(21, true);
        buttons.addPara(Strings.RecentBattles.pinDesc, 10f, Misc.getHighlightColor(), "" + MAX_SIZE);
        buttonsPanel.addUIElement(buttons).inMid();
        panel.addComponent(buttonsPanel).rightOfMid(shipsPanel, 0f);
    }


    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (REPLAY_BUTTON_ID.equals(buttonId)) {
            try {
                if (replayBattle == null) {
                    Class<?> cls =
                            ModPlugin.classLoader.loadClass("shipmastery.campaign.recentbattles.RecentBattlesReplay");
                    replayBattle = lookup.findStatic(cls, "replayBattle", MethodType.methodType(void.class, CampaignFleetAPI.class));
                }
                replayBattle.invoke(fleet);
            }
            catch (Throwable e) {
                logger.error("Failed to replay battle: ", e);
            }
        }
        else if (SAVE_BUTTON_ID.equals(buttonId)) {
            setImportant(!isImportant());
            ui.updateUIForItem(this);
        }
    }
}
