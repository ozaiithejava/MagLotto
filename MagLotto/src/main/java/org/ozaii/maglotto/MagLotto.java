package org.ozaii.maglotto;

import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.maglotto.commands.LottoCommand;
import org.ozaii.maglotto.commands.LottoTabCompleter;
import org.ozaii.maglotto.listeners.JoinListener;
import org.ozaii.maglotto.lotto.LottoManager;
import org.ozaii.maglotto.managers.ConfigManager;
import org.ozaii.maglotto.utils.VaultUtil;

public final class MagLotto extends JavaPlugin {

    private static volatile MagLotto instance;

    @Override
    public void onEnable() {
        instance = this;

        // Vault kurulum
        if (!setupVault()) {
            getLogger().severe("Vault bulunamadı! Plugin devre dışı bırakılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Diğer kurulumlar
        VaultUtil.setupEconomy();
        ConfigManager.getInstance(); // Config'i yükle
        LottoManager.getInstance().initialize();
        JoinListener.initialize();

        // Komutları kaydet
        getCommand("lotto").setExecutor(new LottoCommand());
        getCommand("lotto").setTabCompleter(new LottoTabCompleter());

        getLogger().info("MagLotto başarıyla yüklendi!");
    }

    @Override
    public void onDisable() {
        if (LottoManager.getInstance() != null) {
            LottoManager.getInstance().shutdown();
        }
        instance = null;
        getLogger().info("MagLotto kapatıldı!");
    }

    private boolean setupVault() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    public static MagLotto getInstance() {
        if (instance == null) {
            synchronized (MagLotto.class) {
                if (instance == null) {
                    throw new IllegalStateException("MagLotto instance is not initialized yet!");
                }
            }
        }
        return instance;
    }
}