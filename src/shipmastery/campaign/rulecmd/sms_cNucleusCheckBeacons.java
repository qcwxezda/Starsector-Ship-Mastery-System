package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.util.Misc;
import shipmastery.procgen.Generator;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class sms_cNucleusCheckBeacons extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var panel = dialog.getTextPanel();

        panel.setFontOrbitron();
        panel.addPara(Strings.Campaign.checkingBeacons + " " + Strings.Campaign.ellipses);
        panel.addPara(Strings.Campaign.ellipses);
        panel.addPara(Strings.Campaign.ellipses);

        var mem = memoryMap.get(MemKeys.GLOBAL);
        //noinspection unchecked
        Set<Integer> salvagedIds = (Set<Integer>) mem.get(Strings.Campaign.SALVAGED_BEACON_IDS);
        if (salvagedIds == null) salvagedIds = new HashSet<>();
        //noinspection unchecked
        List<String> locations = (List<String>) mem.get(Strings.Campaign.BEACON_LOCATION_NAMES);
        if (locations == null) locations = new ArrayList<>();

        int i = 0;
        for (; i < Generator.NUM_STATIONS_ITEM + Generator.NUM_STATIONS_HULLMOD; i++) {
            String loc = locations.size() <= i ? Strings.Campaign.unknown : locations.get(i);
            String status = salvagedIds.contains(i) ? Strings.Campaign.noConnection : Strings.Campaign.ok;
            Color highlight = salvagedIds.contains(i) ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor();
            String br = i == 0 ? Utils.makeLineBreak(dialog.getTextWidth()-20f, Fonts.ORBITRON_12) + "\n\n": "";
            panel.addPara(String.format(br + Strings.Campaign.statusText + "\n" + Utils.makeLineBreak(dialog.getTextWidth()-20f, Fonts.ORBITRON_12), i+1, loc, status), Misc.getTextColor(), highlight, status);
        }

        // Remote beacon
        boolean lootedRemote = mem.getBoolean(Strings.Campaign.LOOTED_REMOTE_BEACON);
        String loc = Strings.Campaign.questionMarks;
        String status = lootedRemote ? Strings.Campaign.noConnection : Strings.Campaign.ok;
        Color highlight = lootedRemote ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor();
        panel.addPara(String.format(Strings.Campaign.statusText + "\n" + Utils.makeLineBreak(dialog.getTextWidth()-20f, Fonts.ORBITRON_12), i+1, loc, status), Misc.getTextColor(), highlight, status);

        panel.setFontInsignia();
        return true;
    }
}
