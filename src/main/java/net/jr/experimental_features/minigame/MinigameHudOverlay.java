package net.jr.experimental_features.minigame;

import net.jr.api.client.render.Draw;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.api.client.render.gui.HudRenderObject;
import net.jr.client.components.elements.TextElement;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.minecraft.network.chat.Component;


public final class MinigameHudOverlay implements HudRenderObject {
    @Override
    public void render(HudRenderContext context) {
        if (!BattleClientState.isBattle()) {
            return;
        }
        renderheadingLabel(context);
    }

    private static void renderheadingLabel(HudRenderContext context) {
        String statusText = resolveHudMessage();
        if (statusText == null || statusText.isBlank()) {
            return;
        }
        TextElement headingLabel = new TextElement(
            Component.literal(statusText), (float) context.viewport().guiWidth() / 2, 10, context.graphics()
        );
        headingLabel.setShadow(true);
        headingLabel.setAlign(Draw.TextAlign.CENTER);
        headingLabel.draw(context.graphics());
    }

    private static String resolveHudMessage() {
        BattlePhase phase = BattleClientState.phase();
        if (phase == null) {
            return null;
        }

        int remainingSeconds = BattleClientState.remainingSecondsCeil();
        return switch (phase) {
            case WAIT_FOR_PLAYERS -> "Esperando jugadores";
            case COUNTDOWN -> "La ronda comienza en: " + remainingSeconds + " segundos";
            case START -> "Comienza la ronda";
            case INMUNITY_COUNTDOWN -> "La invulnerabilidad terminara en: " + remainingSeconds + " segundos";
            case BATTLE -> BattleClientState.elapsedTicks() < (5 * 20) ? "La invulnerabilidad ha terminado" : null;
            case SHOWDOWN -> remainingSeconds <= 10
                    ? "La ronda termina en: " + remainingSeconds + " segundos"
                    : "Showdown";
            case WINNER_ANOUNCEMENT -> BattleClientState.winnerName().isBlank()
                    ? "¡Tenemos un ganador!" : "Ganador: " + BattleClientState.winnerName();
            case FINISH -> "Fin de la ronda";
        };
    }

}
