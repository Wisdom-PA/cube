package wisdom.cube.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import wisdom.cube.core.ApiGateway;
import wisdom.cube.device.DeviceDiscoveryService;
import wisdom.cube.device.DeviceHealthScheduler;
import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.device.LightDevice;
import wisdom.cube.device.NoOpDeviceDiscoveryService;
import wisdom.cube.logging.InMemoryBehaviourLogStore;
import wisdom.cube.profile.MutableProfileStore;
import wisdom.cube.profile.ProfileEntry;
import wisdom.cube.profile.ProfileStore;
import wisdom.cube.routine.MutableRoutineCatalog;
import wisdom.cube.routine.RoutineCatalog;
import wisdom.cube.routine.RoutineDefinition;
import wisdom.cube.routine.RoutineJson;
import wisdom.cube.routine.RoutineTickScheduler;

/**
 * Skeleton API gateway: JDK {@link HttpServer} implementing paths from {@code openapi/cube-app.yaml}
 * (cube ↔ app contract). In-memory config; static fixture payloads for lists and logs.
 */
public final class HttpServerGateway implements ApiGateway {

    private static final Pattern CHAT_MESSAGE_FIELD = Pattern.compile(
        "\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern ROUTINE_NAME_PATCH_FIELD = Pattern.compile(
        "\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PROFILE_DISPLAY_NAME_PATCH_FIELD = Pattern.compile(
        "\"display_name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final byte[] BACKUP_BYTES = "WISDOM-BACKUP-v0\n".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private final int port;
    private final Executor executor;
    private final Object configLock = new Object();
    private final ConfigBodyParser.MutableConfig config =
        new ConfigBodyParser.MutableConfig("Mock Cube", "paranoid");
    private final DeviceFixtureStore deviceStore;
    private final DeviceDiscoveryService deviceDiscovery;
    private final InMemoryBehaviourLogStore behaviourLog;
    private final long deviceHealthPeriodSeconds;
    private final RoutineCatalog routineCatalog;
    private final long routineTickPeriodSeconds;
    private final ProfileStore profileStore;
    private DeviceHealthScheduler deviceHealthScheduler;
    private RoutineTickScheduler routineTickScheduler;

    public HttpServerGateway(int port, Executor executor) {
        this(port, executor, new InMemoryBehaviourLogStore());
    }

    public HttpServerGateway(int port, Executor executor, InMemoryBehaviourLogStore behaviourLog) {
        this(port, executor, behaviourLog, defaultDeviceStore(), new NoOpDeviceDiscoveryService(), 0L, null, 0L, null);
    }

    /**
     * Shares {@link DeviceFixtureStore} and log with other cube subsystems (e.g. {@link wisdom.cube.voice.VoicePipelineFactory}).
     */
    public HttpServerGateway(int port, Executor executor, InMemoryBehaviourLogStore behaviourLog, DeviceFixtureStore deviceStore) {
        this(port, executor, behaviourLog, deviceStore, new NoOpDeviceDiscoveryService(), 0L, null, 0L, null);
    }

    public HttpServerGateway(
        int port,
        Executor executor,
        InMemoryBehaviourLogStore behaviourLog,
        DeviceFixtureStore deviceStore,
        DeviceDiscoveryService deviceDiscovery
    ) {
        this(port, executor, behaviourLog, deviceStore, deviceDiscovery, 0L, null, 0L, null);
    }

    public HttpServerGateway(
        int port,
        Executor executor,
        InMemoryBehaviourLogStore behaviourLog,
        DeviceFixtureStore deviceStore,
        DeviceDiscoveryService deviceDiscovery,
        long deviceHealthPeriodSeconds
    ) {
        this(port, executor, behaviourLog, deviceStore, deviceDiscovery, deviceHealthPeriodSeconds, null, 0L, null);
    }

    public HttpServerGateway(
        int port,
        Executor executor,
        InMemoryBehaviourLogStore behaviourLog,
        DeviceFixtureStore deviceStore,
        DeviceDiscoveryService deviceDiscovery,
        long deviceHealthPeriodSeconds,
        RoutineCatalog routineCatalog
    ) {
        this(port, executor, behaviourLog, deviceStore, deviceDiscovery, deviceHealthPeriodSeconds, routineCatalog, 0L, null);
    }

    public HttpServerGateway(
        int port,
        Executor executor,
        InMemoryBehaviourLogStore behaviourLog,
        DeviceFixtureStore deviceStore,
        DeviceDiscoveryService deviceDiscovery,
        long deviceHealthPeriodSeconds,
        RoutineCatalog routineCatalog,
        long routineTickPeriodSeconds
    ) {
        this(
            port,
            executor,
            behaviourLog,
            deviceStore,
            deviceDiscovery,
            deviceHealthPeriodSeconds,
            routineCatalog,
            routineTickPeriodSeconds,
            null
        );
    }

    public HttpServerGateway(
        int port,
        Executor executor,
        InMemoryBehaviourLogStore behaviourLog,
        DeviceFixtureStore deviceStore,
        DeviceDiscoveryService deviceDiscovery,
        long deviceHealthPeriodSeconds,
        RoutineCatalog routineCatalog,
        long routineTickPeriodSeconds,
        ProfileStore profileStore
    ) {
        this.port = port;
        this.executor = executor != null ? executor : Runnable::run;
        this.behaviourLog = behaviourLog;
        this.deviceStore = deviceStore;
        this.deviceDiscovery = deviceDiscovery != null ? deviceDiscovery : new NoOpDeviceDiscoveryService();
        this.deviceHealthPeriodSeconds = Math.max(0L, deviceHealthPeriodSeconds);
        this.routineCatalog = routineCatalog != null ? routineCatalog : new MutableRoutineCatalog();
        this.routineTickPeriodSeconds = Math.max(0L, routineTickPeriodSeconds);
        this.profileStore = profileStore != null ? profileStore : new MutableProfileStore();
    }

    public DeviceFixtureStore deviceStore() {
        return deviceStore;
    }

    public InMemoryBehaviourLogStore behaviourLog() {
        return behaviourLog;
    }

    public RoutineCatalog routineCatalog() {
        return routineCatalog;
    }

    public ProfileStore profileStore() {
        return profileStore;
    }

    /** F5.T2.S4 — blocks cloud paths when true. */
    public boolean isGlobalOffline() {
        synchronized (configLock) {
            return config.globalOffline;
        }
    }

    /** {@code paranoid} or {@code normal}; drives cloud allow in {@link wisdom.cube.internet.DefaultInternetAccessGate}. */
    public String getDefaultPrivacyMode() {
        synchronized (configLock) {
            return config.defaultPrivacyMode;
        }
    }

    @Override
    public void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(executor);
            server.createContext("/status", this::handleStatus);
            server.createContext("/config", this::handleConfig);
            server.createContext("/devices", this::handleDevices);
            server.createContext("/routines", this::handleRoutines);
            server.createContext("/profiles", this::handleProfiles);
            server.createContext("/logs", this::handleLogs);
            server.createContext("/backup", this::handleBackup);
            server.createContext("/restore", this::handleRestore);
            server.createContext("/chat", this::handleChat);
            server.createContext("/internet-activity", this::handleInternetActivity);
            server.start();
            if (deviceHealthPeriodSeconds > 0L) {
                deviceHealthScheduler = DeviceHealthScheduler.everySeconds(
                    deviceDiscovery,
                    deviceStore.registry(),
                    deviceHealthPeriodSeconds
                );
                deviceHealthScheduler.start();
            }
            if (routineTickPeriodSeconds > 0L) {
                routineTickScheduler = new RoutineTickScheduler(
                    Clock.systemDefaultZone(),
                    routineCatalog,
                    deviceStore.registry(),
                    behaviourLog,
                    routineTickPeriodSeconds
                );
                routineTickScheduler.start();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start API gateway on port " + port, e);
        }
    }

