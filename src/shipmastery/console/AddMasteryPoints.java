package shipmastery.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import shipmastery.ShipMastery;

public class AddMasteryPoints implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        String[] argList = args.split("\\s+");

        if (argList.length != 2) {
            return badSyntax();
        }

        String hullId = argList[0];
        int amount;

        try {
            amount = Integer.parseInt(argList[1]);
        } catch (NumberFormatException e) {
            return badSyntax();
        }

        ShipHullSpecAPI spec = Global.getSettings().getHullSpec(hullId);

        if (spec == null) {
            Console.showMessage("No ship found with hull id: " + hullId);
            return CommandResult.ERROR;
        }

        ShipMastery.addPlayerMasteryPoints(spec, amount, false, false, ShipMastery.MasteryGainSource.OTHER);
        return CommandResult.SUCCESS;
    }

    public CommandResult badSyntax() {
        Console.showMessage("sms_AddMasteryPoints hullId amount");
        return CommandResult.BAD_SYNTAX;
    }
}
