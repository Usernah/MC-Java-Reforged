package net.jr.client.sound.config;

public class SoundTransitionConfig {
    public static boolean alwaysPlayMusic = false;
    public static int fadeInTicks = 40;
    public static int fadeOutTicks = 60;
    public static boolean seamlessTransitions = true;
    public static int seamlessSoundTransitions = 0;
    public static boolean allowPausingMusic = false;

    public static int minStartTicks = 20 * 20;
    public static int maxStartTicks = 60 * 20;
    public static int minDelayTicks = 30 * 20;
    public static int maxDelayTicks = 240 * 20;

    public static int menuMinStartTicks = minStartTicks;
    public static int menuMaxStartTicks = maxStartTicks;
    public static int menuMinDelayTicks = minDelayTicks;
    public static int menuMaxDelayTicks = maxDelayTicks;
}
