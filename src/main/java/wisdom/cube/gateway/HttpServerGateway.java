package wisdom.cube.gateway;

import wisdom.cube.core.ApiGateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Skeleton API gateway: HTTP server implementing the cube ↔ app contract (Phase 1.4).
 * Uses JDK HttpServer only; no extra dependencies.
 * Serves GET /status; other routes return 501 until implemented.
 */
public final class HttpServerGateway implements ApiGateway {

    private static final String STATUS_JSON =
        "{\"version\":\"0.1.0\",\"ready\":true,\"privacy_mode\":\"paranoid\"}";

    private HttpServer server;
    private final int port;
    private final Executor executor;

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
            server.createContext("/", this::handleOther);
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
        sendJson(exchange, 200, STATUS_JSON);
    }

    private void handleOther(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 501, "Not Implemented");
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
