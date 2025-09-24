package ch.ksrminecraft.murdermystery;

import ch.ksrminecraft.murdermystery.Commands.JoinCommand;
import ch.ksrminecraft.murdermystery.Listener.GameListener;
import ch.ksrminecraft.murdermystery.Utils.GameManager;
import ch.ksrminecraft.murdermystery.Utils.PointsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field;

// TODO:
// Spectators können Spiel noch nicht vorzeitig verlassen
// Gold farm für bystander zum bogen erspielen
// eigene Maps bauen

public class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private GameManager gameManager;
    private PointsManager pointsManager;
    private CommandMap commandMap;

    @Override
    public void onEnable() {

        instance = this;
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // Config-Werte einlesen und prüfen mit Fallback Wert
        int minPlayers = getConfig().getInt("min-players", 3);
        int countdownTime = getConfig().getInt("countdown-seconds", 10);
        if (minPlayers < 3) {
            getLogger().severe("Config-Fehler: MIN_PLAYERS muss mindestens 3 sein. Default 3 wird angewendet.");
            minPlayers = 3;
        }

        // PointsManager mit Config-Daten initialisieren
        pointsManager = new PointsManager(getLogger(), this);

        // GameManager mit PointsManager initialisieren
        gameManager = new GameManager(pointsManager, this);

        // Config-Werte im GameManager einsetzen
        gameManager.setMinPlayers(minPlayers);
        gameManager.setCountdownTime(countdownTime);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        // Command-Registrierung mit yml geht mit Java 1.21+ nicht mehr ---> ChatGPT Lösung
        // Alte Methode: getCommand("join").setExecutor(new JoinCommand(gameManager));
        // CommandMap per Reflection holen laut ChatGPT
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            getLogger().severe("Failed to get CommandMap!");
            e.printStackTrace();
        }

        if (commandMap != null) {
            commandMap.register("murdermystery3", new JoinCommand(gameManager));
        }



        // RankPointsAPI Check
        if (getServer().getPluginManager().getPlugin("RankPointsAPI") == null) {
            getLogger().severe("RankPointsAPI Plugin nicht gefunden! Punkteverteilung nicht möglich.");
        }
    }


    // Plugin Instanz Getter
    public static MurderMystery getInstance() {
        return instance;
    }

    // Getter für GameManager
    public GameManager getGameManager() {
        return gameManager;
    }

    // Getter für PointsManager
    public PointsManager getPointsManager() {
        return pointsManager;
    }

    // Boolean (wird für mehrere Klassen gebraucht, zentral speichern ist einfacher)
    private boolean murdererKilledByBow = false;
    public boolean isMurdererKilledByBow() {
        return murdererKilledByBow;
    }

    public void setMurdererKilledByBow(boolean murdererKilledByBow) {
        this.murdererKilledByBow = murdererKilledByBow;
    }

}

