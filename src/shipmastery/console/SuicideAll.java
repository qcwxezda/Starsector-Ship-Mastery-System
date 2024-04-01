package shipmastery.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;

public class SuicideAll implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        if (!context.equals(CommandContext.COMBAT_CAMPAIGN)) return CommandResult.WRONG_CONTEXT;
        if (Global.getCombatEngine() == null) return CommandResult.ERROR;

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (ship.getOwner() == 0) {
                Global.getCombatEngine().applyDamage(ship, ship.getLocation(), ship.getHitpoints()*100f + 10000f, DamageType.ENERGY, 0f, true, false, ship);
            }
        }

        return CommandResult.SUCCESS;
    }
}
