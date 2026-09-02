package net.jr.client.runtime.render.experimental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import net.jr.Java_reforged;
import net.jr.client.input.binding.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** A deliberately bounded allocation/GC capture for render experiments. */
@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class ControlledJfrProfiler {
    private static final Duration CAPTURE_DURATION = Duration.ofSeconds(30L);
    private static final long MAX_CAPTURE_SIZE = 512L * 1024L * 1024L;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static Recording activeRecording;
    private static Path activeDestination;

    private ControlledJfrProfiler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeyBindings.PROFILE_ALLOCATIONS.consumeClick()) {
            if (activeRecording == null) {
                start(minecraft);
            } else {
                stop(minecraft, true);
            }
        }

        if (activeRecording != null && activeRecording.getState() == RecordingState.STOPPED) {
            finish(minecraft, false);
        }
    }

    private static void start(Minecraft minecraft) {
        if (!FlightRecorder.isAvailable()) {
            notify(minecraft, Component.translatable("message.java_reforged.jfr.unavailable").withStyle(ChatFormatting.RED));
            return;
        }

        Recording recording = null;
        try {
            Path directory = minecraft.gameDirectory.toPath().resolve("profiling");
            Files.createDirectories(directory);
            Path destination = directory.resolve(
                "java-reforged-allocation-" + FILE_TIME.format(LocalDateTime.now()) + ".jfr"
            );

            recording = new Recording(Configuration.getConfiguration("profile"));
            recording.setName("Java Reforged controlled allocation capture");
            recording.setDuration(CAPTURE_DURATION);
            recording.setDestination(destination);
            recording.setDumpOnExit(true);
            recording.setToDisk(true);
            recording.setMaxSize(MAX_CAPTURE_SIZE);
            enableMemoryDiagnostics(recording);
            recording.start();

            activeRecording = recording;
            activeDestination = destination;
            notify(minecraft, Component.translatable("message.java_reforged.jfr.started", CAPTURE_DURATION.toSeconds())
                .withStyle(ChatFormatting.YELLOW));
            Java_reforged.LOGGER.info("Captura JFR controlada iniciada: {}", destination.toAbsolutePath());
        } catch (Exception exception) {
            if (recording != null) {
                recording.close();
            }
            notify(minecraft, Component.translatable("message.java_reforged.jfr.failed").withStyle(ChatFormatting.RED));
            Java_reforged.LOGGER.error("No se pudo iniciar la captura JFR controlada", exception);
        }
    }

    private static void enableMemoryDiagnostics(Recording recording) {
        recording.enable("jdk.ObjectAllocationSample")
            .with("throttle", "1000/s")
            .withStackTrace();

        // Exact allocation sites for large arrays and objects that bypass the TLAB.
        recording.enable("jdk.ObjectAllocationOutsideTLAB").withStackTrace();
        // TLAB refill sites complement the sampled allocation view without recording every object.
        recording.enable("jdk.ObjectAllocationInNewTLAB").withStackTrace();

        // Preserve allocation stacks and GC-root paths for objects that survive collection.
        recording.enable("jdk.OldObjectSample")
            .withStackTrace()
            .with("cutoff", "1 h");
        recording.enable("jdk.ObjectCountAfterGC");

        // Keep the heap/collector timeline explicit even if the bundled profile changes.
        recording.enable("jdk.GCHeapSummary");
        recording.enable("jdk.G1HeapSummary");
        recording.enable("jdk.GarbageCollection").withThreshold(Duration.ZERO);
        recording.enable("jdk.YoungGarbageCollection").withThreshold(Duration.ZERO);
        recording.enable("jdk.OldGarbageCollection").withThreshold(Duration.ZERO);
    }

    private static void stop(Minecraft minecraft, boolean cancelledEarly) {
        try {
            if (activeRecording.getState() == RecordingState.RUNNING) {
                activeRecording.stop();
            }
            finish(minecraft, cancelledEarly);
        } catch (Exception exception) {
            Java_reforged.LOGGER.error("No se pudo detener la captura JFR controlada", exception);
            closeAndClear();
            notify(minecraft, Component.translatable("message.java_reforged.jfr.failed").withStyle(ChatFormatting.RED));
        }
    }

    private static void finish(Minecraft minecraft, boolean stoppedEarly) {
        Path destination = activeDestination;
        closeAndClear();
        notify(minecraft, Component.translatable(
            stoppedEarly ? "message.java_reforged.jfr.stopped" : "message.java_reforged.jfr.finished",
            destination == null ? "profiling" : destination.getFileName().toString()
        ).withStyle(ChatFormatting.GREEN));
        if (destination != null) {
            Java_reforged.LOGGER.info("Captura JFR controlada guardada: {}", destination.toAbsolutePath());
        }
    }

    private static void closeAndClear() {
        Recording recording = activeRecording;
        activeRecording = null;
        activeDestination = null;
        if (recording != null) {
            recording.close();
        }
    }

    private static void notify(Minecraft minecraft, Component message) {
        minecraft.gui.hud.setOverlayMessage(message, false);
    }
}
