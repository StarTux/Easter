package com.cavetale.easter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import lombok.Getter;
import org.bukkit.Bukkit;

public final class Timer {
    @Getter private static int dayId;
    @Getter private static int year;
    @Getter private static int month;
    @Getter private static int day;
    @Getter private static int hour;
    @Getter private static int dayOfWeek;
    public static final int ONE = 2026_04_03;
    public static final int END = 2026_04_17;
    private static final int[] EASTER_DAYS = {
        ONE,
        2026_04_04,
        2026_04_05,
        2026_04_06,
        2026_04_07,
        2026_04_08,
        2026_04_09,
        2026_04_10,
        2026_04_11,
        2026_04_12,
        2026_04_13,
        2026_04_14,
        2026_04_15,
        2026_04_16,
        END,
    };
    public static final int DAYS = EASTER_DAYS.length;
    public static final int EGGS_PER_DAY = 10;
    public static final ZoneId ZONE_ID = ZoneId.of("UTC-11");

    private Timer() { }

    static void update() {
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZONE_ID);
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
        return Arrays.binarySearch(EASTER_DAYS, dayId) + 1;
    }

    /**
     * How many regular eggs a player can find right now.
     * Basically amount of days times eggs per day.
     */
    public static int getTotalEggs() {
        return Math.min(DAYS, getEasterDay()) * EGGS_PER_DAY;
    }
}
