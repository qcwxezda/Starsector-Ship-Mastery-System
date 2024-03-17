package shipmastery.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import shipmastery.ShipMastery;
import shipmastery.util.Utils;

public class ClearAllMasteryPoints implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            spec = Utils.getRestoredHullSpec(spec);
            ShipMastery.spendPlayerMasteryPoints(spec, ShipMastery.getPlayerMasteryPoints(spec));
        }
        return CommandResult.SUCCESS;
    }
}
