package club.somc.mainAccountCheckSpigot;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MainAccountCheckSpigot extends JavaPlugin {

    Connection nc;

    @Override
    public void onEnable() {
        // Plugin startup logic

        super.onEnable();
        this.saveDefaultConfig();

        try {
            this.nc = Nats.connect(getConfig().getString("natsUrl"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(new PlayerLogin(nc, getLogger()), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
