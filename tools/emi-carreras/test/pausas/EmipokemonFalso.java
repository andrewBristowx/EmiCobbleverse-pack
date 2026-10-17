package com.emipokemon;
public class Emipokemon {
    public static class Sub { public boolean enabled = true; }
    public static class Cfg { public boolean itemCleanupEnabled = true, arrowCleanupEnabled = true, entityCapEnabled = true; public Sub megaBoss = new Sub(), gigantamaxBoss = new Sub(), teraBoss = new Sub(), shinyEvent = new Sub(), legendaryTrainer = new Sub(); }
    public static class Mgr { public final Cfg c = new Cfg(); public Cfg get() { return c; } }
    static final Mgr M = new Mgr();
    public static Mgr configManager() { return M; }
}
