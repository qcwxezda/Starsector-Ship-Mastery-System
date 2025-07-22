package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CampaignUtils {

    public static PersonAPI getFleetCommanderForStats(MutableShipStatsAPI stats) {
        var lookup = VariantLookup.getVariantInfo(stats.getVariant());
        if (lookup == null) return null;
        return lookup.commander;
    }

    public static PersonAPI getCaptain(MutableShipStatsAPI stats) {
        PersonAPI captain;
        if (stats.getEntity() instanceof ShipAPI ship) {
            captain = ship.getCaptain();
        } else {
            captain = stats.getFleetMember() == null ? null : stats.getFleetMember().getCaptain();
        }
        return captain;
    }

    public static NavigableMap<CommoditySpecAPI, Integer> getPlayerCommodityCounts(Function<CommoditySpecAPI, Boolean> filter) {
        return Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                .stream()
                .<Map.Entry<CommoditySpecAPI, Integer>>mapMulti((x, c) -> {
                    var id = x.getCommodityId();
                    if (id == null) return;
                    var spec = Global.getSettings().getCommoditySpec(id);
                    if (spec == null || !filter.apply(spec)) return;
                    c.accept(Map.entry(spec, (int) x.getSize()));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        () -> new TreeMap<>(
                                Comparator.comparingDouble(CommoditySpecAPI::getOrder)
                                        .thenComparing((x, y) -> CharSequence.compare(x.getId(), y.getId()))),
                        Collectors.summingInt(Map.Entry::getValue)));
    }

    public static ShipVariantAPI cloneAndSetVariantIfNeeded(FleetMemberAPI fm, ShipVariantAPI variant) {
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            variant = variant.clone();
            variant.setGoalVariant(false);
            variant.setSource(VariantSource.REFIT);
            if (fm != null) {
                fm.setVariant(variant, false, false);
            }
        }
        return variant;
    }

    private static ShipVariantAPI addPermaModHelper(ShipVariantAPI variant, FleetMemberAPI fm, String hullmodId, boolean addToModules) {
        if (variant == null) return null;
        variant = cloneAndSetVariantIfNeeded(fm, variant);
        variant.addPermaMod(hullmodId, false);
        if (addToModules) {
            for (String id : variant.getModuleSlots()) {
                variant.setModuleVariant(id, addPermaModHelper(variant.getModuleVariant(id), null, hullmodId, true));
            }
        }
        return variant;
    }

    public static void addPermaModCloneVariantIfNeeded(FleetMemberAPI fm, String hullmodId, boolean addToModules) {
        addPermaModHelper(fm.getVariant(), fm, hullmodId, addToModules);
    }

    /** Will not work on non-refit-source variants */
    public static void removePermaModFromCustomVariant(FleetMemberAPI fm, String hullmodId, boolean removeFromModules) {
        Queue<ShipVariantAPI> variants = new LinkedList<>();
        variants.add(fm.getVariant());

        while (!variants.isEmpty()) {
            var variant = variants.poll();
            variant.removePermaMod(hullmodId);
            if (removeFromModules) {
                for (String id : variant.getModuleSlots()) {
                    variants.add(variant.getModuleVariant(id));
                }
            }
        }
    }

    public static LabelAPI addStoryPointUseInfo(TooltipMakerAPI tooltip, float bonusXPFrac) {
        int sp = Global.getSector().getPlayerStats().getStoryPoints();
        String pointOrPoints = sp == 1 ? Strings.Misc.storyPoint : Strings.Misc.storyPoints;
        Color hc = sp == 0 ? Misc.getNegativeHighlightColor() : Misc.getStoryOptionColor();
        if (bonusXPFrac <= 0f) {
            return tooltip.addPara(Strings.Misc.requiresStoryPointNoBonus, 10f, new Color[] {hc, hc, hc}, Strings.Misc.storyPoint, "" + sp, pointOrPoints);
        }
        return tooltip.addPara(Strings.Misc.requiresStoryPointWithBonus, 10f, new Color[] {hc, Misc.getStoryOptionColor(), hc, hc}, Strings.Misc.storyPoint, Utils.asPercent(bonusXPFrac), "" + sp, pointOrPoints);
    }

    public static abstract class TextFieldDelegate implements CustomDialogDelegate {

        private TextFieldAPI textField = null;
        private final String prompt;
        private final String defaultText;
        private boolean clearedFirstFocus = false;

        protected TextFieldDelegate(String prompt, @NotNull String defaultText) {
            this.prompt = prompt;
            this.defaultText = defaultText;
        }

        protected TextFieldDelegate(String prompt) {
            this.prompt = prompt;
            defaultText = "";
        }

        public abstract void onConfirm(String text);

        @Override
        public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
            float w = panel.getPosition().getWidth();
            float h = panel.getPosition().getHeight();
            TooltipMakerAPI ttm = panel.createUIElement(w, h, false);
            ttm.setParaFont(Fonts.ORBITRON_20AA);
            ttm.addPara(prompt, 20f).setAlignment(Alignment.MID);
            textField = ttm.addTextField(w-10f, 20f);
            textField.setText(defaultText);
            textField.setMidAlignment();
            textField.setHandleCtrlV(false);
            textField.setMaxChars(20);
            if (defaultText.isEmpty()) {
                textField.grabFocus();
            } else {
                textField.setColor(Misc.getGrayColor());
            }
            panel.addUIElement(ttm).inLMid(0f);
        }

        @Override
        public final boolean hasCancelButton() {
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
        public final void customDialogConfirm() {
            if (textField == null || textField.getText() == null) return;
            onConfirm(textField.getText().trim());
        }

        @Override
        public void customDialogCancel() {}

        @Override
        public final CustomUIPanelPlugin getCustomPanelPlugin() {
            return new CustomUIPanelPlugin() {
                @Override
                public void positionChanged(PositionAPI position) {}

                @Override
                public void renderBelow(float alphaMult) {}

                @Override
                public void render(float alphaMult) {}

                @Override
                public void advance(float amount) {
                    if (textField != null && textField.hasFocus() && !defaultText.isEmpty() && !clearedFirstFocus) {
                        textField.setText("");
                        textField.setColor(Misc.getButtonTextColor());
                        clearedFirstFocus = true;
                    }
                }

                @Override
                public void processInput(List<InputEventAPI> events) {}

                @Override
                public void buttonPressed(Object buttonId) {}
            };
        }
    }
}
