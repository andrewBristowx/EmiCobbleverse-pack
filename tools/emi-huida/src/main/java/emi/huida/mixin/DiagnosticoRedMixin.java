package emi.huida.mixin;

import emi.huida.EmiHuida;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Diagnostico (0.6.1): Minecraft solo enseña «Internal Exception: DecoderException: Failed to decode packet ...» sin decir QUE mod lo causa
 * (la causa se escribe a nivel debug, que no sale en latest.log). Esto escribe en el log, con la pila completa, las primeras excepciones
 * de red que lleguen a la conexion (cliente o servidor integrado), para poder saber que paquete y que mod fallan. No cambia nada mas.
 */
@Mixin(ClientConnection.class)
public abstract class DiagnosticoRedMixin {
    private static final AtomicInteger emi_huida$n = new AtomicInteger();

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void emi_huida$diagnostico(ChannelHandlerContext ctx, Throwable ex, CallbackInfo ci) {
        if (emi_huida$n.incrementAndGet() <= 6) EmiHuida.LOG.warn("[diagnostico de red] excepcion en la conexion (hilo {}): {}", Thread.currentThread().getName(), ex.toString(), ex);
    }
}
