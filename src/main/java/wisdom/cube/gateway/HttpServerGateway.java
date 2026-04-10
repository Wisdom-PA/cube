package wisdom.cube.gateway;

import wisdom.cube.core.ApiGateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Skeleton API gateway: JDK {@link HttpServer} implementing paths from {@code openapi/cube-app.yaml}
 * (cube ↔ app contract). In-memory config; static fixture payloads for lists and logs.
 */
public final class HttpServerGateway implements ApiGateway {

    private static final Pattern CHAT_MESSAGE_FIELD = Pattern.compile(
        "\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final String ROUTINES_JSON = "{\"routines\":["
        + "{\"id\":\"r1\",\"name\":\"Evening lights\"},"
        + "{\"id\":\"r2\",\"name\":\"Good morning\"}"
        + "]}";

    private static final String PROFILES_JSON = "{\"profiles\":["
        + "{\"id\":\"p1\",\"role\":\"adult\",\"display_name\":\"Adult\"},"
        + "{\"id\":\"p2\",\"role\":\"guest\",\"display_name\":\"Guest\"}"
        + "]}";

    private static final String LOGS_JSON = "{\"chains\":[]}";

    private static final byte[] BACKUP_BYTES = "WISDOM-BACKUP-v0\n".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private final int port;
    private final Executor executor;
    private final Object configLock = new Object();
    private final ConfigBodyParser.MutableConfig config =
        new ConfigBodyParser.MutableConfig("Mock Cube", "paranoid");
    private final DeviceFixtureStore deviceStore = new DeviceFixtureStore();

    public HttpServerGateway(int port, Executor executor) {
        this.port = port;
        this.executor = executor != null ? executor : Runnable::run;
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
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start API gateway on port " + port, e);
        }
    }

    @Override
    public void stop() {
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
            String updated = deviceStore.patch(deviceId, body);
            if (updated == null) {
                sendJson(exchange, 404, "{\"error\":\"unknown device\"}");
                return;
            }
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
        String json = "{\"reply\":\"" + ConfigBodyParser.jsonEscape(replyText) + "\",\"source\":\"on_device\"}";
        sendJson(exchange, 200, json);
    }

    private void handleInternetActivity(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJson(exchange, 200, "{\"events\":[]}");
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
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJson(exchange, 200, ROUTINES_JSON);
    }

    private void handleProfiles(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJson(exchange, 200, PROFILES_JSON);
    }

    private void handleLogs(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJson(exchange, 200, LOGS_JSON);
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
}
