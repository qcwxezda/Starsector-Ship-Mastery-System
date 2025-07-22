package shipmastery.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import shipmastery.ShipMastery;

public class AddMasteryPointsAll implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MARKET && context != CommandContext.CAMPAIGN_MAP) {
            return CommandResult.WRONG_CONTEXT;
        }

        String[] argList = args.split("\\s+");

        if (argList.length != 1) {
            return badSyntax();
        }

        int amount;
        try {
            amount = Integer.parseInt(argList[0]);
        } catch (NumberFormatException e) {
            return badSyntax();
        }

        Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()
                .stream()
                .map(FleetMemberAPI::getHullSpec)
                .distinct()
                .forEach(x -> ShipMastery.addPlayerMasteryPoints(x, amount, false, false, ShipMastery.MasteryGainSource.OTHER));

        return CommandResult.SUCCESS;
    }

    public CommandResult badSyntax() {
        Console.showMessage("sms_AddMasteryPointsAll amount");
        return CommandResult.BAD_SYNTAX;
    }
}
