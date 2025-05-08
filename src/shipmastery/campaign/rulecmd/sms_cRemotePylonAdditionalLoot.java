package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoPickerListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cRemotePylonAdditionalLoot extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        CargoAPI cargo = Global.getFactory().createCargo(true);
        cargo.addCommodity(Commodities.SUPPLIES, MathUtils.randBetween(50f, 100f));
        cargo.addCommodity(Commodities.FUEL, MathUtils.randBetween(250f, 400f));
        cargo.addCommodity(Commodities.METALS, MathUtils.randBetween(100f, 200f));
        cargo.addCommodity("sms_fractured_gamma_core", MathUtils.randBetween(5f, 10f));
        dialog.showCargoPickerDialog("Select Loot", Strings.Misc.confirm, Strings.Misc.cancel, false, 0f, cargo, new CargoPickerListener() {
            @Override
            public void pickedCargo(CargoAPI cargo) {
                if (cargo.isEmpty()) return;
                Global.getSector().getPlayerFleet().getCargo().addAll(cargo);
                FireBest.fire(null, dialog, memoryMap, "sms_tRemotePylonNoShieldLootPicked");
            }

            @Override
            public void cancelledCargoSelection() {
            }

            @Override
            public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
            }
        });
        return true;
    }
}
