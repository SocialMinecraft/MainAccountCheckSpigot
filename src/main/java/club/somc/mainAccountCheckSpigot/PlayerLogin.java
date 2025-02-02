package club.somc.mainAccountCheckSpigot;

import club.somc.protos.minecraftaccounts.GetMinecraftAccountRequest;
import club.somc.protos.minecraftaccounts.GetMinecraftAccountResponse;
import club.somc.protos.minecraftaccounts.MinecraftAccount;
import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Connection;
import io.nats.client.Message;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.Duration;
import java.util.logging.Logger;

public class PlayerLogin implements Listener {

    Connection nc;
    Logger logger;
    LuckPerms lp;

    public PlayerLogin(Connection nc, Logger logger) {
        this.nc = nc;
        this.logger = logger;

        setupLuckPerms();
    }

    private void setupLuckPerms() {
        try {
            lp = LuckPermsProvider.get();
        } catch (NoClassDefFoundError ex) {
            logger.warning("LuckPerms not found");
            // should really crash.
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        //User user = lp.getPlayerAdapter(Player.class).getUser(e.getPlayer());

        MinecraftAccount account = getMinecraftAccount(e.getPlayer());
        if (account == null) {
            logger.warning("Account not found: " + e.getPlayer().getName());
            return;
        }

        if (account.getIsMain()) {
            logger.warning("Account is main: " + e.getPlayer().getName());
            lp.getUserManager().modifyUser(e.getPlayer().getUniqueId(), user -> {
                InheritanceNode node = InheritanceNode.builder("main_accounts").build();
                user.data().add(node);
            });
        } else {
            logger.warning("Account is not main: " + e.getPlayer().getName());
            lp.getUserManager().modifyUser(e.getPlayer().getUniqueId(), user -> {
                InheritanceNode node = InheritanceNode.builder("main_accounts").build();
                user.data().remove(node);
            });
        }
    }

    private MinecraftAccount getMinecraftAccount(Player player) {
        GetMinecraftAccountRequest req = GetMinecraftAccountRequest.newBuilder()
                .setMinecraftUuid(player.getUniqueId().toString())
                .build();

        Message m;
        try {
            m = nc.request("accounts.minecraft.get", req.toByteArray(), Duration.ofMillis(300));
        } catch (InterruptedException e) {
            logger.warning(e.getMessage());
            return null;
        }

        GetMinecraftAccountResponse res;
        try {
            res = GetMinecraftAccountResponse.parseFrom(m.getData());
        } catch (InvalidProtocolBufferException e) {
            logger.warning(e.getMessage());
            return null;
        }

        if (!res.getAccountFound()) {
            return null;
        }

        return res.getAccount();
    }


}
