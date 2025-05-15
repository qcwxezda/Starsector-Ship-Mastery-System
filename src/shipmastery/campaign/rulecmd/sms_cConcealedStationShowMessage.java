package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.misc.FleetLogIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class sms_cConcealedStationShowMessage extends BaseCommandPlugin {

    public static final Color TEXT_COLOR = Utils.mixColor(Misc.getHighlightColor(), Misc.getGrayColor(), 0.25f);
    public static final Color HIGHLIGHT_COLOR = Utils.mixColor(Misc.getHighlightColor(), Color.WHITE, 0.75f);

    public static class ConcealedStationMessageIntel extends FleetLogIntel {
        int messageIndex;

        public ConcealedStationMessageIntel(int messageIndex) {
            this.messageIndex = messageIndex;
        }

        @Override
        protected String getName() {
            return String.format(Strings.Campaign.fleetLogIntelTitle, messageIndex + 1);
        }

        @Override
        public boolean hasSmallDescription() {
            return false;
        }

        @Override
        public boolean hasLargeDescription() {
            return true;
        }

        @Override
        public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
            String logoRot1 = "graphics/factions/sms_logo_curator_rot1.png";
            String logoRot2 = "graphics/factions/sms_logo_curator_rot2.png";
            particleengine.Utils.getLoadedSprite(logoRot1);
            particleengine.Utils.getLoadedSprite(logoRot2);
            TooltipMakerAPI crestTtm1 = panel.createUIElement(256f, 410f, false);
            crestTtm1.addImage(logoRot1, 0f);
            panel.addUIElement(crestTtm1).inLMid(30f);

            TooltipMakerAPI crestTtm2 = panel.createUIElement(256f, 410f, false);
            crestTtm2.addImage(logoRot2, 0f);
            panel.addUIElement(crestTtm2).inRMid(40f);

            TooltipMakerAPI ttm = panel.createUIElement(600f, height - 150f, true);
            ttm.addSectionHeading(String.format(Strings.Campaign.fleetLogIntelHeader, messageIndex+1), Alignment.MID, 0f);
            ttm.setParaFont(Fonts.ORBITRON_12);
            FormattedMessage fmt = addAndGetFormattedMessage(null, 600f, Strings.Campaign.messages[messageIndex], messageIndex);
            ttm.addPara(fmt.fmt, 20f, TEXT_COLOR, HIGHLIGHT_COLOR, fmt.highlights);
            TooltipMakerAPI button = panel.createUIElement(150f, height, false);
            button.addButton(Strings.Campaign.fleetLogDeleteButton, BUTTON_DELETE, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, 150f, 30f, 20f);
            panel.addUIElement(ttm).inTMid(20f);
            panel.addUIElement(button).inTMid(height - 130f);
        }

        @Override
        public String getIcon() {
            return "graphics/factions/sms_crest_curator.png";
        }
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var textPanel = dialog.getTextPanel();
        textPanel.setFontOrbitron();

        var localMemory = memoryMap.get(MemKeys.LOCAL);
        var globalMem = memoryMap.get(MemKeys.GLOBAL);

        var seenIndex = localMemory.get(Strings.Campaign.STATION_SEEN_MESSAGE_INDEX);
        if (seenIndex != null) {
            addAndGetFormattedMessage(textPanel, dialog.getTextWidth(), Strings.Campaign.messages[(int) seenIndex], (Integer) seenIndex);
        } else {
            int seen = memoryMap.get(MemKeys.GLOBAL).getInt(Strings.Campaign.NUM_MESSAGES_SEEN);
            if (seen >= Strings.Campaign.messages.length) return false;
            var message = Strings.Campaign.messages[seen];
            localMemory.set(Strings.Campaign.STATION_SEEN_MESSAGE_INDEX, seen);
            var intel = new ConcealedStationMessageIntel(seen);
            intel.setImportant(true);
            Global.getSector().getIntelManager().addIntel(intel, false, textPanel);
            addAndGetFormattedMessage(textPanel, dialog.getTextWidth(), message, seen);
        }

        textPanel.setFontInsignia();
        return true;
    }

    private static void appendToStringAndHighlight(StringBuilder sb, StringBuilder highlight, String str) {
        sb.append(str);
        if (highlight != null) {
            highlight.append(str);
        }
    }

    public static class FormattedMessage {
        String fmt;
        String[] highlights;

        public FormattedMessage(String fmt, String[] highlights) {
            this.fmt = fmt;
            this.highlights = highlights;
        }
    }

    public static FormattedMessage addAndGetFormattedMessage(@Nullable TextPanelAPI panel, float textWidth, String message, int messageIndex) {
        Random random = new Random((Global.getSector().getSeedString() + "_" + messageIndex).hashCode());
        StringBuilder sb = new StringBuilder();
        List<StringBuilder> highlights = new ArrayList<>();
        StringBuilder currentHighlight = null;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '<') {
                int j = message.indexOf('>', i + 1);
                String tag = message.substring(i + 1, j);
                switch (tag) {
                    case "br" -> {
                        String br = Utils.makeLineBreak(textWidth - 30f, Fonts.ORBITRON_12);
                        appendToStringAndHighlight(sb, currentHighlight, "\n");
                        appendToStringAndHighlight(sb, currentHighlight, br);
                        appendToStringAndHighlight(sb, currentHighlight, "\n");
                    }
                    case "h" -> currentHighlight = new StringBuilder();
                    case "/h" -> {
                        highlights.add(currentHighlight);
                        currentHighlight = null;
                    }
                    case "name" ->
                            appendToStringAndHighlight(sb, currentHighlight, Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(random).getName().getFirst());
                    case "fullName" ->
                            appendToStringAndHighlight(sb, currentHighlight, Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(random).getNameString());
                    case "nucleusLoc" -> {
                        Vector2f loc = Utils.toLightyears((Vector2f) Global.getSector().getMemoryWithoutUpdate().get(Strings.Campaign.NUCLEUS_LOCATION));
                        String str = String.format("(%.1f, %.1f)", loc.x, loc.y);
                        appendToStringAndHighlight(sb, currentHighlight, str);
                    }
                    case "remoteBeaconLoc1" -> {
                        Vector2f loc = Utils.toLightyears((Vector2f) Global.getSector().getMemoryWithoutUpdate().get(Strings.Campaign.REMOTE_BEACON_LOCATION));
                        String str = String.format("(%.1f, %.1f)", loc.x + 3f, loc.y);
                        appendToStringAndHighlight(sb, currentHighlight, str);
                    }
                    case "remoteBeaconLoc2" -> {
                        Vector2f loc = Utils.toLightyears((Vector2f) Global.getSector().getMemoryWithoutUpdate().get(Strings.Campaign.REMOTE_BEACON_LOCATION));
                        String str = String.format("(%.1f, %.1f)", loc.x + 7.3f, loc.y);
                        appendToStringAndHighlight(sb, currentHighlight, str);
                    }
                    default -> appendToStringAndHighlight(sb, currentHighlight, "<" + tag + ">");
                }
                i = j;
                continue;
            }
            appendToStringAndHighlight(sb, currentHighlight, String.valueOf(c));
        }
        var highlightStrings = highlights.stream().map(StringBuilder::toString).toArray(String[]::new);
        String str = sb.toString();
        if (panel != null) {
            panel.addPara(str, TEXT_COLOR, HIGHLIGHT_COLOR, highlightStrings);
        }
        return new FormattedMessage(str, highlightStrings);
    }
}
