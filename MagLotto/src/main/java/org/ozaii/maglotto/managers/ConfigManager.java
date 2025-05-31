package org.ozaii.maglotto.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.ozaii.maglotto.MagLotto;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static ConfigManager instance;
    private FileConfiguration config;
    private File configFile;

    private final Map<String, Object> configDefaults = new HashMap<String, Object>() {{
        put("starting-id", 1);
        put("auto-create-interval-minutes", 10);
        put("start-delay-seconds", 30);
        put("quick-start-delay-seconds", 5); // YENİ AYAR
        put("reward-amount", 100.0);
        put("min-players", 2);
        put("max-players", 50);
        put("entry-cost", 25.0);
        put("lotto-message-title", "%lotto% başladı!");
        put("lotto-message-subtitle", "hemen katıl");
        put("lotto-join-message", true);
        put("auto-start-when-full", true);
        put("cancel-if-not-enough-players", true);
    }};

    private ConfigManager() {
        setupConfig();
        loadDefaults();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void setupConfig() {
        File pluginFolder = MagLotto.getInstance().getDataFolder();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        configFile = new File(pluginFolder, "lotto.yml");

        if (!configFile.exists()) {
            MagLotto.getInstance().saveResource("lotto.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadDefaults() {
        boolean needsSave = false;

        for (Map.Entry<String, Object> entry : configDefaults.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }

        if (needsSave) {
            saveConfig();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDefaults(); // Reload sonrası eksik değerleri tekrar kontrol et
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            MagLotto.getInstance().getLogger().severe("Config kaydedilemedi: " + e.getMessage());
        }
    }

    // Otomatik getter metodu
    private Object getConfigValue(String key, Object defaultValue) {
        return config.get(key, defaultValue);
    }


    public int getStartingId() {
        return (Integer) getConfigValue("starting-id", configDefaults.get("starting-id"));
    }

    public int getAutoCreateInterval() {
        return (Integer) getConfigValue("auto-create-interval-minutes", configDefaults.get("auto-create-interval-minutes"));
    }

    public int getStartDelay() {
        return (Integer) getConfigValue("start-delay-seconds", configDefaults.get("start-delay-seconds"));
    }

    public double getRewardAmount() {
        return (Double) getConfigValue("reward-amount", configDefaults.get("reward-amount"));
    }

    public int getMinPlayers() {
        return (Integer) getConfigValue("min-players", configDefaults.get("min-players"));
    }

    public int getMaxPlayers() {
        return (Integer) getConfigValue("max-players", configDefaults.get("max-players"));
    }

    public double getEntryCost() {
        return (Double) getConfigValue("entry-cost", configDefaults.get("entry-cost"));
    }

    public String getLottoMessageTitle() {
        return (String) getConfigValue("lotto-message-title", configDefaults.get("lotto-message-title"));
    }

    public String getLottoMessageSubtitle() {
        return (String) getConfigValue("lotto-message-subtitle", configDefaults.get("lotto-message-subtitle"));
    }

    public boolean isLottoJoinMessage() {
        return (Boolean) getConfigValue("lotto-join-message", configDefaults.get("lotto-join-message"));
    }

    public boolean isAutoStartWhenFull() {
        return (Boolean) getConfigValue("auto-start-when-full", configDefaults.get("auto-start-when-full"));
    }

    public boolean isCancelIfNotEnoughPlayers() {
        return (Boolean) getConfigValue("cancel-if-not-enough-players", configDefaults.get("cancel-if-not-enough-players"));
    }

    public int getQuickStartDelay() {
        return (Integer) getConfigValue("quick-start-delay-seconds", configDefaults.get("quick-start-delay-seconds"));
    }

    public FileConfiguration getConfig() {
        return config;
    }


    public void addConfigDefault(String key, Object defaultValue) {
        configDefaults.put(key, defaultValue);
        if (!config.contains(key)) {
            config.set(key, defaultValue);
            saveConfig();
        }
    }

    public void resetToDefaults() {
        for (Map.Entry<String, Object> entry : configDefaults.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        saveConfig();
    }
}