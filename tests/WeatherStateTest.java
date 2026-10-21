package com.sitebox.app;

import java.util.Calendar;
import java.util.TimeZone;

public final class WeatherStateTest {
    private static final long H = 3600000L;
    private static int checks;
    private static void check(boolean result, String name) {
        checks++; if (!result) throw new AssertionError(name);
    }
    private static WeatherState weather(int code, long rise, long set, long at) {
        return new WeatherState(20, 3, code, 80, at, rise, set, at, true);
    }
    public static void main(String[] args) {
        TimeZone old = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Calendar c = Calendar.getInstance(); c.clear(); c.set(2026, Calendar.OCTOBER, 21);
            long day = c.getTimeInMillis(), rise = day + 7 * H, set = day + 17 * H;
            WeatherState w = weather(61, rise, set, day + 12 * H);
            check(w.daylight(day + 12 * H) == 1, "noon");
            check(w.daylight(day + 2 * H) == 0, "night");
            check(w.daylight(rise) == .5f, "sunrise twilight");
            check(w.daylight(set) == .5f, "sunset twilight");
            check(w.daylight(rise - 45 * 60000L) == 0, "twilight boundary");
            check(w.daylight(rise + 45 * 60000L) == 1, "day boundary");
            check(w.daylight(day + 24 * H + 6 * H) == .5f, "next day uses fallback dawn");
            check(w.daylight(day - 24 * H + 18 * H) == .5f, "old day fallback dusk");
            check(WeatherState.empty().daylight(day + 12 * H) == 1, "empty fallback");
            check(WeatherState.empty().daylight(day + 2 * H) == 0, "empty night");
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
            check(WeatherState.empty().daylight(day + 3 * H) == 1, "device local noon");
            check(WeatherState.empty().daylight(day + 15 * H) == 0, "device local midnight");
            check(w.daylight(day + 12 * H) == 1, "valid solar ignores device zone");
            for (int code : new int[]{51,53,55,56,57,61,63,65,66,67,80,81,82,95,96,99})
                check(weather(code,rise,set,day).precipitation() == 1,"rain " + code);
            for (int code : new int[]{71,73,75,77,85,86})
                check(weather(code,rise,set,day).precipitation() == 2,"snow " + code);
            for (int code : new int[]{0,1,2,3,45,48})
                check(weather(code,rise,set,day).precipitation() == 0,"dry " + code);
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            // Seoul's solar noon is 03:00 UTC, midnight is 15:00 UTC: device UTC must not brighten it.
            WeatherState seoul = new WeatherState(20,3,0,20,day+3*H,day-3*H,day+9*H,day+3*H,true,9*3600,1);
            check(seoul.daylight(day+15*H) == 0, "selected city midnight across solar boundary");
            check(seoul.daylight(day+16*H) == 0, "selected city early night next day cache");
            check(seoul.daylight(day+24*H+3*H) == 1, "next day cached solar projection noon");
            check(seoul.daylight(day+24*H-3*H) == .5f, "next day cached solar projection sunrise");
            check(seoul.daylight(day+72*H+15*H) == 0, "expired solar uses selected offset midnight");
            WeatherState missingSolar = new WeatherState(20,3,0,20,day+3*H,0,0,day+3*H,true,9*3600,-1);
            check(missingSolar.daylight(day+15*H) == 0, "missing solar selected offset midnight");
            check(missingSolar.daylight(day+3*H) == 1, "missing solar selected offset noon");
            WeatherState polarDay = new WeatherState(20,3,0,20,day,0,0,day,true,0,1);
            WeatherState polarNight = new WeatherState(20,3,0,20,day+12*H,0,0,day+12*H,true,0,0);
            check(polarDay.daylight(day+H) == 1, "current polar day");
            check(polarNight.daylight(day+13*H) == 0, "current polar night");
            check(w.cloudiness() == .8f, "cloud scale");
            check(!w.stale(w.fetchedAt + H/2), "thirty minute boundary");
            check(w.stale(w.fetchedAt + H/2+1), "expired");
            check(w.stale(w.fetchedAt-1), "future timestamp");
            check(WeatherState.empty().stale(day), "missing stale");
            check(!new WeatherState(Double.NaN,3,61,80,day,rise,set,day,true).available,"nan");
            check(!new WeatherState(20,Double.POSITIVE_INFINITY,61,80,day,rise,set,day,true).available,"infinity");
            check(!new WeatherState(-101,3,61,80,day,rise,set,day,true).available,"extreme temperature");
            check(!new WeatherState(20,-1,61,80,day,rise,set,day,true).available,"negative wind");
            check(!new WeatherState(20,3,61,101,day,rise,set,day,true).available,"cloud bounds");
            check(!new WeatherState(20,3,100,80,day,rise,set,day,true).available,"code bounds");
            check(new WeatherState(20,3,61,80,day,set,rise,day,true).sunrise == 0,"bad solar discarded");
            check(!new WeatherState(20,3,61,80,Long.MAX_VALUE,rise,set,day,true).available,"timestamp bounds");
            check("눈".equals(weather(71,rise,set,day).description()),"Korean snow");
            check("뇌우".equals(weather(95,rise,set,day).description()),"Korean thunder");
            System.out.println("WeatherStateTest: " + checks + " checks passed");
        } finally { TimeZone.setDefault(old); }
    }
}
