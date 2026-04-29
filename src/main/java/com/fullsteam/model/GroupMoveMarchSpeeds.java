package com.fullsteam.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-unit march speed caps for grouped move orders: units in the same spatial cluster
 * (and same elevation band) move at the slowest member's speed. Units isolated from the selection
 * by distance do not get a cap.
 */
public final class GroupMoveMarchSpeeds {

    /**
     * Max distance between unit centers for an edge in the cohesion graph. Units farther apart
     * than this (transitively) do not share a march-speed cluster.
     */
    public static final double CLUSTER_LINK_DISTANCE = 380.0;

    private GroupMoveMarchSpeeds() {
    }

    /**
     * @param movers non-empty list of units receiving the same move order
     * @return map of unit → march speed cap for units in a cluster of size ≥ 2; singletons omitted
     */
    public static Map<Unit, Double> computeMarchSpeedCaps(List<Unit> movers) {
        Map<Unit, Double> caps = new HashMap<>();
        if (movers == null || movers.size() < 2) {
            return caps;
        }

        int n = movers.size();
        boolean[] visited = new boolean[n];
        List<List<Integer>> adj = buildAdjacency(movers, n);

        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                continue;
            }
            List<Integer> comp = new ArrayList<>();
            dfs(i, adj, visited, comp);
            if (comp.size() < 2) {
                continue;
            }
            double minSpeed = Double.POSITIVE_INFINITY;
            for (int idx : comp) {
                minSpeed = Math.min(minSpeed, movers.get(idx).getMovementSpeed());
            }
            if (minSpeed <= 0 || !Double.isFinite(minSpeed)) {
                continue;
            }
            for (int idx : comp) {
                caps.put(movers.get(idx), minSpeed);
            }
        }
        return caps;
    }

    private static List<List<Integer>> buildAdjacency(List<Unit> movers, int n) {
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            Unit a = movers.get(i);
            for (int j = i + 1; j < n; j++) {
                Unit b = movers.get(j);
                if (!sameElevationBand(a, b)) {
                    continue;
                }
                if (a.getPosition().distance(b.getPosition()) <= CLUSTER_LINK_DISTANCE) {
                    adj.get(i).add(j);
                    adj.get(j).add(i);
                }
            }
        }
        return adj;
    }

    private static boolean sameElevationBand(Unit a, Unit b) {
        return a.getUnitType().getElevation() == b.getUnitType().getElevation();
    }

    private static void dfs(int start, List<List<Integer>> adj, boolean[] visited, List<Integer> out) {
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(start);
        while (!stack.isEmpty()) {
            int u = stack.remove(stack.size() - 1);
            if (visited[u]) {
                continue;
            }
            visited[u] = true;
            out.add(u);
            for (int v : adj.get(u)) {
                if (!visited[v]) {
                    stack.add(v);
                }
            }
        }
    }
}
