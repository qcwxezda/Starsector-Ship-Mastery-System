package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.apache.log4j.Logger;
import shipmastery.deferred.Action;
import shipmastery.plugin.ModPlugin;
import shipmastery.util.FleetMemberTooltipCreator;
import shipmastery.util.OnShipButtonClicked;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecentBattlesIntel extends BaseIntelPlugin {
    public static final int MAX_SIZE = 10;
    public static final String REPLAY_BUTTON_ID = "sms_ReplayBattle";
    public static final String SOLO_REPLAY_BUTTON_ID = "sms_SoloReplay";
    public static final String SAVE_BUTTON_ID = "sms_SaveBattle";
    public static final Logger logger = Logger.getLogger(RecentBattlesIntel.class);
    // Use MethodHandles to essentially cast across classloaders; TooltipCreator is loaded with the reflection-enabled
    // classloader, so normal casting won't work
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    public static MethodHandle saveScrollbarLocationHandle;
    private static MethodHandle replayBattle;
    private static MethodHandle findInteractionDialogClassIfNeeded;

    static {
        try {
            createMethodHandles();
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** Contains all information about the context, except both playerFleet and otherFleet should be null */
    private final BattleCreationContext bccStub;
    /** The primary fleet should be the first item in the list */
    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private final LocationAPI foughtLocation;
    private final String dateString;
    private final boolean preciseMode;
    private transient FleetMemberAPI selectedFleetMember;
    private transient TooltipMakerAPI membersListTooltip;
    private transient float savedScrollbarLocation = 0f;

    public RecentBattlesIntel(boolean preciseMode, List<CampaignFleetAPI> enemyFleets, BattleCreationContext bccStub, LocationAPI location) {
        this.preciseMode = preciseMode;
        fleets.addAll(enemyFleets);
        this.bccStub = bccStub;
        this.foughtLocation = location;
        CampaignClockAPI clock = Global.getSector().getClock();
        dateString = clock.getShortDate();
    }

    public static void saveScrollbarLocation(RecentBattlesIntel intel, TooltipMakerAPI tooltip) {
        try {
            // Container's scrollbar location should be more accurate, won't cause jumps
            // when refreshing while scrolling
            Object contentContainer = ReflectionUtils.invokeMethodNoCatch(tooltip.getExternalScroller(),
                                                                          "getContentContainer");
            float yOffset = (float) ReflectionUtils.invokeMethod(contentContainer, "getYOffset");
            intel.saveScrollbarLocation(yOffset);
        }
        catch (Exception e) {
            intel.saveScrollbarLocation(tooltip.getExternalScroller().getYOffset());
        }
    }

    public void selectFleetMember(FleetMemberAPI member) {
        selectedFleetMember = selectedFleetMember == member ? null : member;
    }

    public void saveScrollbarLocation(float position) {
        savedScrollbarLocation = position;
    }

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

    private static void createMethodHandles() throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        saveScrollbarLocationHandle = lookup.findStatic(
                RecentBattlesIntel.class,
                "saveScrollbarLocation",
                MethodType.methodType(void.class, RecentBattlesIntel.class, TooltipMakerAPI.class));
        Class<?> replayClass =
                ModPlugin.classLoader.loadClass("shipmastery.campaign.recentbattles.RecentBattlesReplay");
        replayBattle = lookup.findStatic(replayClass, "replayBattle", MethodType.methodType(void.class, BattleCreationContext.class, Action.class));
        findInteractionDialogClassIfNeeded = lookup.findStatic(replayClass, "findInteractionDialogClassIfNeeded", MethodType.methodType(void.class, IntelUIAPI.class));
    }

    private void addShipsList(
            RecentBattlesIntel intel,
            IntelUIAPI intelUI,
            TooltipMakerAPI tooltip,
            List<FleetMemberAPI> members,
            int numPerRow,
            float size,
            Color color) {
        tooltip.addShipList(numPerRow, (members.size() + numPerRow - 1) / numPerRow, size, color, members, 10f);
        try {
            FleetMemberTooltipCreator.modifyShipButtons.invoke(tooltip, tooltip.getPrev(), (OnShipButtonClicked) (fm, args) -> {
                RecentBattlesIntel.saveScrollbarLocation(intel, tooltip);
                intel.selectFleetMember(fm);
                intelUI.updateUIForItem(intel);
            });
        }
        catch (Throwable e) {
            logger.error("Failed to modify ship buttons", e);
        }
    }

    private Pair<Float, Float> addFleetMemberHighlight(
            TooltipMakerAPI tooltip,
            FactionAPI combinedFaction,
            int index,
            int numPerRow,
            float size,
            boolean brightOutline,
            float existingOffsetX,
            float existingOffsetY) {
        if (index > -1) {
            ButtonAPI button = tooltip.addAreaCheckbox(
                    "",
                    "dummy button",
                    combinedFaction.getBaseUIColor(),
                    brightOutline ? combinedFaction.getBaseUIColor(): combinedFaction.getDarkUIColor(),
                    combinedFaction.getBrightUIColor(),
                    size,
                    size,
                    0f);
            button.highlight();
            button.setChecked(false);
            button.setClickable(false);
            button.setMouseOverSound(null);
            button.setHighlightBrightness(brightOutline ? 1f : 0.5f);
            button.setGlowBrightness(0f);
            int yOffset = -(index / numPerRow);
            float offsetX = size * (index % numPerRow) - existingOffsetX;
            float offsetY = size * yOffset - existingOffsetY;
            button.getPosition().setXAlignOffset(offsetX);
            button.getPosition().setYAlignOffset(offsetY);
            tooltip.addSpacer(-size);
            return new Pair<>(offsetX+existingOffsetX, offsetY+existingOffsetY);
        }
        return new Pair<>(0f, 0f);
    }

    private Pair<Float, Float> addFleetMemberHighlight(
            TooltipMakerAPI tooltip,
            List<FleetMemberAPI> combinedList,
            FactionAPI combinedFaction,
            FleetMemberAPI member,
            int numPerRow,
            float size) {
        int index = combinedList.indexOf(member);
        return addFleetMemberHighlight(
                tooltip,
                combinedFaction,
                index,
                numPerRow,
                size,
                true,
                0f,
                0f);
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        try {
            findInteractionDialogClassIfNeeded.invoke(panel.getIntelUI());
        } catch (Throwable e) {
            logger.error("Failed to find interaction dialog class", e);
        }

        FactionAPI faction = fleets.get(0).getFaction();
        float shipsPanelWidth = width - 350f;
        CustomPanelAPI shipsPanel = panel.createCustomPanel(shipsPanelWidth, height, null);

        float membersListHeight = selectedFleetMember == null ? height : (int) ((height+1f)/2f);
        CustomPanelAPI membersListPanel = shipsPanel.createCustomPanel(shipsPanelWidth, membersListHeight, null);

        TooltipMakerAPI membersHeader = membersListPanel.createUIElement(shipsPanelWidth, 20f, false);
        membersHeader.addSectionHeading(Strings.RecentBattles.fleetDataHeader, faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
        membersListPanel.addUIElement(membersHeader).inTL(0f, 0f);
        TooltipMakerAPI membersList = membersListPanel.createUIElement(shipsPanelWidth, membersListHeight - 20f, true);
        List<FleetMemberAPI> members = new ArrayList<>();

        for (CampaignFleetAPI fleet : fleets) {
            members.addAll(fleet.getFleetData().getMembersListCopy());
        }
        members.sort(Utils.byDPComparator);
        float size = 60f;
        int numPerRow = Math.max(1, (int) ((shipsPanelWidth / size)));
        float offsetX = 0f, offsetY = 0f;
        if (selectedFleetMember != null) {
            Pair<Float, Float> offsets = addFleetMemberHighlight(
                    membersList,
                    members,
                    faction,
                    selectedFleetMember,
                    numPerRow,
                    size);
            offsetX = offsets.one;
            offsetY = offsets.two;
            if (selectedFleetMember.isFlagship()) {
                for (int i = 0; i < members.size(); i++) {
                    FleetMemberAPI member = members.get(i);
                    if (member != selectedFleetMember &&
                            member.getFleetData().getFleet() == selectedFleetMember.getFleetData().getFleet()) {
                        Pair<Float, Float> offsets2 = addFleetMemberHighlight(
                                membersList,
                                faction,
                                i,
                                numPerRow,
                                size,
                                false,
                                offsetX,
                                offsetY);
                        offsetX = offsets2.one;
                        offsetY = offsets2.two;
                    }
                }
            }
        }

        addShipsList(this, panel.getIntelUI(), membersList, members, numPerRow, size, faction.getBaseUIColor());
        membersList.getPrev().getPosition().setXAlignOffset(-offsetX);
        membersList.getPrev().getPosition().setYAlignOffset(-offsetY);
        membersListPanel.addUIElement(membersList).inTL(0f, 20f);
        membersList.getExternalScroller().setYOffset(Math.max(0f, Math.min(savedScrollbarLocation, membersList.getHeightSoFar()-membersListHeight+20f)));
        membersListTooltip = membersList;
        shipsPanel.addComponent(membersListPanel).inTL(0f, 0f);

        if (selectedFleetMember != null) {
            FactionAPI specificFaction = selectedFleetMember.getFleetData().getFleet().getFaction();
            CustomPanelAPI selectedPanel = shipsPanel.createCustomPanel(shipsPanelWidth, (int) (height/2f), null);
            TooltipMakerAPI moreInfoHeader = selectedPanel.createUIElement(shipsPanelWidth, 20f, false);
            String officerDataTitle = selectedFleetMember.getCaptain().isDefault()
                    ? Strings.RecentBattles.officerDataTitle
                    : String.format(Strings.RecentBattles.officerDataTitleWithName, selectedFleetMember.getCaptain().getNameString());
            moreInfoHeader.addSectionHeading(officerDataTitle, specificFaction.getBaseUIColor(), specificFaction.getDarkUIColor(), Alignment.MID, 0f);
            selectedPanel.addUIElement(moreInfoHeader).inTL(0f, 0f);

            float bigShipSize = 150f;
            TooltipMakerAPI bigShipDisplay = selectedPanel.createUIElement(bigShipSize, bigShipSize, false);
            List<FleetMemberAPI> singleton = Collections.singletonList(selectedFleetMember);
            bigShipDisplay.addShipList(1, 1, bigShipSize, specificFaction.getBaseUIColor(), singleton, 0f);
            selectedPanel.addUIElement(bigShipDisplay).inLMid(25f);

            TooltipMakerAPI officerDisplay = selectedPanel.createUIElement(shipsPanelWidth - bigShipSize - 50f, height/2f - 35f, true);

            if (selectedFleetMember.isFlagship()) {
                Set<SkillSpecAPI> tempModifiedSkills = new HashSet<>();
                for (String id : Global.getSettings().getSkillIds()) {
                    SkillSpecAPI spec = Global.getSettings().getSkillSpec(id);
                    if (spec.isAdmiralSkill() && !spec.isCombatOfficerSkill()) {
                        spec.setCombatOfficerSkill(true);
                        tempModifiedSkills.add(spec);
                    }
                    if (spec.isCombatOfficerSkill() && !spec.isAdmiralSkill()) {
                        spec.setCombatOfficerSkill(false);
                        tempModifiedSkills.add(spec);
                    }
                }
                officerDisplay.addSectionHeading(
                        Strings.RecentBattles.admiralSkillsTitle,
                        specificFaction.getBaseUIColor(),
                        specificFaction.getDarkUIColor(),
                        Alignment.MID,
                        15f);
                officerDisplay.addSkillPanel(selectedFleetMember.getCaptain(), 0f);

                for (SkillSpecAPI spec : tempModifiedSkills) {
                    spec.setCombatOfficerSkill(!spec.isCombatOfficerSkill());
                }
            }

            officerDisplay.addSectionHeading(
                    Strings.RecentBattles.combatSkillsTitle,
                    specificFaction.getBaseUIColor(),
                    specificFaction.getDarkUIColor(),
                    Alignment.MID,
                    15f);
            officerDisplay.addSkillPanel(selectedFleetMember.getCaptain(), 0f);

            selectedPanel.addUIElement(officerDisplay).inBR(10f,10f);
            shipsPanel.addComponent(selectedPanel).belowMid(membersListPanel, 0f);
        }

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
        buttons.addButton(Strings.RecentBattles.replayButton, REPLAY_BUTTON_ID, 300f, 20f, 40f).setShortcut(20, true);
        buttons.addPara(Strings.RecentBattles.replayDesc, 10f);
        ButtonAPI soloButton = buttons.addButton(Strings.RecentBattles.soloReplayButton, SOLO_REPLAY_BUTTON_ID, 300f, 20f, 40f);
        soloButton.setShortcut(21, true);
        soloButton.setEnabled(selectedFleetMember != null);
        buttons.addPara(Strings.RecentBattles.soloReplayDesc, 10f);
        buttons.addButton(isImportant() ? Strings.RecentBattles.unpinButton : Strings.RecentBattles.pinButton, SAVE_BUTTON_ID, 300f, 20f, 40f).setShortcut(22, true);
        buttons.addPara(Strings.RecentBattles.pinDesc, 10f, Misc.getHighlightColor(), "" + MAX_SIZE);
        buttons.addSpacer(20f);
        addDeleteButton(buttons, 300f, Strings.RecentBattles.deleteButton);
        buttonsPanel.addUIElement(buttons).inMid();
        panel.addComponent(buttonsPanel).rightOfMid(shipsPanel, 0f);
    }

    @Override
    protected void createDeleteConfirmationPrompt(TooltipMakerAPI prompt) {
        prompt.addPara(Strings.RecentBattles.deleteConfirm, Misc.getTextColor(), 0f);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (REPLAY_BUTTON_ID.equals(buttonId)) {
            try {
                BattleAPI battle = Global.getFactory().createBattle(Global.getSector().getPlayerFleet(), fleets.get(0));
                for (int i = 1; i < fleets.size(); i++) {
                    battle.join(fleets.get(i), BattleAPI.BattleSide.TWO);
                }
                // Fleet is not alive (location is null), so battle will see it as empty
                battle.genCombinedDoNotRemoveEmpty();
                replayBattle.invoke(
                        RecentBattlesTracker.cloneContextAlwaysAttack(
                                bccStub, Global.getSector().getPlayerFleet(), battle.getNonPlayerCombined()),
                        (Action) this::repairFleets);
                battle.finish(null, false);
            }
            catch (Throwable e) {
                logger.error("Failed to replay battle: ", e);
            }
        }
        else if (SOLO_REPLAY_BUTTON_ID.equals(buttonId)) {
            if (selectedFleetMember == null) return;
            final CampaignFleetAPI tempFleet = Global.getFactory().createEmptyFleet(
                    selectedFleetMember.getFleetData().getFleet().getFaction().getId(), "temp", true);
            FleetMemberAPI tempMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, selectedFleetMember.getVariant());
            tempMember.setCaptain(selectedFleetMember.getCaptain());
            tempMember.setFleetCommanderForStats(selectedFleetMember.getFleetCommander(), null);
            tempMember.getCrewComposition().setCrew(selectedFleetMember.getCrewComposition().getCrew());
            tempFleet.getFleetData().addFleetMember(tempMember);
            tempMember.getStatus().repairFully();
            tempMember.getRepairTracker().setCR(tempMember.getRepairTracker().getMaxCR());
            BattleAPI battle = Global.getFactory().createBattle(Global.getSector().getPlayerFleet(), tempFleet);
            BattleCreationContext newContext = RecentBattlesTracker.cloneContextAlwaysAttack(
                    bccStub, Global.getSector().getPlayerFleet(), tempFleet);
            newContext.objectivesAllowed = false;
            newContext.aiRetreatAllowed = false;
            newContext.fightToTheLast = true;
            newContext.enemyDeployAll = true;
            try {
                replayBattle.invoke(
                        newContext,
                        (Action) () -> {
                            repairFleets();
                            tempFleet.getFleetData().clear();
                        });
                battle.finish(null, false);
            }
            catch (Throwable e) {
                logger.error("Failed to replay battle: ", e);
            }
        }
        else if (SAVE_BUTTON_ID.equals(buttonId)) {
            setImportant(!isImportant());
            try {
                if (membersListTooltip != null) {
                    saveScrollbarLocationHandle.invoke(this, membersListTooltip);
                }
            }
            catch (Throwable e) {
                logger.error("Failed to save scrollbar location, e");
            }
            ui.updateUIForItem(this);
        }
        else if (BUTTON_DELETE.equals(buttonId)) {
            endImmediately();
            ui.recreateIntelUI();
        }
    }

    private void repairFleets() {
        for (CampaignFleetAPI fleet : fleets) {
            for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                // Stations have their own CR calculation based on market conditions (see OrbitalStation.getCR)
                // Intel isn't going to save the market, so just set all station CR to 1
                if (fm.isStation()) {
                    fm.getRepairTracker().setCR(1f);
                }
                else {
                    fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
                }
                fm.getStatus().repairFully();
                fm.getStatus().resetDamageTaken();
            }
        }
    }
}
