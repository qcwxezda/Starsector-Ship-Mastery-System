package shipmastery.ui.buttons;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.HullmodUtils;

public abstract class ButtonWithHullmodSelection extends ButtonWithItemSelection<HullModSpecAPI> {

    public record HullmodItem(HullModSpecAPI spec) implements Item<HullModSpecAPI> {
        @Override
        public String getId() {
            return spec.getId();
        }

        @Override
        public String getDisplayName() {
            return spec.getDisplayName();
        }

        @Override
        public HullModSpecAPI getItem() {
            return spec;
        }
    }

    protected final ShipAPI selectedShip;

    public ButtonWithHullmodSelection(String spriteName, boolean useStoryColors, ShipAPI selectedShip) {
        super(spriteName, useStoryColors);
        this.selectedShip = selectedShip;
    }

    @Override
    protected TooltipMakerAPI.TooltipCreator getPerItemTooltipCreator(Item<HullModSpecAPI> item) {
        return new HullmodUtils.HullmodTooltipCreator(item.getItem(), selectedShip);
    }
}
