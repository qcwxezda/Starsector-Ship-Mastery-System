package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.loading.SkillSpec;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import shipmastery.achievements.AmorphousPseudocoreUsed;
import shipmastery.achievements.PseudocoreCrewedShip;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.campaign.items.PseudocorePlugin;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.hullmods.PseudocoreUplinkHullmod;
import shipmastery.util.CampaignUtils;
import shipmastery.util.FleetMemberTooltipCreator;
import shipmastery.util.IntRef;
import shipmastery.util.OnShipButtonClicked;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class sms_cPseudocoreUplink extends BaseCommandPlugin {

    public static final String CORE_BUTTON_PREFIX = "sms_core_button";
    public static final String SKILL_BUTTON_PREFIX = "sms_skill_button";
    public static final String FLEET_MEMBER_BUTTON_PREFIX = "sms_fm_button";
    private static MethodHandle setMaxShadowHeight = null;
    private static final MethodHandle addTooltipAbove;
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    public static Logger logger = Logger.getLogger(sms_cPseudocoreUplink.class);

    static {
        try {
            var temp = Global.getSettings().createCustom(0f, 0f, null);
            Class<?> tooltipUIInterface = temp.getClass().getSuperclass().getSuperclass().getInterfaces()[3];
            addTooltipAbove = lookup.findStatic(StandardTooltipV2Expandable.class, "addTooltipAbove", MethodType.methodType(void.class, tooltipUIInterface, StandardTooltipV2.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        try {
            ((List<?>) ReflectionUtils.uiPanelGetChildrenNonCopy.invoke(dialog)).clear();
        } catch (Throwable e) {
            logger.error("Couldn't clear dialog panel's children", e);
        }
        RuleBasedInteractionDialogPluginImpl plugin = (RuleBasedInteractionDialogPluginImpl) dialog.getPlugin();
        SpecialItemPlugin.RightClickActionHelper helper = (SpecialItemPlugin.RightClickActionHelper) plugin.getCustom1();

        // Prevent losing cores if player fleet has no flagship and core is added to first member in fleet
        var playerFlagship = Global.getSector().getPlayerFleet().getFlagship();
        if (!playerFlagship.isFlagship()) {
            playerFlagship.setFlagship(true);
        }
        Global.getSoundPlayer().playUISound("ui_noise_static_message_quiet", 1f, 1f);

        boolean isMk2 = memoryMap.get(MemKeys.LOCAL).getBoolean(PseudocoreUplinkPlugin.IS_MK2_MEM_KEY);
        dialog.showCustomDialog(880f, 600f, new CorePickerDialog(helper, dialog, isMk2));
        return true;
    }

    private static class CorePickerDialog extends BaseCustomDialogDelegate {

        private final boolean allowAnyCore;
        private CorePickerPlugin plugin = null;
        private final SpecialItemPlugin.RightClickActionHelper helper;
        private final InteractionDialogAPI dialog;

        public CorePickerDialog(SpecialItemPlugin.RightClickActionHelper helper, InteractionDialogAPI dialog, boolean allowAnyCore) {
            this.allowAnyCore = allowAnyCore;
            this.helper = helper;
            this.dialog = dialog;
        }

        private <T> List<ButtonAPI> addAreaCheckboxesWithImage(
                TooltipMakerAPI tooltip,
                String buttonIdPrefix,
                List<? extends T> items,
                float itemSize,
                float pad,
                int buttonsPerRow,
                Action<? super T> perItemAction) {
            var bc = Misc.getBasePlayerColor();
            var dc = Misc.getDarkPlayerColor();
            var hc = Misc.getBrightPlayerColor();
            List<ButtonAPI> buttons = new ArrayList<>();
            int rowCount = 0;
            boolean isFirst = true;
            float prevOffset = 0f;
            float totalAlignOffset = 0f;
            for (int i = 0; i < items.size(); i++) {
                T item = items.get(i);
                buttons.add(tooltip.addAreaCheckbox("", buttonIdPrefix + i, bc, dc, hc, itemSize, itemSize, -itemSize));

                if (isFirst) {
                    tooltip.getPrev().getPosition().setXAlignOffset(pad);
                    totalAlignOffset += pad;
                } else if (rowCount != 0) {
                    tooltip.getPrev().getPosition().setXAlignOffset(pad + itemSize - prevOffset);
                    totalAlignOffset += pad + itemSize - prevOffset;
                } else {
                    float xAlignOffset = -(pad + itemSize) * (buttonsPerRow - 1) - prevOffset;
                    tooltip.getPrev().getPosition().setXAlignOffset(xAlignOffset);
                    totalAlignOffset += xAlignOffset;
                }

                prevOffset = perItemAction.perform(item);

                isFirst = false;
                rowCount++;
                if (rowCount == buttonsPerRow && i < items.size() - 1) {
                    rowCount = 0;
                    tooltip.addSpacer(itemSize + pad);
                }
            }

            // So that the next items created from tooltip won't have the alignment offset
            tooltip.addSpacer(0f).getPosition().setXAlignOffset(-totalAlignOffset);
            return buttons;
        }

        private interface Action<T> {
            // Return the additional x-align offset added by this call that will need to be adjusted for in the next button
            float perform(T t);
        }

        private void removeScrollerShadow(ScrollPanelAPI scrollPanel) {
            if (setMaxShadowHeight == null) {
                try {
                    setMaxShadowHeight = lookup.findVirtual(scrollPanel.getClass(), "setMaxShadowHeight", MethodType.methodType(void.class, float.class));
                } catch (Exception e) {
                    logger.error("Couldn't set max shadow height", e);
                    return;
                }
            }
            if (setMaxShadowHeight != null) {
                try {
                    setMaxShadowHeight.invoke(scrollPanel, 0f);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private Pair<PositionAPI, LabelAPI> createOutlineAndTitle(CustomPanelAPI panel, String titleText, float width, float height, float pad) {
            var bc = Misc.getBasePlayerColor();
            var dc = Misc.getDarkPlayerColor();
            var hc = Misc.getBrightPlayerColor();

            var outlineMaker = panel.createUIElement(width, height, false);
            var outline = outlineMaker.addAreaCheckbox("", null, bc, dc, hc, width, height, 0f);
            outline.setClickable(false);
            outline.setGlowBrightness(0f);
            outline.setMouseOverSound(null);
            outlineMaker.setParaFont(Fonts.ORBITRON_20AA);
            var label = outlineMaker.addPara(titleText, hc, pad-height);
            label.setAlignment(Alignment.MID);
            return new Pair<>(panel.addUIElement(outlineMaker), label);
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {

            // AI Core selection buttons
            float corePanelWidth = 306f, corePanelHeight = 188f, itemPad = 10f;
            createOutlineAndTitle(panel, allowAnyCore ? Strings.Items.uplinkMk2CoreSelect : Strings.Items.uplinkPseudocoreSelect, corePanelWidth, corePanelHeight, itemPad).one.inTL(itemPad, itemPad);
            float itemSize = 64f;
            var corePicker = panel.createUIElement(corePanelWidth, corePanelHeight-20f-2f*itemPad, true);
            //corePicker.getPosition().setXAlignOffset(-itemPad);
            int itemsPerRow = (int) corePanelWidth / (int) itemSize;

            var coreStacks = Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                    .stream()
                    .<Map.Entry<CommoditySpecAPI, CargoStackAPI>>mapMulti((stack, consumer) -> {
                        var id = stack.getCommodityId();
                        if (id == null) return;
                        var spec = Global.getSettings().getCommoditySpec(stack.getCommodityId());
                        if (spec == null) return;
                        var plugin = Misc.getAICoreOfficerPlugin(stack.getCommodityId());
                        if (plugin == null) return;
                        if (!allowAnyCore && !(plugin instanceof PseudocorePlugin)) return;
                        consumer.accept(Map.entry(spec, stack));
                    })
                    .<TreeMap<CommoditySpecAPI, CargoStackAPI>>collect(() -> new TreeMap<>(
                                    Comparator.comparingDouble(CommoditySpecAPI::getOrder)
                                            .thenComparing((x, y) -> CharSequence.compare(x.getId(), y.getId()))),
                            (map, entry) -> map.putIfAbsent(entry.getKey(), entry.getValue()),
                            Map::putAll)
                    .values()
                    .stream()
                    .toList();

            for (var stack : coreStacks) {
                var spec = Global.getSettings().getCommoditySpec(stack.getCommodityId());
                var aiPlugin = ((CampaignEngine) Global.getSector()).getModAndPluginData().pickAICoreOfficerPlugin(spec.getId());
                plugin.corePlugins.add(aiPlugin);
                plugin.coreIds.add(spec.getId());
            }
            corePicker.addSpacer(itemSize);
            plugin.coreButtons = addAreaCheckboxesWithImage(corePicker, CORE_BUTTON_PREFIX, coreStacks, itemSize, itemPad, itemsPerRow, stack -> {
                var spec = Global.getSettings().getCommoditySpec(stack.getCommodityId());
                corePicker.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                    @Override
                    public boolean isTooltipExpandable(Object tooltipParam) {
                        return false;
                    }

                    @Override
                    public float getTooltipWidth(Object tooltipParam) {
                        return 600f;
                    }

                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addTitle(spec.getName());
                        var strings = Global.getSettings().getDescription(spec.getId(), Description.Type.RESOURCE).getText1Paras();
                        String str = "";
                        if (!strings.isEmpty()) {
                            str += strings.get(0);
                        }
                        if (strings.size() > 1) {
                            str += "\n\n";
                            str += strings.get(1);
                        }
                        tooltip.addPara(str, 10f);
                        ListenerUtil.addCommodityTooltipSectionAfterPrice(tooltip, 600f, false, stack);
                        tooltip.addPara(Strings.Items.cargoCount, 10f, Misc.getGrayColor(), Misc.getHighlightColor(), "" + (int) helper.getNumItems(CargoAPI.CargoItemType.RESOURCES, spec.getId()));
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);

                corePicker.addImage(spec.getIconName(), itemSize, itemSize, -itemSize);
                return 0f;
            });

            corePicker.addSpacer(itemPad);
            panel.addUIElement(corePicker).inTL(itemPad, itemPad + 20f + 2f * itemPad);
            removeScrollerShadow(corePicker.getExternalScroller());

            // Skill selection buttons
            float skillPanelWidth = 528f, skillPanelHeight = 188f;
            int skillsPerRow = 7;
            var pair = createOutlineAndTitle(panel, "", skillPanelWidth, skillPanelHeight, itemPad);
            pair.one.inTR(2f*itemPad, itemPad);
            plugin.skillsTitle = pair.two;
            plugin.skillsPos = pair.one;
            plugin.updateTitleLabel();
            var skillPicker = panel.createUIElement(skillPanelWidth, skillPanelHeight-20f-2f*itemPad, true);
            List<String> ids = new ArrayList<>(Utils.combatSkillIds);
            skillPicker.addSpacer(itemSize);
            IntRef curIndex = new IntRef(0);
            plugin.skillButtons = addAreaCheckboxesWithImage(skillPicker, SKILL_BUTTON_PREFIX, ids, itemSize, itemPad, skillsPerRow, id -> {
                plugin.skillButtonIndicesById.put(id, curIndex.value);
                curIndex.value++;

                var spec = Global.getSettings().getSkillSpec(id);
                skillPicker.addImage(spec.getSpriteName(), itemSize, itemSize, -itemSize);
                var prev = skillPicker.getPrev();
                plugin.skillImages.add(prev);
                prev.setOpacity(0.25f);
                try {
                    addTooltipAbove.invoke(prev,
                            ReflectionUtils.createSkillTooltip.invoke(
                                    (SkillSpec) spec,
                                    (CharacterStats) Global.getSector().getPlayerStats(),
                                    800f,
                                    300f,
                                    true,
                                    false,
                                    0,
                                    null
                    ));
                } catch (Throwable e) {
                    logger.error("Couldn't modify skill buttons", e);
                }

                skillPicker.addImage(Utils.eliteIcons.get(spec.getGoverningAptitudeId()), itemSize+6f, itemSize+6f, -itemSize-6f);
                prev = skillPicker.getPrev();
                plugin.skillEliteIcons.add(prev);
                prev.getPosition().setXAlignOffset(-3f);
                prev.setOpacity(0f);
                return -3f;
            });
            skillPicker.addSpacer(itemPad);
            panel.addUIElement(skillPicker).inTR(2f*itemPad, itemPad + 20f + 2f * itemPad);
            removeScrollerShadow(skillPicker.getExternalScroller());

            // Fleet member selection buttons
            float fleetPanelWidth = 850f, fleetPanelHeight = 388f;
            int fleetMembersPerRow = 6;
            float fleetMemberIconSize = 2f*itemSize + 4f;
            var pos = createOutlineAndTitle(panel, Strings.Misc.selectAShip, fleetPanelWidth, fleetPanelHeight, itemPad).one;
            pos.inBL(itemPad, 0f);
            plugin.fleetPos = pos;
            var fleetMemberPicker = panel.createUIElement(fleetPanelWidth-15f, fleetPanelHeight-20f-2f*itemPad, true);
            List<FleetMemberAPI> fms = Global
                    .getSector()
                    .getPlayerFleet()
                    .getFleetData()
                    .getMembersListCopy()
                    .stream()
                    .filter(x -> !Misc.isAutomated(x) && !x.isFlagship() && !Misc.isUnremovable(x.getCaptain()))
                    .toList();

            fleetMemberPicker.addSpacer(fleetMemberIconSize + itemPad/2f);
            addAreaCheckboxesWithImage(fleetMemberPicker, "sms_fm_button", fms, fleetMemberIconSize-itemPad, itemPad, fleetMembersPerRow, fm -> {
                var prev = fleetMemberPicker.getPrev();
                prev.setOpacity(0f);
                plugin.fmButtons.put(fm, (ButtonAPI) prev);
                return 0f;
            });

            fleetMemberPicker.addSpacer(-fleetMemberPicker.getHeightSoFar()).getPosition().setXAlignOffset(itemPad/2f);

            fleetMemberPicker.addShipList(fleetMembersPerRow, (fms.size() + fleetMembersPerRow - 1) / fleetMembersPerRow, fleetMemberIconSize, Misc.getBasePlayerColor(), fms, itemPad);
            try {
                FleetMemberTooltipCreator.modifyShipButtons.invoke(fleetMemberPicker, fleetMemberPicker.getPrev(), (OnShipButtonClicked) (fm, args) -> {
                    if (plugin.checkedCoreButtonIndex < 0) return;
                    if (plugin.selectedFleetMember == fm) {
                        var keepChecked = plugin.fmButtons.get(fm);
                        if (keepChecked != null) keepChecked.setChecked(true);
                        return;
                    }
                    if (plugin.selectedFleetMember != null) {
                        var deselect = plugin.fmButtons.get(plugin.selectedFleetMember);
                        if (deselect != null) {
                            deselect.setChecked(false);
                            deselect.setOpacity(0f);
                        }
                    }
                    var button = plugin.fmButtons.get(fm);
                    if (button != null) {
                        button.setChecked(true);
                        button.setOpacity(1f);
                        plugin.selectedFleetMember = fm;
                    }
                    plugin.regenerateSkills(false);
                    plugin.updateConfirmButton();
                });
            } catch (Throwable e) {
                logger.error("Couldn't modify fleet member buttons", e);
            }

            panel.addUIElement(fleetMemberPicker).inBL(2f*itemPad+10f, itemPad);
            removeScrollerShadow(fleetMemberPicker.getExternalScroller());

            try {
                plugin.confirmButton = (ButtonAPI) ((List<?>) ReflectionUtils.uiPanelGetChildrenNonCopy.invoke(ReflectionUtils.uiPanelGetParent.invoke(panel))).get(1);
                plugin.updateConfirmButton();
            } catch (Throwable e) {
                logger.error("Could not modify confirm button", e);
            }
        }

        @Override
        public boolean hasCancelButton() {
            return true;
        }

        @Override
        public String getConfirmText() {
            return Strings.Misc.confirm;
        }

        @Override
        public String getCancelText() {
            return Strings.Misc.cancel;
        }

        @Override
        public void customDialogCancel() {
            dialog.dismissAsCancel();
        }

        @Override
        public void customDialogConfirm() {
            if (plugin.selectedFleetMember == null || plugin.person == null || plugin.checkedCoreButtonIndex < 0) {
                dialog.dismissAsCancel();
                return;
            }
            plugin.person.getStats().setSkipRefresh(true);
            // Only have to check the skills in the map as there's no way for the player to modify any other skills
            for (var entry : plugin.skillButtonIndicesById.entrySet()) {
                plugin.person.getStats().setSkillLevel(entry.getKey(), plugin.skillButtons.get(entry.getValue()).isChecked() ? 2f : 0f);
            }
            plugin.person.getStats().setSkipRefresh(false);

            // If the displaced officer was a core, add that core back to cargo
            var existingCaptain = plugin.selectedFleetMember.getCaptain();
            if (existingCaptain != null && existingCaptain.isAICore()) {
                Global.getSector().getPlayerFleet().getCargo().addCommodity(existingCaptain.getAICoreId(), 1);
            }

            var coreId = plugin.coreIds.get(plugin.checkedCoreButtonIndex);
            plugin.person.getMemoryWithoutUpdate().set(PseudocoreUplinkHullmod.USED_UPLINK_MEM_KEY, coreId);
            plugin.selectedFleetMember.setCaptain(plugin.person);
            CampaignUtils.addPermaModCloneVariantIfNeeded(plugin.selectedFleetMember, "sms_pseudocore_uplink_handler", false);

            helper.removeFromAnyStack(CargoAPI.CargoItemType.RESOURCES, coreId, 1);
            Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
            dialog.dismiss();

            // Update achievements
            UnlockAchievementAction.unlockWhenUnpaused(PseudocoreCrewedShip.class);
            if (AmorphousPseudocoreUsed.canCompleteAchievement(plugin.selectedFleetMember)) {
                UnlockAchievementAction.unlockWhenUnpaused(AmorphousPseudocoreUsed.class);
            }
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            if (plugin == null) {
                plugin = new CorePickerPlugin(this);
            }
            return plugin;
        }
    }

    private static class CorePickerPlugin extends BaseCustomUIPanelPlugin {
        List<AICoreOfficerPlugin> corePlugins = new ArrayList<>();
        List<ButtonAPI> coreButtons = new ArrayList<>();
        List<String> coreIds = new ArrayList<>();

        List<ButtonAPI> skillButtons = new ArrayList<>();
        List<UIComponentAPI> skillImages = new ArrayList<>();
        List<UIComponentAPI> skillEliteIcons = new ArrayList<>();

        Map<String, Integer> skillButtonIndicesById = new LinkedHashMap<>();

        Map<FleetMemberAPI, ButtonAPI> fmButtons = new HashMap<>();

        LabelAPI skillsTitle;
        int checkedCoreButtonIndex = -1;
        int checkedFleetMemberIndex = -1;
        int numCheckedSkills = 0;
        int maxSkills = 0;
        FleetMemberAPI selectedFleetMember = null;
        ButtonAPI confirmButton;

        PersonAPI person;
        final CorePickerDialog dialog;
        PositionAPI skillsPos, fleetPos;

        @Override
        public void render(float alphaMult) {
            if (checkedCoreButtonIndex >= 0 || skillsPos == null || fleetPos == null) return;
            float sxm = skillsPos.getX(), sxM = sxm + skillsPos.getWidth() + 10f;
            float sym = skillsPos.getY(), syM = sym + skillsPos.getHeight();
            float fxm = fleetPos.getX(), fxM = fxm + fleetPos.getWidth() + 10f;
            float fym = fleetPos.getY(), fyM = fym + fleetPos.getHeight();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(0f, 0f, 0f, 0.8f);
            GL11.glVertex2f(sxm, sym);
            GL11.glVertex2f(sxm, syM);
            GL11.glVertex2f(sxM, syM);
            GL11.glVertex2f(sxM, sym);
            GL11.glVertex2f(fxm, fym);
            GL11.glVertex2f(fxm, fyM);
            GL11.glVertex2f(fxM, fyM);
            GL11.glVertex2f(fxM, fym);
            GL11.glEnd();
        }

        public CorePickerPlugin(CorePickerDialog dialog) {
            this.dialog = dialog;
        }

        private void updateTitleLabel() {
            skillsTitle.setText(String.format(Strings.Items.uplinkSkillSelect, numCheckedSkills, maxSkills));
            skillsTitle.setHighlight("" + numCheckedSkills, "" + maxSkills);
        }

        private void updateConfirmButton() {
            if (confirmButton == null) return;
            confirmButton.setEnabled(selectedFleetMember != null && checkedCoreButtonIndex >= 0);
        }

        private void regenerateSkills(boolean addSkills) {
            if (checkedCoreButtonIndex < 0) return;

            var person = corePlugins.get(checkedCoreButtonIndex).createPerson(
                    coreIds.get(checkedCoreButtonIndex),
                    Global.getSector().getPlayerFaction().getId(),
                    Misc.random);
            this.person = person;

            maxSkills = person.getStats().getLevel();
            int numSkillsNotInList = 0;
            for (var skill : person.getStats().getSkillsCopy()) {
                if (skill.getSkill().isCombatOfficerSkill() && skill.getLevel() > 0f && !skillButtonIndicesById.containsKey(skill.getSkill().getId())) {
                    numSkillsNotInList++;
                }
            }
            maxSkills -= numSkillsNotInList;

            if (maxSkills > numCheckedSkills && addSkills) {
                for (var entry : skillButtonIndicesById.entrySet()) {
                    if (skillButtons.get(entry.getValue()).isChecked()) continue;
                    int skillIndex = entry.getValue();
                    skillButtons.get(skillIndex).setChecked(true);
                    skillImages.get(skillIndex).setOpacity(1f);
                    skillEliteIcons.get(skillIndex).setOpacity(1f);
                    numCheckedSkills++;
                    if (numCheckedSkills >= maxSkills) break;
                }
            } else if (maxSkills < numCheckedSkills) {
                var allEntries = new ArrayList<>(skillButtonIndicesById.entrySet());
                Collections.reverse(allEntries);
                for (var entry : allEntries) {
                    if (!skillButtons.get(entry.getValue()).isChecked()) continue;
                    int skillIndex = entry.getValue();
                    skillButtons.get(skillIndex).setChecked(false);
                    skillImages.get(skillIndex).setOpacity(0.25f);
                    skillEliteIcons.get(skillIndex).setOpacity(0f);
                    numCheckedSkills --;
                    if (numCheckedSkills <= maxSkills) break;
                }
            }

            person.getStats().setSkipRefresh(true);
            for (var entry : skillButtonIndicesById.entrySet()) {
                boolean isChecked = skillButtons.get(entry.getValue()).isChecked();
                person.getStats().setSkillLevel(entry.getKey(), isChecked ? 2f : 0f);
            }
            person.getStats().setSkipRefresh(false);

            updateTitleLabel();
        }

        @Override
        public void buttonPressed(Object buttonId) {
            String id = (String) buttonId;
            int index;
            if (id.startsWith(CORE_BUTTON_PREFIX)) {
                index = Integer.parseInt(id.substring(CORE_BUTTON_PREFIX.length()));
                if (checkedCoreButtonIndex >= 0) {
                    coreButtons.get(checkedCoreButtonIndex).setChecked(false);
                }
                coreButtons.get(index).setChecked(true);
                if (checkedCoreButtonIndex != index) {
                    checkedCoreButtonIndex = index;
                    regenerateSkills(true);
                }
            } else if (id.startsWith(SKILL_BUTTON_PREFIX)) {
                index = Integer.parseInt(id.substring(SKILL_BUTTON_PREFIX.length()));
                if (skillButtons.get(index).isChecked()) {
                    if (numCheckedSkills >= maxSkills) {
                        skillButtons.get(index).setChecked(false);
                        Global.getSoundPlayer().playUISound("ui_char_can_not_increase_skill_or_aptitude", 1f, 1f);
                    } else {
                        numCheckedSkills++;
                        skillImages.get(index).setOpacity(1f);
                        skillEliteIcons.get(index).setOpacity(1f);
                    }
                }
                else {
                    numCheckedSkills--;
                    skillImages.get(index).setOpacity(0.25f);
                    skillEliteIcons.get(index).setOpacity(0f);
                }
            }
            updateTitleLabel();
            updateConfirmButton();
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (var event : events) {
                 if (event.getEventValue() == Keyboard.KEY_SPACE) {
                    if (confirmButton == null || confirmButton.isEnabled()) {
                        dialog.customDialogConfirm();
                    }
                    event.consume();
                }
            }
        }
    }
}
