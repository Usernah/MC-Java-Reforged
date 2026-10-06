package net.jr.client.runtime.context;

import java.util.ArrayDeque;
import java.util.Deque;
import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.Minecraft;

public final class PrimaryTickScope {
    private static final ThreadLocal<Deque<InstanceExecution.Scope>> SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private PrimaryTickScope() {
    }

    public static void begin(Minecraft minecraft) {
        ClientRuntime.INSTANCE.viewportResize().refreshWindow(minecraft);
        SCOPES.get().push(InstanceExecution.enterPrimary(minecraft));
    }

    public static void end() {
        Deque<InstanceExecution.Scope> scopes = SCOPES.get();
        if (scopes.isEmpty()) {
            throw new IllegalStateException("Primary runTick scope was not opened");
        }
        scopes.pop().close();
        if (scopes.isEmpty()) {
            SCOPES.remove();
        }
    }
}
