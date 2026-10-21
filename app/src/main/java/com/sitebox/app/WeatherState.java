package com.sitebox.app;

import java.util.Calendar;

/** Immutable weather snapshot. All timestamps are UTC epoch milliseconds. */
public final class WeatherState {
    public final double temperature, wind;
    public final int code, cloud;
    /** Offset from the selected city response, or MIN_VALUE for legacy/device fallback. */
    public final int utcOffsetSeconds, isDay;
    public final long observedAt, sunrise, sunset, fetchedAt;
    public final boolean available;
    private static final long HOUR = 3600000L;
    public WeatherState(double temperature, double wind, int code, int cloud,
                        long observedAt, long sunrise, long sunset, long fetchedAt, boolean available) {
        this(temperature, wind, code, cloud, observedAt, sunrise, sunset, fetchedAt, available,
                Integer.MIN_VALUE, -1);
    }
    public WeatherState(double temperature, double wind, int code, int cloud,
                        long observedAt, long sunrise, long sunset, long fetchedAt, boolean available,
                        int utcOffsetSeconds, int isDay) {
        this.utcOffsetSeconds = Math.abs((long)utcOffsetSeconds) <= 18 * 3600 ? utcOffsetSeconds : Integer.MIN_VALUE;
        this.isDay = isDay == 0 || isDay == 1 ? isDay : -1;
        boolean valid = Double.isFinite(temperature) && temperature >= -100 && temperature <= 70
                && Double.isFinite(wind) && wind >= 0 && wind <= 150 && code >= 0 && code <= 99
                && cloud >= 0 && cloud <= 100 && validTime(observedAt) && validTime(fetchedAt);
        this.available = available && valid;
        this.temperature = this.available ? temperature : 0;
        this.wind = this.available ? wind : 0;
        this.code = this.available ? code : 0;
        this.cloud = this.available ? cloud : 0;
        this.observedAt = this.available ? observedAt : 0;
        this.fetchedAt = this.available ? fetchedAt : 0;
        boolean solarValid = validTime(sunrise) && validTime(sunset) && sunset > sunrise && sunset - sunrise < 24 * HOUR;
        this.sunrise = solarValid ? sunrise : 0;
        this.sunset = solarValid ? sunset : 0;
    }
    private static boolean validTime(long time) { return time > 0 && time <= 32503680000000L; }
    public static WeatherState empty() { return new WeatherState(0, 0, 0, 0, 0, 0, 0, 0, false); }
    /** A recent city's solar cycle may be projected by one day across midnight.
     * Older/missing solar data uses the city's saved UTC offset; empty/legacy data uses device time.
     */
    public float daylight(long now) {
        long rise = sunrise, set = sunset;
        long noon = rise + (set - rise) / 2;
        boolean inSolarDay = rise != 0 && now >= noon - 12 * HOUR && now < noon + 12 * HOUR;
        if (available && !inSolarDay && rise != 0 && utcOffsetSeconds != Integer.MIN_VALUE
                && now >= fetchedAt && now - fetchedAt <= 48 * HOUR) {
            long shift = Math.round((now - noon) / (double)(24 * HOUR));
            if (Math.abs(shift) <= 1) { rise += shift * 24 * HOUR; set += shift * 24 * HOUR; inSolarDay = true; }
        }
        if (!available || !inSolarDay) {
            // A current polar-day/night observation is more reliable than a clock estimate.
            if (available && sunrise == 0 && isDay >= 0 && now >= observedAt && now - observedAt <= 2 * HOUR)
                return isDay;
            if (available && utcOffsetSeconds != Integer.MIN_VALUE) {
                long local = now + utcOffsetSeconds * 1000L;
                long midnight = Math.floorDiv(local, 24 * HOUR) * 24 * HOUR - utcOffsetSeconds * 1000L;
                rise = midnight + 6 * HOUR; set = midnight + 18 * HOUR;
            } else {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(now);
                c.set(Calendar.HOUR_OF_DAY, 6); c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                rise = c.getTimeInMillis();
                c.set(Calendar.HOUR_OF_DAY, 18); set = c.getTimeInMillis();
            }
        }
        long twilight = 45 * 60000L;
        if (now <= rise - twilight || now >= set + twilight) return 0f;
        if (now < rise + twilight) return clamp((now - (rise - twilight)) / (float)(2 * twilight));
        if (now > set - twilight) return clamp((set + twilight - now) / (float)(2 * twilight));
        return 1f;
    }
    private static float clamp(float x) { return Math.max(0, Math.min(1, x)); }
    public int precipitation() {
        if (!available) return 0;
        if (code == 71 || code == 73 || code == 75 || code == 77 || code == 85 || code == 86) return 2;
        if (code == 51 || code == 53 || code == 55 || code == 56 || code == 57
                || code == 61 || code == 63 || code == 65 || code == 66 || code == 67
                || code == 80 || code == 81 || code == 82 || code == 95 || code == 96 || code == 99) return 1;
        return 0;
    }
    public float cloudiness() { return available ? cloud / 100f : 0.25f; }
    public boolean stale(long now) { return !available || now < fetchedAt || now - fetchedAt > HOUR / 2
            || now < observedAt || now - observedAt > HOUR / 2; }
    public String description() {
        if (!available) return "날씨 정보 없음";
        if (precipitation() == 2) return "눈";
        if (code >= 95) return "뇌우";
        if (precipitation() == 1) return "비";
        if (code == 45 || code == 48) return "안개";
        if (code == 0) return "맑음";
        if (code == 1 || code == 2) return "구름 조금";
        if (code == 3) return "흐림";
        return "날씨 정보";
    }
}
