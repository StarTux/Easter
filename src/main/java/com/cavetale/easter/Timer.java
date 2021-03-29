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

    private Timer() { }

    static void update() {
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
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

    public static boolean isEasterSeason() {
        // Easter 2021 is April 4
        return month < 4 || (month == 4 && day <= 4);
    }

    public static int getEasterDay() {
        switch (dayId) {
        case 20210329: return 1;
        case 20210330: return 2;
        case 20210331: return 3;
        case 20210401: return 4;
        case 20210402: return 5;
        case 20210403: return 6;
        case 20210404: return 7;
        default: return 0;
        }
    }

    public static int getTotalEggs() {
        return getEasterDay() * 10;
    }
}
