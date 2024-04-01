package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class KnowledgeConstructPlugin extends BaseSpecialItemPlugin {
    public static final int NUM_POINTS_GAINED = 5;
    private ShipHullSpecAPI spec;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        spec = Global.getSettings().getHullSpec(stack.getSpecialDataIfSpecial().getData());
    }

    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return true;
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult,
                       SpecialItemRendererAPI renderer) {
        if (spec == null) return;
        float hw = w/2f, hh = h/2f;
        float cx = x + hw;
        float cy = y + hh;

        String hullId = stack.getSpecialDataIfSpecial().getData();
        Color bgColor = Global.getSector().getPlayerFaction().getDarkUIColor();
        bgColor = Misc.setAlpha(bgColor, 255);

        float tlX = cx;
        float tlY = cy;
        float blX = cx;
        float blY = cy - hh*0.75f;
        float trX = cx + hw*0.649519f;
        float trY = cy + hh*0.375f;
        float brX = cx + hw*0.649519f;
        float brY = cy - hh*0.375f;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, false);

        trX = cx;
        trY = cy;
        brX = cx;
        brY = cy - hh*0.75f;
        tlX = cx - hw*0.649519f;
        tlY = cy + hh*0.375f;
        blX = cx - hw*0.649519f;
        blY = cy - hh*0.375f;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, false);

        trY = cy + hh*0.75f;
        brX = cx + hw*0.649519f;
        brY = cy + hh*0.375f;
        tlX = cx - hw*0.649519f;
        tlY = cy + hh*0.375f;
        blX = cx;
        blY = cy;
        renderShipExt(renderer, hullId, bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, true);
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
            boolean squishY) {
        renderer.renderBGWithCorners(bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY,
                                     alphaMult*0.5f,  0.5f, true);
        renderer.renderScanlinesWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, true);
        renderer.renderShipWithCorners(hullId, null, blX, blY + (squishY ? 10f : 0f), tlX, tlY, trX, trY - (squishY ? 10f : 0f), brX, brY,
                                       alphaMult*0.5f, 0f, true);
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        if (spec == null) return 0;
        return (int) Math.min(100000f, spec.getBaseValue() * 0.25f);
    }

    @Override
    public String getName() {
        if (spec == null) return super.getName();
        return spec.getHullNameWithDashClass() + " " + Strings.Items.knowledgeConstruct;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler,
                              Object stackSource) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource);
        if (spec == null) return;
        float opad = 10.0F;
        Color b = Misc.getPositiveHighlightColor();
        Description desc = Global.getSettings().getDescription(spec.getDescriptionId(), Description.Type.SHIP);
        String prefix = "";
        if (spec.getDescriptionPrefix() != null) {
            prefix = spec.getDescriptionPrefix() + "\n\n";
        }
        tooltip.addPara(prefix + desc.getText1FirstPara(), opad);
        this.addCostLabel(tooltip, opad, transferHandler, stackSource);
        tooltip.addPara(String.format(Strings.Items.knowledgeConstructRightClick, NUM_POINTS_GAINED), b, opad);
    }

    @Override
    public void performRightClickAction() {
        if (spec != null) {
            ShipMastery.addPlayerMasteryPoints(spec, NUM_POINTS_GAINED);
            Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
            Global.getSector().getCampaignUI().getMessageDisplay()
                  .addMessage(String.format(Strings.Messages.gainedMPSingle, NUM_POINTS_GAINED, spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);

        }
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

        Iterator<ShipHullSpecAPI> iter = specs.iterator();
        while(iter.hasNext()) {
            ShipHullSpecAPI spec = iter.next();
            if (spec != Utils.getRestoredHullSpec(spec)) {
                iter.remove();
            }
            else if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE)
                    && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE)) {
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
            picker.add(spec, spec.getRarity());
        }
        ShipHullSpecAPI pick = picker.pick();
        if (pick == null) {
            return null;
        } else {
            return pick.getHullId();
        }
    }
}
