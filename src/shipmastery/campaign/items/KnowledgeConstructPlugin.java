package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.ShipMastery;
import shipmastery.campaign.MasterySharingHandler;
import shipmastery.campaign.rulecmd.sms_cBlankConstruct;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class KnowledgeConstructPlugin extends BaseSpecialItemPlugin {
    public static final int NUM_POINTS_GAINED = 100;
    public static final String PREF_IN_FLEET_TAG = "~pref_in_fleet";
    public static final String PLAYER_CREATED_PREFIX = "sms_PlayerCreated_";
    private ShipHullSpecAPI hullSpec;
    private boolean wasCreatedByPlayer = false;
    private boolean isBlank = false;
    private boolean noSpecString = false;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        String specStr = stack.getSpecialDataIfSpecial().getData();
        if (specStr == null) {
            noSpecString = true;
            return;
        }
        if (specStr.startsWith(PLAYER_CREATED_PREFIX)) {
            wasCreatedByPlayer = true;
            isBlank = true;
            specStr = specStr.substring(PLAYER_CREATED_PREFIX.length());
        }
        if (specStr.isEmpty()) {
            hullSpec = null;
        } else {
            hullSpec = Global.getSettings().getHullSpec(specStr);
        }
    }

    @Override
    public boolean hasRightClickAction() {
        return isBlank || (hullSpec != null && !wasCreatedByPlayer);
    }

    public void render(float x, float y, float w, float h, float alphaMult, float glowMult,
                       SpecialItemRendererAPI renderer, Color bgColor) {
        float hw = w/2f, hh = h/2f;
        float cx = x + hw;
        float cy = y + hh;

        if (noSpecString) return;
        String hullId = hullSpec == null ? null : hullSpec.getHullId();
        bgColor = Misc.setAlpha(bgColor, 255);

        float tlX = cx;
        float tlY = cy;
        float blX = cx;
        float blY = cy - hh*0.75f;
        float trX = cx + hw*0.649519f;
        float trY = cy + hh*0.375f;
        float brX = cx + hw*0.649519f;
        float brY = cy - hh*0.375f;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, glowMult,false);

        trX = cx;
        trY = cy;
        brX = cx;
        brY = cy - hh*0.75f;
        tlX = cx - hw*0.649519f;
        tlY = cy + hh*0.375f;
        blX = cx - hw*0.649519f;
        blY = cy - hh*0.375f;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, glowMult, false);

        trY = cy + hh*0.75f;
        brX = cx + hw*0.649519f;
        brY = cy + hh*0.375f;
        tlX = cx - hw*0.649519f;
        tlY = cy + hh*0.375f;
        blX = cx;
        blY = cy;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, glowMult, true);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult,
                       SpecialItemRendererAPI renderer) {
        render(x, y, w, h, alphaMult, glowMult, renderer,Global.getSector().getPlayerFaction().getDarkUIColor());
    }

    private void renderShipExt(
            SpecialItemRendererAPI renderer,
            String hullId,
            Color bgColor,
            float blX,
            float blY,
            float tlX,
            float tlY,
            float trX,
            float trY,
            float brX,
            float brY,
            float alphaMult,
            float glowMult,
            boolean squishY) {
        renderer.renderBGWithCorners(bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY,
                                     alphaMult*0.5f,  glowMult*0.5f, true);
        renderer.renderScanlinesWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, true);
        if (hullId != null && !isBlank) {
            renderer.renderShipWithCorners(hullId, null, blX, blY + (squishY ? 10f : 0f), tlX, tlY, trX, trY - (squishY ? 10f : 0f), brX, brY,
                    alphaMult * 0.5f, 0f, true);
        }
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        if (isBlank) return 20000;
        if (hullSpec == null) return 10000;
        return getPrice(hullSpec);
    }

    public static int getPrice(ShipHullSpecAPI spec) {
        return (int) Math.min(25000f, 10000f + spec.getBaseValue() * 0.02f);
    }

    @Override
    public String getName() {
        if (hullSpec == null || isBlank) return super.getName();
        return hullSpec.getHullNameWithDashClass() + " " + Strings.Items.knowledgeConstruct;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler,
                              Object stackSource) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource);
        float opad = 10.0F;
        Color b = Misc.getPositiveHighlightColor();
        if (hullSpec != null) {
            Description desc = Global.getSettings().getDescription(hullSpec.getDescriptionId(), Description.Type.SHIP);
            String prefix = "";
            if (hullSpec.getDescriptionPrefix() != null) {
                prefix = hullSpec.getDescriptionPrefix() + "\n\n";
            }
            tooltip.addPara(prefix + desc.getText1FirstPara(), opad);
        }
        this.addCostLabel(tooltip, opad, transferHandler, stackSource);
        if (getId() != null && getId().startsWith("sms_superconstruct")) return;

        if (!hasRightClickAction() && !noSpecString) {
            tooltip.addPara(Strings.Items.knowledgeConstructCantRightClick, Misc.getGrayColor(), opad);
        }
        else if (!noSpecString && !isBlank) {
            tooltip.addPara(String.format(Strings.Items.knowledgeConstructRightClick, NUM_POINTS_GAINED), b, opad);
        } else if (!noSpecString) {
            tooltip.addPara(String.format(Strings.Items.blankConstructRightClick, Utils.asInt(MasterySharingHandler.SHARED_MASTERY_MP_GAIN)), b, opad);
        }
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        if (hullSpec != null && !wasCreatedByPlayer) {
            ShipMastery.addPlayerMasteryPoints(hullSpec, NUM_POINTS_GAINED, false, false, ShipMastery.MasteryGainSource.ITEM);
            Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
            Global.getSector().getCampaignUI().getMessageDisplay()
                    .addMessage(String.format(Strings.Messages.gainedMPSingle, NUM_POINTS_GAINED + " " + Strings.Misc.XP, hullSpec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);

        } else if (isBlank) {
            RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tBlankConstructClicked");
            plugin.setCustom1(helper);
            var target = Global.getSector().getPlayerFleet();
            target.getMemoryWithoutUpdate().set(sms_cBlankConstruct.TYPE_MEMORY_KEY, sms_cBlankConstruct.BlankConstructType.MASTERY_SHARING_CONSTRUCT, 0f);
            Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin, target, () -> {});
        }
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return !isBlank;
    }

    @Override
    public String resolveDropParamsToSpecificItemData(String params, Random random) throws JSONException {
        if (params == null || params.isEmpty()) return null;

        if (!params.startsWith("{")) {
            return params;
        }

        JSONObject json = new JSONObject(params);

        Set<String> tags = new HashSet<>();
        if (json.has("tags")) {
            JSONArray tagsArray = json.getJSONArray("tags");
            for (int i = 0; i < tagsArray.length(); i++) {
                tags.add(tagsArray.getString(i));
            }
        }

        return pickShip(tags, random);
    }


    public static String pickShip(Set<String> tags, Random random) {
        Set<ShipHullSpecAPI> specs = new HashSet<>(Global.getSettings().getAllShipHullSpecs());
        Set<ShipHullSpecAPI> playerFleetSpecs = new HashSet<>();
        for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            playerFleetSpecs.add(Utils.getRestoredHullSpec(fm.getHullSpec()));
        }

        Iterator<ShipHullSpecAPI> iter = specs.iterator();
        while(iter.hasNext()) {
            ShipHullSpecAPI spec = iter.next();
            if (spec != Utils.getRestoredHullSpec(spec)) {
                iter.remove();
                continue;
            }
            if (playerFleetSpecs.contains(spec)) continue;

            if (!CodexDataV2.hasUnlockedEntry("codex_hull_" + spec.getHullId())) {
                iter.remove();
            }
            else if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) {
                iter.remove();
            }
            else if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE)
                    && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE)) {
                iter.remove();
            }
            else if (spec.getTags().contains(Tags.RESTRICTED)) {
                iter.remove();
            }
            else if (ShipAPI.HullSize.FIGHTER.equals(spec.getHullSize())) {
                iter.remove();
            }
        }

        if (!tags.isEmpty()) {
            iter = specs.iterator();
            while (iter.hasNext()) {
                ShipHullSpecAPI curr = iter.next();
                for (String tag : tags) {
                    if (tag.startsWith("~")) continue;
                    boolean not = tag.startsWith("!");
                    tag = not ? tag.substring(1) : tag;
                    boolean has = curr.hasTag(tag);
                    if (not == has) {
                        iter.remove();
                        break;
                    }
                }
            }
        }

        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>(random);
        for (ShipHullSpecAPI spec : specs) {
            float weight = spec.getRarity();
            if (playerFleetSpecs.contains(spec) && tags.contains(PREF_IN_FLEET_TAG)) {
                weight += 200f * Utils.getSelectionWeightScaledByValueDecreasing(
                        ShipMastery.getPlayerMasteryPoints(spec), 100f, 2000f, 10000f, 5f);
            }
            picker.add(spec, weight);
        }

        ShipHullSpecAPI pick = picker.pick();
        if (pick == null) {
            return null;
        } else {
            return pick.getHullId();
        }
    }
}
