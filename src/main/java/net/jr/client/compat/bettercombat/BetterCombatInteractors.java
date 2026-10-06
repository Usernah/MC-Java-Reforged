package net.jr.client.compat.bettercombat;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.bettercombat.client.AttackInteractor;
import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.minecraft.client.Minecraft;

public final class BetterCombatInteractors {
    private static final Map<ClientInstance, AttackInteractor> INTERACTORS = new WeakHashMap<>();

    private BetterCombatInteractors() {
    }

    public static AttackInteractor current(Minecraft minecraft) {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            client = ClientRuntime.INSTANCE.clients().client(0);
        }
        synchronized (INTERACTORS) {
            return INTERACTORS.computeIfAbsent(client, ignored -> new AttackInteractor(minecraft));
        }
    }

    public static void reset() {
        ArrayList<AttackInteractor> interactors;
        synchronized (INTERACTORS) {
            interactors = new ArrayList<>(INTERACTORS.values());
            INTERACTORS.clear();
        }
        interactors.forEach(AttackInteractor::onDisconnected);
    }
}
