package com.cavetale.easter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.bukkit.Bukkit;
import lombok.Getter;

public final class Timer {
    @Getter private static int dayId;
    @Getter private static int year;
    @Getter private static int month;
    @Getter private static int day;
    @Getter private static int hour;
    @Getter private static int dayOfWeek;
    public static final int ONE = 20230407;
    public static final int END = 20230421;
    public static final int DAYS = END - ONE + 1;
    public static final int EGGS_PER_DAY = 7;

    private Timer() { }

    static void update() {
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC-11"));
        LocalDate localDate = localDateTime.toLocalDate();
        year = localDate.getYear();
        month = localDate.getMonth().getValue();
        day = localDate.getDayOfMonth();
        hour = localDateTime.getHour();
        dayOfWeek = localDate.getDayOfWeek().getValue() - 1; // 0-6
        dayId = year * 10000 + month * 100 + day;
    }

    public static void enable() {
        update();
        Bukkit.getScheduler().runTaskTimer(EasterPlugin.instance, Timer::update, 200L, 200L);
    }

    /**
     * Counting the day from the start of the event to the end. The
     * day after Easter Monday is the finale. Any other day yields 0.
     */
    public static int getEasterDay() {
        if (dayId < ONE || dayId > END) return 0;
        return dayId - ONE + 1;
    }

    /**
     * How many regular eggs a player can find right now.
     * Basically amount of days times eggs per day, except the final
     * day is considered overhang and does not count.
     */
    public static int getTotalEggs() {
        return Math.min(DAYS - 1, getEasterDay()) * EGGS_PER_DAY;
    }
}
