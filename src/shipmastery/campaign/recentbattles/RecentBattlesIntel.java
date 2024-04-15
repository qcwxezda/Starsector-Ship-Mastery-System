package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
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
    /** Contains all information about the context, except both playerFleet and otherFleet should be null */
    private final BattleCreationContext bccStub;
    /** The primary fleet should be the first item in the list */
    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private final LocationAPI foughtLocation;
    private final String dateString;
    private final boolean preciseMode;

    public RecentBattlesIntel(boolean preciseMode, List<CampaignFleetAPI> enemyFleets, BattleCreationContext bccStub, LocationAPI location) {
        this.preciseMode = preciseMode;
        fleets.addAll(enemyFleets);
        this.bccStub = bccStub;
        this.foughtLocation = location;
        CampaignClockAPI clock = Global.getSector().getClock();
        dateString = clock.getShortDate();
    }

    public PersonAPI getCombinedCommander() {return fleets.get(0).getCommander();}

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        return new HashSet<>(Collections.singleton(Strings.RecentBattles.tagName));
    }

    @Override
    public String getIcon() {
        return fleets.get(0).getFaction().getCrest();
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(Strings.RecentBattles.title, 0f,
                     new Color[] {fleets.get(0).getFaction().getBaseUIColor(), getTitleColor(mode), getTitleColor(mode)},
                     getName(), dateString, foughtLocation.getName());
    }

    @Override
    public String getName() {
        String name = fleets.get(0).getNameWithFactionKeepCase();
        if (fleets.size() > 1) {
            name += Strings.RecentBattles.alliesSuffix;
        }
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
        repairFleets();
        if (preciseMode) return;
        for (CampaignFleetAPI fleet : fleets) {
            if (!fleet.isInflated() && fleet.getInflater() != null) {
                fleet.inflateIfNeeded();
            }
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
        FactionAPI faction = fleets.get(0).getFaction();
        float shipsPanelWidth = width - 350f;
        CustomPanelAPI shipsPanel = panel.createCustomPanel(shipsPanelWidth, height, null);
        TooltipMakerAPI membersList = shipsPanel.createUIElement(shipsPanelWidth, height/2f, true);
        List<FleetMemberAPI> members = new ArrayList<>();
        for (CampaignFleetAPI fleet : fleets) {
            members.addAll(fleet.getFleetData().getMembersListCopy());
        }
        Collections.sort(members, Utils.byDPComparator);
        float size = 60f;
        int numPerRow = Math.max(1, (int) ((shipsPanelWidth / size)));
        membersList.addSectionHeading(Strings.RecentBattles.fleetDataHeader, Alignment.MID, 0f);
        membersList.addShipList(numPerRow, (members.size() + numPerRow - 1) / numPerRow, size, faction.getBaseUIColor(), members, 10f);
        shipsPanel.addUIElement(membersList).inTL(0f, 0f);

        TooltipMakerAPI officersList = shipsPanel.createUIElement(shipsPanelWidth, height/2f, true);
        boolean addedOfficerHeading = false;
        for (final FleetMemberAPI member : members) {
            PersonAPI captain = member.getCaptain();
            if (captain.isDefault()) continue;
            boolean isCommanderWithAdmiralSkills = captain == getCombinedCommander() && hasAdmiralSkill(captain);

            if (!addedOfficerHeading) {
                officersList.addSectionHeading(Strings.RecentBattles.officerDataHeader, Alignment.MID, 0f);
                officersList.addSpacer(10f);
                addedOfficerHeading = true;
            }

            List<FleetMemberAPI> singleton = Collections.singletonList(member);
            officersList.addShipList(1, 1, 80f, faction.getBaseUIColor(), singleton, 0f);
            float totalOffset = 0f, offset = 42f, centerOffset = isCommanderWithAdmiralSkills ? 40f : 24f;
            officersList.addSpacer(-centerOffset).getPosition().setXAlignOffset(offset);
            totalOffset += offset;
            for (String id : Global.getSettings().getSortedSkillIds()) {
                SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(id);
                if (captain.getStats().getSkillLevel(id) <= 0f) continue;
                if (!skillSpec.isCombatOfficerSkill()) continue;
                totalOffset += addSkillImageWithTooltip(officersList, skillSpec, captain, 36f, offset-36f);
                totalOffset += offset;
            }
            if (isCommanderWithAdmiralSkills) {
                officersList.addSpacer(offset).getPosition().setXAlignOffset(-totalOffset + offset);
                float totalCommanderOffset = 0f;
                for (String id : Global.getSettings().getSortedSkillIds()) {
                    SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(id);
                    if (captain.getStats().getSkillLevel(id) <= 0f) continue;
                    if (!skillSpec.isAdmiralSkill()) continue;
                    totalOffset += addSkillImageWithTooltip(officersList, skillSpec, captain, 36f, offset-36f);
                    totalCommanderOffset += offset;
                }
                officersList.addSpacer(-offset).getPosition().setXAlignOffset(totalOffset - totalCommanderOffset - offset);
            }
            officersList.addSpacer(centerOffset).getPosition().setXAlignOffset(-totalOffset);
        }

        shipsPanel.addUIElement(officersList).inBL(0f, 0f);
        panel.addComponent(shipsPanel).inLMid(0f);

        CustomPanelAPI buttonsPanel = panel.createCustomPanel(350f, height, null);

        float fleetPoints = 0, deployPoints = 0;
        for (CampaignFleetAPI fleet : fleets) {
            fleetPoints += fleet.getFleetPoints();
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                deployPoints += member.getDeploymentPointsCost();
            }
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
                    replayBattle = lookup.findStatic(cls, "replayBattle", MethodType.methodType(void.class, BattleCreationContext.class));
                }
                BattleAPI battle = Global.getFactory().createBattle(Global.getSector().getPlayerFleet(), fleets.get(0));
                for (int i = 1; i < fleets.size(); i++) {
                    battle.join(fleets.get(i), BattleAPI.BattleSide.TWO);
                }
                // Fleet is not alive (location is null), so battle will see it as empty
                battle.genCombinedDoNotRemoveEmpty();
                replayBattle.invoke(RecentBattlesTracker.cloneContext(bccStub, Global.getSector().getPlayerFleet(), battle.getNonPlayerCombined()));
                battle.finish(null, false);
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

    private void repairFleets() {
        for (CampaignFleetAPI fleet : fleets) {
            for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
                fm.getStatus().repairFully();
                fm.getStatus().resetDamageTaken();
            }
        }
    }
}