    @Override
    public void stop() {
        if (routineTickScheduler != null) {
            routineTickScheduler.stop();
            routineTickScheduler = null;
        }
        if (deviceHealthScheduler != null) {
            deviceHealthScheduler.stop();
            deviceHealthScheduler = null;
        }
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * Port the server is bound to (after start). Returns 0 if not started or port was 0 and not yet bound.
     */
    public int getPort() {
        if (server == null) {
            return 0;
        }
        return server.getAddress().getPort();
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        synchronized (configLock) {
            sendJson(exchange, 200, buildStatusJson());
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equals(method)) {
            synchronized (configLock) {
                sendJson(exchange, 200, buildConfigJson());
            }
            return;
        }
        if ("PATCH".equals(method)) {
            String body = readBody(exchange);
            synchronized (configLock) {
                ConfigBodyParser.applyPatch(body, config);
                sendJson(exchange, 200, buildConfigJson());
            }
            return;
        }
        sendResponse(exchange, 405, "Method Not Allowed");
    }

    private void handleDevices(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if ("/devices".equals(path) || "/devices/".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, deviceStore.listJson());
                return;
            }
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if ("/devices/discover".equals(path) || "/devices/discover/".equals(path)) {
            if ("POST".equals(method)) {
                int added = deviceDiscovery.refreshDiscoveries(deviceStore.registry());
                String list = deviceStore.listJson();
                String array = extractJsonArrayAfterKey(list, "devices");
                sendJson(exchange, 200, "{\"status\":\"complete\",\"added\":" + added + ",\"devices\":" + array + "}");
                return;
            }
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!path.startsWith("/devices/")) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        String deviceId = path.substring("/devices/".length());
        if (deviceId.isEmpty() || deviceId.indexOf('/') >= 0) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        if ("PATCH".equals(method)) {
            String body = readBody(exchange);
            if (deviceStore.registry().contains(deviceId)) {
                Optional<LightDevice> cur = deviceStore.registry().get(deviceId);
                if (cur.isPresent() && !cur.get().reachable()) {
                    sendJson(exchange, 503, DeviceApiErrors.deviceUnreachableJson());
                    return;
                }
            }
            String updated = deviceStore.patch(deviceId, body);
            if (updated == null) {
                sendJson(exchange, 404, DeviceApiErrors.deviceNotFoundJson());
                return;
            }
            behaviourLog.recordDevicePatchFromApp(deviceId, body, updated);
            sendJson(exchange, 200, updated);
            return;
        }
        sendResponse(exchange, 405, "Method Not Allowed");
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        String body = readBody(exchange);
        String message = extractChatMessage(body);
        if (message == null) {
            sendJson(exchange, 400, "{\"error\":\"missing or invalid message\"}");
            return;
        }
        String replyText = "On-device (stub): " + message;
        behaviourLog.recordChatFromApp(message, replyText);
        String json = "{\"reply\":\"" + ConfigBodyParser.jsonEscape(replyText) + "\",\"source\":\"on_device\"}";
        sendJson(exchange, 200, json);
    }

