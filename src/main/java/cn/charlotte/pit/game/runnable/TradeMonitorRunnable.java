package cn.charlotte.pit.game.runnable;

import cn.charlotte.pit.data.temp.TradeRequest;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/5 0:30
 */
public class TradeMonitorRunnable extends BukkitRunnable {
    private static final Map<UUID, Map<UUID, TradeRequest>> tradeRequestsByTarget = new HashMap<>();

    public static List<TradeRequest> getTradeRequests() {
        removeExpiredRequests();
        List<TradeRequest> requests = new ArrayList<>();
        for (Map<UUID, TradeRequest> requestsByPlayer : tradeRequestsByTarget.values()) {
            requests.addAll(requestsByPlayer.values());
        }
        return requests;
    }

    public static List<TradeRequest> getPendingRequests(UUID target) {
        Map<UUID, TradeRequest> requestsByPlayer = tradeRequestsByTarget.get(target);
        if (requestsByPlayer == null || requestsByPlayer.isEmpty()) {
            return new ArrayList<>(0);
        }

        removeExpiredRequests(target, requestsByPlayer);
        return new ArrayList<>(requestsByPlayer.values());
    }

    public static TradeRequest getRequest(UUID player, UUID target) {
        Map<UUID, TradeRequest> requestsByPlayer = tradeRequestsByTarget.get(target);
        if (requestsByPlayer == null || requestsByPlayer.isEmpty()) {
            return null;
        }

        TradeRequest request = requestsByPlayer.get(player);
        if (request == null) {
            return null;
        }
        if (request.getCooldown().hasExpired()) {
            requestsByPlayer.remove(player);
            if (requestsByPlayer.isEmpty()) {
                tradeRequestsByTarget.remove(target);
            }
            return null;
        }
        return request;
    }

    public static void addRequest(TradeRequest request) {
        removeExpiredRequests();
        tradeRequestsByTarget
                .computeIfAbsent(request.getTarget(), key -> new HashMap<>())
                .put(request.getPlayer(), request);
    }

    public static void removeRequest(UUID player, UUID target) {
        Map<UUID, TradeRequest> requestsByPlayer = tradeRequestsByTarget.get(target);
        if (requestsByPlayer == null) {
            return;
        }

        requestsByPlayer.remove(player);
        if (requestsByPlayer.isEmpty()) {
            tradeRequestsByTarget.remove(target);
        }
    }

    private static void removeExpiredRequests() {
        List<UUID> emptyTargets = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, TradeRequest>> entry : tradeRequestsByTarget.entrySet()) {
            removeExpiredRequests(entry.getKey(), entry.getValue());
            if (entry.getValue().isEmpty()) {
                emptyTargets.add(entry.getKey());
            }
        }
        emptyTargets.forEach(tradeRequestsByTarget::remove);
    }

    private static void removeExpiredRequests(UUID target, Map<UUID, TradeRequest> requestsByPlayer) {
        requestsByPlayer.entrySet().removeIf(entry -> entry.getValue().getCooldown().hasExpired());
    }

    @Override
    public void run() {
        removeExpiredRequests();
    }
}
