package org.ozaii.maglotto.lotto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LottoGame {

    private final int id;
    private final Set<UUID> players = new HashSet<>();
    private final int startDelaySeconds;
    private final boolean manualStart;
    private final double rewardAmount;
    private final int minPlayers;
    private final LocalDateTime createdAt;
    private final int maxPlayers;

    private UUID winner;
    private boolean started = false;
    private boolean finished = false;
    private boolean cancelled = false;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String cancelReason;
    private LottoStatus status;

    public int getStartDelay() {
        return startDelaySeconds;
    }

    // Lotto durumları için enum
    public enum LottoStatus {
        WAITING_FOR_PLAYERS("Oyuncu Bekleniyor"),
        READY_TO_START("Başlamaya Hazır"),
        STARTED("Başladı"),
        FINISHED("Bitti"),
        CANCELLED("İptal Edildi");

        private final String displayName;

        LottoStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public LottoGame(int id, int startDelaySeconds, boolean manualStart, double rewardAmount, int minPlayers) {
        this(id, startDelaySeconds, manualStart, rewardAmount, minPlayers, 50); // Varsayılan max 50 oyuncu
    }

    public LottoGame(int id, int startDelaySeconds, boolean manualStart, double rewardAmount, int minPlayers, int maxPlayers) {
        this.id = id;
        this.startDelaySeconds = startDelaySeconds;
        this.manualStart = manualStart;
        this.rewardAmount = rewardAmount;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.createdAt = LocalDateTime.now();
        this.status = LottoStatus.WAITING_FOR_PLAYERS;
    }

    // Getters
    public int getId() {
        return id;
    }

    public Set<UUID> getPlayers() {
        return new HashSet<>(players); // Defensive copy
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public boolean isManualStart() {
        return manualStart;
    }

    public double getRewardAmount() {
        return rewardAmount;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public UUID getWinner() {
        return winner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LottoStatus getStatus() {
        return status;
    }

    // Durum kontrolleri
    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isActive() {
        return !finished && !cancelled;
    }

    public boolean canStart() {
        return players.size() >= minPlayers && !started && !finished && !cancelled;
    }

    public boolean canJoin() {
        return !started && !finished && !cancelled && players.size() < maxPlayers;
    }

    public boolean canLeave() {
        return !started && !finished;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasPlayer(UUID playerUUID) {
        return players.contains(playerUUID);
    }

    // Setters ve işlemler
    public void setWinner(UUID winner) {
        this.winner = winner;
    }

    public boolean addPlayer(UUID playerUUID) {
        if (!canJoin()) {
            return false;
        }

        boolean added = players.add(playerUUID);
        if (added) {
            updateStatus();
        }
        return added;
    }

    public boolean removePlayer(UUID playerUUID) {
        if (!canLeave()) {
            return false;
        }

        boolean removed = players.remove(playerUUID);
        if (removed) {
            updateStatus();
        }
        return removed;
    }

    public void start() {
        if (canStart()) {
            this.started = true;
            this.startedAt = LocalDateTime.now();
            this.status = LottoStatus.STARTED;
        }
    }

    public void finish() {
        if (started && !finished) {
            this.finished = true;
            this.finishedAt = LocalDateTime.now();
            this.status = LottoStatus.FINISHED;
        }
    }

    public void cancel(String reason) {
        if (!finished) {
            this.cancelled = true;
            this.cancelReason = reason;
            this.finishedAt = LocalDateTime.now();
            this.status = LottoStatus.CANCELLED;
        }
    }

    // Durum güncelleme
    private void updateStatus() {
        if (cancelled) {
            status = LottoStatus.CANCELLED;
        } else if (finished) {
            status = LottoStatus.FINISHED;
        } else if (started) {
            status = LottoStatus.STARTED;
        } else if (canStart()) {
            status = LottoStatus.READY_TO_START;
        } else {
            status = LottoStatus.WAITING_FOR_PLAYERS;
        }
    }

    // Utility metodları
    public String getFormattedCreatedTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public String getFormattedStartedTime() {
        return startedAt != null ? startedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "Henüz başlamadı";
    }

    public String getFormattedFinishedTime() {
        return finishedAt != null ? finishedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "Henüz bitmedi";
    }

    public long getAgeInMinutes() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    public double getCompletionPercentage() {
        return (double) players.size() / minPlayers * 100.0;
    }

    public int getRemainingSlots() {
        return maxPlayers - players.size();
    }

    public int getNeededPlayers() {
        return Math.max(0, minPlayers - players.size());
    }

    // toString için güzel bir format
    @Override
    public String toString() {
        return String.format("LottoGame{id=%d, players=%d/%d, status=%s, reward=$%.2f, created=%s}",
                id, players.size(), maxPlayers, status.getDisplayName(), rewardAmount, getFormattedCreatedTime());
    }

    // Equals ve hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LottoGame lottoGame = (LottoGame) obj;
        return id == lottoGame.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }


    // Debug bilgileri için
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LOTTO #").append(id).append(" DEBUG INFO ===\n");
        sb.append("Status: ").append(status.getDisplayName()).append("\n");
        sb.append("Players: ").append(players.size()).append("/").append(maxPlayers).append(" (min: ").append(minPlayers).append(")\n");
        sb.append("Reward: $").append(rewardAmount).append("\n");
        sb.append("Created: ").append(getFormattedCreatedTime()).append("\n");
        sb.append("Age: ").append(getAgeInMinutes()).append(" minutes\n");
        sb.append("Can Start: ").append(canStart()).append("\n");
        sb.append("Can Join: ").append(canJoin()).append("\n");
        sb.append("Is Active: ").append(isActive()).append("\n");
        if (winner != null) {
            sb.append("Winner: ").append(winner).append("\n");
        }
        if (cancelReason != null) {
            sb.append("Cancel Reason: ").append(cancelReason).append("\n");
        }
        sb.append("==============================");
        return sb.toString();
    }


}