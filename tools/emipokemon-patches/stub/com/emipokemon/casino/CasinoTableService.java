package com.emipokemon.casino;
import java.util.*;
final class CasinoTableService {
    private final Map<TableKey, Session> sessions = new HashMap<>();
    boolean handles(CasinoGameType type) { return false; }
    private void emiClaimView(UUID id, Session keep) {
        for (Session other : this.sessions.values()) {
            if (other != keep) other.viewers.remove(id);
        }
    }
    synchronized void emiReleaseView(UUID id, CasinoMachineBlockEntity machine) {
        if (!this.handles(machine.gameType())) this.emiClaimView(id, null);
    }
    private static final class Session { final LinkedHashSet<UUID> viewers = new LinkedHashSet<>(); }
    private record TableKey(Object world, long pos) {}
}