    private void handleInternetActivity(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        int limit = parseQueryInt(exchange.getRequestURI().getRawQuery(), "limit", 50, 1, 200);
        sendJson(exchange, 200, behaviourLog.toInternetActivityJson(limit));
    }

    private static String extractChatMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher m = CHAT_MESSAGE_FIELD.matcher(body);
        if (!m.find()) {
            return null;
        }
        return ConfigBodyParser.jsonUnescape(m.group(1));
    }

    private void handleRoutines(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        String path = normalizePath(rawPath);
        String method = exchange.getRequestMethod();
        if ("/routines/history".equals(path)) {
            if ("GET".equals(method)) {
                int limit = parseQueryInt(exchange.getRequestURI().getRawQuery(), "limit", 50, 1, 200);
                sendJson(exchange, 200, behaviourLog.toRoutineRunHistoryJson(limit));
                return;
            }
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if ("/routines".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, routineCatalog.listSummariesJson());
                return;
            }
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!path.startsWith("/routines/")) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        String routineIdRaw = path.substring("/routines/".length());
        if (routineIdRaw.isEmpty() || routineIdRaw.indexOf('/') >= 0) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        final String routineId = URLDecoder.decode(routineIdRaw, StandardCharsets.UTF_8);
        if ("PATCH".equals(method)) {
            String body = readBody(exchange);
            String newName = extractRoutineNamePatch(body);
            if (newName == null || newName.isBlank()) {
                sendJson(exchange, 400, RoutineApiErrors.routinePatchInvalidJson());
                return;
            }
            Optional<RoutineDefinition> updated =
                routineCatalog.patchRoutineDisplayName(routineId, newName.trim());
            if (updated.isPresent()) {
                RoutineDefinition d = updated.get();
                String json = "{\"id\":\"" + RoutineJson.escape(d.routineId()) + "\",\"name\":\""
                    + RoutineJson.escape(d.name()) + "\"}";
                sendJson(exchange, 200, json);
                return;
            }
            boolean exists = routineCatalog.definitions().stream()
                .anyMatch(d -> d.routineId().equals(routineId));
            if (!exists) {
                sendJson(exchange, 404, RoutineApiErrors.routineNotFoundJson());
                return;
            }
            sendJson(exchange, 501, RoutineApiErrors.routinePatchUnsupportedJson());
            return;
        }
        sendResponse(exchange, 405, "Method Not Allowed");
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static int parseQueryInt(String rawQuery, String key, int defaultVal, int min, int max) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Math.min(Math.max(defaultVal, min), max);
        }
        for (String part : rawQuery.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
            if (!key.equals(k)) {
                continue;
            }
            String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
            try {
                int n = Integer.parseInt(v);
                return Math.min(Math.max(n, min), max);
            } catch (NumberFormatException e) {
                return Math.min(Math.max(defaultVal, min), max);
            }
        }
        return Math.min(Math.max(defaultVal, min), max);
    }

    private static String extractRoutineNamePatch(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher m = ROUTINE_NAME_PATCH_FIELD.matcher(body);
        if (!m.find()) {
            return null;
        }
        return ConfigBodyParser.jsonUnescape(m.group(1));
    }

    private void handleProfiles(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        String path = normalizePath(rawPath);
        String method = exchange.getRequestMethod();
        if ("/profiles".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, profileStore.listProfilesJson());
                return;
            }
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!path.startsWith("/profiles/")) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        String profileIdRaw = path.substring("/profiles/".length());
        if (profileIdRaw.isEmpty() || profileIdRaw.indexOf('/') >= 0) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }
        final String profileId = URLDecoder.decode(profileIdRaw, StandardCharsets.UTF_8);
        if ("PATCH".equals(method)) {
            String body = readBody(exchange);
            String newName = extractProfileDisplayNamePatch(body);
            if (newName == null || newName.isBlank()) {
                sendJson(exchange, 400, ProfileApiErrors.profilePatchInvalidJson());
                return;
            }
            Optional<ProfileEntry> updated = profileStore.patchDisplayName(profileId, newName.trim());
            if (updated.isPresent()) {
                sendJson(exchange, 200, MutableProfileStore.toSummaryJson(updated.get()));
                return;
            }
            if (!profileStore.profileExists(profileId)) {
                sendJson(exchange, 404, ProfileApiErrors.profileNotFoundJson());
                return;
            }
            sendJson(exchange, 501, ProfileApiErrors.profilePatchUnsupportedJson());
            return;
        }
        sendResponse(exchange, 405, "Method Not Allowed");
    }

    private static String extractProfileDisplayNamePatch(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher m = PROFILE_DISPLAY_NAME_PATCH_FIELD.matcher(body);
        if (!m.find()) {
            return null;
        }
        return ConfigBodyParser.jsonUnescape(m.group(1));
    }

    private void handleLogs(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJson(exchange, 200, behaviourLog.toLogQueryJson());
    }

    private void handleBackup(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        drainBody(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, BACKUP_BYTES.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(BACKUP_BYTES);
        }
    }

    private void handleRestore(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        drainBody(exchange);
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().close();
    }

    private static void drainBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            if (in != null) {
                in.readAllBytes();
            }
        }
    }

    private String buildStatusJson() {
        return "{\"version\":\"0.1.0\",\"ready\":true,\"privacy_mode\":\"" + config.defaultPrivacyMode
            + "\",\"global_offline\":" + config.globalOffline + "}";
    }

    private String buildConfigJson() {
        return "{\"device_name\":\"" + ConfigBodyParser.jsonEscape(config.deviceName) + "\",\"default_privacy_mode\":\""
            + config.defaultPrivacyMode + "\",\"global_offline\":" + config.globalOffline + "}";
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static DeviceFixtureStore defaultDeviceStore() {
        return new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
    }

    /** Extracts the JSON array value for {@code "key": [...] } from a small object string. */
    static String extractJsonArrayAfterKey(String objectJson, String key) {
        String needle = "\"" + key + "\":";
        int i = objectJson.indexOf(needle);
        if (i < 0) {
            return "[]";
        }
        int bracket = objectJson.indexOf('[', i + needle.length());
        if (bracket < 0) {
            return "[]";
        }
        int depth = 0;
        for (int p = bracket; p < objectJson.length(); p++) {
            char c = objectJson.charAt(p);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return objectJson.substring(bracket, p + 1);
                }
            }
        }
        return "[]";
    }
}
