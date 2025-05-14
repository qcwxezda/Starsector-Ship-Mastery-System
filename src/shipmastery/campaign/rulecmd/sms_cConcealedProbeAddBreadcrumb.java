package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.misc.BreadcrumbIntelV2;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class sms_cConcealedProbeAddBreadcrumb extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null) return false;
        var entity = dialog.getInteractionTarget();
        var mem = getEntityMemory(memoryMap);
        var parent = (SectorEntityToken) mem.get(Strings.Campaign.PROBE_PARENT_STATION);
        if (parent == null || !parent.isAlive()) return true;
        int stationId = parent.getMemoryWithoutUpdate().getInt(Strings.Campaign.BEACON_ID);
        var globalMem = memoryMap.get(MemKeys.GLOBAL);
        //noinspection unchecked
        var seenBreadcrumbs = (Set<Integer>) globalMem.get(Strings.Campaign.ACQUIRED_STATION_LEADS);
        if (seenBreadcrumbs == null) {
            seenBreadcrumbs = new HashSet<>();
            globalMem.set(Strings.Campaign.ACQUIRED_STATION_LEADS, seenBreadcrumbs);
        }

        if (seenBreadcrumbs.contains(stationId)) return true;
        seenBreadcrumbs.add(stationId);

        String shortName = entity.getCustomEntitySpec().getShortName();
        String longName = entity.getCustomEntitySpec().getNameInText();
        String stationName = parent.getCustomEntitySpec().getNameInText();
        String stationNameTitle = parent.getCustomEntitySpec().getDefaultName();

        String text = String.format(Strings.Campaign.probeBreadcrumb, shortName, stationName, BreadcrumbSpecial.getLocatedString(parent, true));
        String intelText = String.format(Strings.Campaign.probeBreadcrumbForIntel, longName, stationName, BreadcrumbSpecial.getLocatedString(parent, false));

        BreadcrumbIntelV2 intel = new BreadcrumbIntelV2(parent);
        intel.setTitle(String.format(Strings.Campaign.probeBreadcrumbIntelTitle, stationNameTitle));
        intel.setText(intelText);
        //intel.setIcon(Global.getSettings().getSpriteName("intel", "leading_to_larger_domain_derelict"));
        intel.setIconId("link_to_orbital_installation");

        dialog.getTextPanel().addPara(text);
        Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());

        return true;
    }
}
