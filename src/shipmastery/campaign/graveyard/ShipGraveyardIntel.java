package shipmastery.campaign.graveyard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.*;

public class ShipGraveyardIntel extends BaseIntelPlugin {
    public static final int MAX_DAYS = 365;
    protected final Set<Pair<FleetMemberAPI, SectorEntityToken>> lostInfo = new TreeSet<>(
            new Comparator<Pair<FleetMemberAPI, SectorEntityToken>>() {
                @Override
                public int compare(Pair<FleetMemberAPI, SectorEntityToken> p1,
                                   Pair<FleetMemberAPI, SectorEntityToken> p2) {
                    return Utils.byDPComparator.compare(p1.one, p2.one);
                }
            });
    protected final SectorEntityToken entityToShow;
    protected final List<FleetMemberAPI> fleetMembers = new ArrayList<>();

    public ShipGraveyardIntel(@NotNull List<Pair<FleetMemberAPI, SectorEntityToken>> lostInfo) {
        this.lostInfo.addAll(lostInfo);
        entityToShow = lostInfo.iterator().next().two;
        Global.getSector().addScript(this);
    }

    @Override
    protected void createDeleteConfirmationPrompt(TooltipMakerAPI prompt) {
        super.createDeleteConfirmationPrompt(prompt);
        prompt.addPara(Strings.Graveyard.intelDespawnWarning,
                       Misc.getNegativeHighlightColor(),
                       10f);
    }

    @Override
    public void advanceImpl(float amount) {
        float days = getDaysSincePlayerVisible();
        float diff = MAX_DAYS - days;

        if (diff <= 0f) {
            endImmediately();
        }
    }

    @Override
    public boolean shouldRemoveIntel() {
        return isEnded() || lostInfo.isEmpty();
    }

    @Override
    protected void notifyEnded() {
        for (Pair<FleetMemberAPI, SectorEntityToken> pair : lostInfo) {
            Misc.fadeAndExpire(pair.two, 1f);
        }
        Global.getSector().removeScript(this);
    }

    @Override
    public void notifyPlayerAboutToOpenIntelScreen() {
        // Remove entries from lostInfo if player salvaged or recovered the wreck
        for (Iterator<Pair<FleetMemberAPI, SectorEntityToken>> iterator = lostInfo.iterator(); iterator.hasNext(); ) {
            Pair<FleetMemberAPI, SectorEntityToken> pair = iterator.next();
            if (!pair.two.isAlive()) {
                iterator.remove();
            }
        }
        fleetMembers.clear();
        for (Pair<FleetMemberAPI, SectorEntityToken> pair : lostInfo) {
            fleetMembers.add(pair.one);
        }
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        return new HashSet<>(Collections.singleton(Tags.INTEL_FLEET_LOG));
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "damage_report");
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.addPara(getName(), c, 0f);
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_1;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara(Strings.Graveyard.intelPara1, 10f);
        info.addShipList(8, (fleetMembers.size() + 7) / 8, 40f, Misc.getBasePlayerColor(), fleetMembers, 10f);

        float days = getDaysSincePlayerVisible();
        int diff = (int) (MAX_DAYS - days);
        info.addPara(diff == 1 ? Strings.Graveyard.intelPara2Single : Strings.Graveyard.intelPara2, 10f, Misc.getNegativeHighlightColor(), "" + diff);
        if (days >= 1) {
            addDays(info, Strings.Graveyard.intelAfterDays, days, Misc.getTextColor(), 10f);
        }

        addDeleteButton(info, width);
    }

    @Override
    public String getName() {
        return Strings.Graveyard.intelTitle;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return entityToShow;
    }
}
