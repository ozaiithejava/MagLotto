package org.ozaii.maglotto.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ozaii.maglotto.MagLotto;
import org.ozaii.maglotto.lotto.LottoManager;
import org.ozaii.maglotto.managers.ConfigManager;

public class JoinListener implements Listener {

    private static JoinListener instance;

    private final LottoManager lottoManager = LottoManager.getInstance();
    private final ConfigManager configManager = ConfigManager.getInstance();

    private JoinListener() {
        Bukkit.getPluginManager().registerEvents(this, MagLotto.getInstance());
    }

    public static void initialize() {
        if (instance == null) {
            instance = new JoinListener();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Aktif lotto varsa ve join mesajı açıksa
        if (lottoManager.getActiveLottos().size() >= 1 && configManager.isLottoJoinMessage()) {
            lottoManager.sendNewLottoMessage(player, lottoManager.getLastLottoId());
        }
    }
}