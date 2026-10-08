package emi.buildbattle;

import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Method;

/**
 * Permisos del organizador. Si LuckPerms (fabric-permissions-api) esta instalado se puede dar el permiso
 * "emi_buildbattle.admin" a un grupo (ej. los moderadores); si no, hace falta ser operador (nivel 2).
 */
public final class Perm {
    public static final String ADMIN = "emi_buildbattle.admin";
    private static Method check;
    private static boolean tried;

    public static boolean admin(ServerCommandSource src) {
        if (src.hasPermissionLevel(2)) return true;
        if (!tried) {
            tried = true;
            try {
                Class<?> c = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
                check = c.getMethod("check", net.minecraft.command.CommandSource.class, String.class, int.class);
            } catch (Throwable t) {
                check = null;
            }
        }
        if (check == null) return false;
        try {
            return (boolean) check.invoke(null, src, ADMIN, 2);
        } catch (Throwable t) {
            return false;
        }
    }
}
