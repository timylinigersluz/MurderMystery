package ch.ksrminecraft.murdermystery.commands.subcommands;

import org.bukkit.command.CommandSender;
import java.util.Collections;
import java.util.List;

/**
 * Basis-Interface für alle MurderMystery SubCommands.
 * Jeder SubCommand definiert Name, Beschreibung, Usage und die Logik in {@link #execute}.
 */
public interface SubCommand {

    /**
     * @return Der Name des Befehls, z. B. "join".
     */
    String getName();

    /**
     * @return Eine kurze Beschreibung, die im /mm help angezeigt wird.
     */
    String getDescription();

    /**
     * @return Beispiel für die Verwendung, z. B. "/mm join".
     */
    String getUsage();

    /**
     * Führt den SubCommand aus.
     *
     * @param sender Der Befehlssender (Spieler oder Konsole).
     * @param args   Die Argumente nach dem SubCommand.
     */
    void execute(CommandSender sender, String[] args);

    /**
     * @return Optional: Aliases für diesen Befehl, z. B. ["j", "beitreten"].
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * @return Optional: Permission, die für den Befehl benötigt wird.
     *         Wenn null zurückgegeben wird, ist der Befehl für alle verfügbar.
     */
    default String getPermission() {
        return null;
    }
}
