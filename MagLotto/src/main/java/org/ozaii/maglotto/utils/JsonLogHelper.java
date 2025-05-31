package org.ozaii.maglotto.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.ozaii.maglotto.MagLotto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JsonLogHelper {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private final File logsFolder;

    public JsonLogHelper() {
        this.logsFolder = new File(MagLotto.getInstance().getDataFolder(), "lottoLogs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    public void logLottoCreated(int lottoId, double rewardAmount, int minPlayers, double entryCost, int startDelay) {
        JsonObject logData = new JsonObject();
        logData.addProperty("event", "LOTTO_CREATED");
        logData.addProperty("lottoId", lottoId);
        logData.addProperty("timestamp", System.currentTimeMillis());
        logData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logData.addProperty("rewardAmount", rewardAmount);
        logData.addProperty("minPlayers", minPlayers);
        logData.addProperty("entryCost", entryCost);
        logData.addProperty("startDelaySeconds", startDelay);

        writeLog(lottoId, logData);
    }

    public void logPlayerJoined(int lottoId, UUID playerUUID, String playerName) {
        JsonObject logData = new JsonObject();
        logData.addProperty("event", "PLAYER_JOINED");
        logData.addProperty("lottoId", lottoId);
        logData.addProperty("timestamp", System.currentTimeMillis());
        logData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logData.addProperty("playerUUID", playerUUID.toString());
        logData.addProperty("playerName", playerName);

        appendLog(lottoId, logData);
    }

    public void logPlayerLeft(int lottoId, UUID playerUUID, String playerName) {
        JsonObject logData = new JsonObject();
        logData.addProperty("event", "PLAYER_LEFT");
        logData.addProperty("lottoId", lottoId);
        logData.addProperty("timestamp", System.currentTimeMillis());
        logData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logData.addProperty("playerUUID", playerUUID.toString());
        logData.addProperty("playerName", playerName);

        appendLog(lottoId, logData);
    }

    public void logLottoFinished(int lottoId, UUID winnerUUID, String winnerName, double rewardAmount,
                                 List<UUID> allParticipants, List<String> participantNames) {
        JsonObject logData = new JsonObject();
        logData.addProperty("event", "LOTTO_FINISHED");
        logData.addProperty("lottoId", lottoId);
        logData.addProperty("timestamp", System.currentTimeMillis());
        logData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logData.addProperty("winnerUUID", winnerUUID != null ? winnerUUID.toString() : null);
        logData.addProperty("winnerName", winnerName);
        logData.addProperty("rewardAmount", rewardAmount);
        logData.addProperty("totalParticipants", allParticipants.size());

        JsonObject participants = new JsonObject();
        for (int i = 0; i < allParticipants.size(); i++) {
            participants.addProperty(allParticipants.get(i).toString(), participantNames.get(i));
        }
        logData.add("participants", participants);

        appendLog(lottoId, logData);
    }

    public void logLottoCancelled(int lottoId, String reason, int participantCount) {
        JsonObject logData = new JsonObject();
        logData.addProperty("event", "LOTTO_CANCELLED");
        logData.addProperty("lottoId", lottoId);
        logData.addProperty("timestamp", System.currentTimeMillis());
        logData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logData.addProperty("reason", reason);
        logData.addProperty("participantCount", participantCount);

        appendLog(lottoId, logData);
    }

    private void writeLog(int lottoId, JsonObject logData) {
        String fileName = generateFileName(lottoId);
        File logFile = new File(logsFolder, fileName);

        try (FileWriter writer = new FileWriter(logFile)) {
            JsonObject rootObject = new JsonObject();
            rootObject.addProperty("lottoId", lottoId);
            rootObject.addProperty("createdAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            JsonArray eventsArray = new JsonArray();
            eventsArray.add(logData);
            rootObject.add("events", eventsArray);

            writer.write(gson.toJson(rootObject));
            writer.flush();
        } catch (IOException e) {
            MagLotto.getInstance().getLogger().severe("Lotto log yazılamadı: " + e.getMessage());
        }
    }

    private void appendLog(int lottoId, JsonObject logData) {
        String fileName = findExistingFileName(lottoId);
        if (fileName == null) {
            // Dosya yoksa yaz
            writeLog(lottoId, logData);
            return;
        }

        File logFile = new File(logsFolder, fileName);

        try {
            String content = new String(java.nio.file.Files.readAllBytes(logFile.toPath()));
            JsonObject rootObject = gson.fromJson(content, JsonObject.class);

            JsonArray eventsArray = rootObject.getAsJsonArray("events");
            if (eventsArray == null) {
                eventsArray = new JsonArray();
            }
            eventsArray.add(logData);
            rootObject.add("events", eventsArray);

            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(gson.toJson(rootObject));
                writer.flush();
            }

        } catch (Exception e) {
            MagLotto.getInstance().getLogger().severe("Lotto log güncellenemedi: " + e.getMessage());
        }
    }

    private String generateFileName(int lottoId) {
        return "lotto_" + lottoId + "_" + dateFormat.format(new Date()) + ".json";
    }

    /**
     * Var olan log dosyasını bulur. Aynı lottoId için tek dosya varsa onun adını döner.
     * Yoksa null döner.
     */
    private String findExistingFileName(int lottoId) {
        File[] files = logsFolder.listFiles((dir, name) -> name.startsWith("lotto_" + lottoId + "_") && name.endsWith(".json"));
        if (files == null || files.length == 0) return null;

        // En eski dosyayı veya ilk dosyayı döndürebiliriz, tek dosya varsayımıyla ilk dosya
        return files[0].getName();
    }




}
