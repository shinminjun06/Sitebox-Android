package com.sitebox.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

/** Foreground-only weather retrieval; callers own refresh scheduling and lifecycle. */
public final class WeatherRepository {
    public interface Listener { void onWeather(WeatherState state, String status); }
    public interface SearchListener { void onResult(List<Place> places, String error); }
    public static final class Place {
        public final String name;
        public final double latitude, longitude;
        public Place(String name, double latitude, double longitude) {
            if (name == null || name.trim().isEmpty() || name.length() > 160
                    || !Double.isFinite(latitude) || Math.abs(latitude) > 90
                    || !Double.isFinite(longitude) || Math.abs(longitude) > 180)
                throw new IllegalArgumentException("Invalid place");
            this.name = name.trim(); this.latitude = latitude; this.longitude = longitude;
        }
    }
    private static final long INTERVAL = 30 * 60000L;
    private static final int MAX_RESPONSE = 256 * 1024;
    private final SharedPreferences prefs;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    // At most one pending fetch and one pending search; newer work cancels its predecessor.
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2), new ThreadPoolExecutor.DiscardOldestPolicy());
    private Future<?> fetchJob, searchJob;
    private boolean connectionIsSearch;
    private volatile HttpsURLConnection connection;
    private volatile boolean closed;
    private WeatherState current = WeatherState.empty();
    private Place selected;
    private String message = "서울 · 날씨 대기 중";
    private long generation, searchGeneration, lastAttempt;
    private boolean loading;

    public WeatherRepository(Context context, Listener listener) {
        this.listener = listener;
        prefs = context.getApplicationContext().getSharedPreferences("sitebox_weather", Context.MODE_PRIVATE);
        try { selected = new Place(prefs.getString("place_name", "서울 (기본 도시)"),
                Double.longBitsToDouble(prefs.getLong("place_lat", Double.doubleToLongBits(37.5665))),
                Double.longBitsToDouble(prefs.getLong("place_lon", Double.doubleToLongBits(126.9780))));
        } catch (RuntimeException e) { selected = new Place("서울 (기본 도시)", 37.5665, 126.9780); }
        try {
            if (prefs.getString("cache_lat", "").equals(Double.toString(selected.latitude))
                    && prefs.getString("cache_lon", "").equals(Double.toString(selected.longitude))) {
                current = decode(new JSONObject(prefs.getString("snapshot", "{}")));
            }
        } catch (Exception ignored) { current = WeatherState.empty(); }
        message = current.available ? "저장된 날씨" : "날씨 대기 중";
    }
    public synchronized WeatherState state() { return current; }
    public synchronized Place place() { return selected; }
    public synchronized String status() {
        return message + (current.available && current.stale(System.currentTimeMillis()) ? " · 오래된 정보" : "");
    }
    public synchronized void refresh(boolean force) {
        if (closed || loading) return;
        long now = System.currentTimeMillis();
        if (!force && ((lastAttempt > 0 && now >= lastAttempt && now - lastAttempt < INTERVAL)
                || (current.available && now >= current.fetchedAt && now - current.fetchedAt < INTERVAL))) return;
        loading = true; lastAttempt = now;
        final long ticket = generation;
        final Place target = selected;
        message = "날씨 업데이트 중";
        notifyWeather();
        cancelQueued(fetchJob);
        fetchJob = worker.submit(() -> {
            synchronized (WeatherRepository.this) { if (closed || ticket != generation) return; }
            WeatherState result = null;
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + target.latitude
                        + "&longitude=" + target.longitude
                        + "&current=temperature_2m,weather_code,cloud_cover,wind_speed_10m,is_day"
                        + "&daily=sunrise,sunset&timeformat=unixtime&timezone=auto&forecast_days=1&wind_speed_unit=ms";
                JSONObject root = request(url, ticket, false);
                JSONObject c = root.getJSONObject("current"), daily = root.getJSONObject("daily");
                result = new WeatherState(c.getDouble("temperature_2m"), c.getDouble("wind_speed_10m"),
                        requiredInt(c, "weather_code"), requiredInt(c, "cloud_cover"), seconds(c.getDouble("time")),
                        solarTime(daily, "sunrise"), solarTime(daily, "sunset"), System.currentTimeMillis(), true,
                        requiredInt(root, "utc_offset_seconds"), requiredInt(c, "is_day"));
                if (!result.available || result.utcOffsetSeconds == Integer.MIN_VALUE || result.isDay < 0)
                    throw new IllegalArgumentException("Invalid weather");
                if (Math.abs(System.currentTimeMillis() - result.observedAt) > 24 * 3600000L)
                    throw new IllegalArgumentException("Invalid weather time");
            } catch (Exception ignored) { result = null; }
            final WeatherState received = result;
            post(() -> {
                synchronized (WeatherRepository.this) {
                    if (ticket != generation) return;
                    loading = false;
                    if (received != null) {
                        current = received; message = "Open-Meteo · 업데이트됨";
                        prefs.edit().putString("cache_lat", Double.toString(target.latitude))
                                .putString("cache_lon", Double.toString(target.longitude))
                                .putString("snapshot", encode(received).toString()).apply();
                    } else message = current.available ? "연결 실패 · 저장된 날씨" : "연결 실패 · 시간에 따른 배경";
                    notifyWeather();
                }
            });
        });
    }
    public synchronized void setPlace(Place place) {
        if (closed || place == null) return;
        generation++; searchGeneration++;
        cancelQueued(fetchJob); cancelQueued(searchJob);
        selected = place; current = WeatherState.empty(); loading = false; lastAttempt = 0;
        HttpsURLConnection active = connection; if (active != null) active.disconnect();
        prefs.edit().putString("place_name", place.name)
                .putLong("place_lat", Double.doubleToLongBits(place.latitude))
                .putLong("place_lon", Double.doubleToLongBits(place.longitude))
                .remove("snapshot").remove("cache_lat").remove("cache_lon").apply();
        refresh(true);
    }
    public synchronized void search(String query, SearchListener callback) {
        if (closed || callback == null) return;
        final long ticket = ++searchGeneration;
        cancelQueued(searchJob);
        if (connectionIsSearch && connection != null) connection.disconnect();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2 || q.length() > 80) {
            post(() -> { synchronized (WeatherRepository.this) {
                if (ticket == searchGeneration) callback.onResult(new ArrayList<>(), "도시 이름은 2~80자로 입력해 주세요");
            }}); return;
        }
        searchJob = worker.submit(() -> {
            synchronized (WeatherRepository.this) { if (closed || ticket != searchGeneration) return; }
            List<Place> places = new ArrayList<>(); String error = null;
            try {
                JSONObject root = request("https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(q, "UTF-8") + "&count=5&language=ko&format=json", ticket, true);
                JSONArray rows = root.optJSONArray("results");
                if (rows != null) for (int i = 0; i < Math.min(5, rows.length()); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    String label = row.getString("name");
                    String region = row.optString("admin1", ""), country = row.optString("country", "");
                    if (!region.isEmpty()) label += ", " + region;
                    if (!country.isEmpty()) label += ", " + country;
                    places.add(new Place(label, row.getDouble("latitude"), row.getDouble("longitude")));
                }
            } catch (Exception ignored) { error = "도시를 검색할 수 없습니다. 다시 시도해 주세요"; }
            final String failure = error;
            post(() -> { synchronized (WeatherRepository.this) {
                if (ticket == searchGeneration) callback.onResult(places, failure);
            }});
        });
    }
    private void cancelQueued(Future<?> previous) {
        if (previous != null) previous.cancel(false);
        worker.purge();
    }
    private void notifyWeather() {
        post(() -> { synchronized (WeatherRepository.this) {
            if (listener != null) listener.onWeather(current, status());
        }});
    }
    private synchronized void post(Runnable action) {
        if (!closed) main.post(() -> { synchronized (WeatherRepository.this) { if (!closed) action.run(); }});
    }
    public synchronized void close() {
        closed = true; generation++; searchGeneration++;
        main.removeCallbacksAndMessages(null); worker.shutdownNow();
        HttpsURLConnection active = connection; if (active != null) active.disconnect();
    }
    private JSONObject request(String address, long ticket, boolean search) throws Exception {
        URL url = new URL(address);
        if (!"https".equals(url.getProtocol()) || !("api.open-meteo.com".equals(url.getHost())
                || "geocoding-api.open-meteo.com".equals(url.getHost()))) throw new IllegalArgumentException("Host");
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        synchronized (this) {
            if (closed || ticket != (search ? searchGeneration : generation)) {
                c.disconnect(); throw new IllegalStateException("Superseded");
            }
            connection = c; connectionIsSearch = search;
        }
        try {
            c.setInstanceFollowRedirects(false); c.setConnectTimeout(6000); c.setReadTimeout(6000);
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP response");
            if (c.getContentLengthLong() > MAX_RESPONSE) throw new IllegalStateException("Response size");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096]; int count;
                long deadline = android.os.SystemClock.elapsedRealtime() + 12000;
                while ((count = in.read(buf)) != -1) {
                    if (closed || Thread.currentThread().isInterrupted()
                            || android.os.SystemClock.elapsedRealtime() > deadline
                            || out.size() + count > MAX_RESPONSE) throw new IllegalStateException("Response limit");
                    out.write(buf, 0, count);
                }
                return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally { c.disconnect(); synchronized (this) { if (connection == c) { connection = null; connectionIsSearch = false; } } }
    }
    private static int requiredInt(JSONObject object, String key) throws Exception {
        double value = object.getDouble(key);
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Integer field");
        return (int)value;
    }
    private static long solarTime(JSONObject daily, String field) {
        try { return seconds(daily.getJSONArray(field).getDouble(0)); }
        catch (Exception ignored) { return 0; } // Polar day/night: current weather remains useful.
    }
    private static long seconds(double value) {
        if (!Double.isFinite(value) || value < 1 || value > 32503680000L) throw new IllegalArgumentException("Time");
        return (long)(value * 1000);
    }
    private static WeatherState decode(JSONObject o) throws Exception {
        return new WeatherState(o.getDouble("temperature"), o.getDouble("wind"), requiredInt(o,"code"),
                requiredInt(o,"cloud"), o.getLong("observed"), o.getLong("sunrise"), o.getLong("sunset"), o.getLong("fetched"), true,
                o.optInt("offset", Integer.MIN_VALUE), o.optInt("is_day", -1));
    }
    private static JSONObject encode(WeatherState w) {
        JSONObject o = new JSONObject();
        try { o.put("temperature",w.temperature).put("wind",w.wind).put("code",w.code).put("cloud",w.cloud)
                .put("observed",w.observedAt).put("sunrise",w.sunrise).put("sunset",w.sunset).put("fetched",w.fetchedAt).put("offset",w.utcOffsetSeconds).put("is_day",w.isDay);
        } catch (Exception ignored) { }
        return o;
    }
}
