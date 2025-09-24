package ch.ksrminecraft.murdermystery.Commands;

import ch.ksrminecraft.murdermystery.Utils.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JoinCommand extends @NotNull Command implements CommandExecutor {
    private final GameManager gameManager;


    public JoinCommand(GameManager gameManager) {
        super("join");
        this.gameManager = gameManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        Player player = (Player) commandSender;
        gameManager.handleJoin(player);
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String @NotNull [] strings) {
        Player player = (Player) commandSender;
        gameManager.handleJoin(player);
        return true;
    }
}
