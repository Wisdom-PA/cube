package wisdom.cube.gateway;

import wisdom.cube.logging.InMemoryBehaviourLogStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HttpServerGatewayTest {

    private HttpServerGateway gateway;

    /**
     * Stops the server between tests. Invoked by JUnit 5, not by other code in this class.
     */
    @AfterEach
    @SuppressWarnings("unused")
    void tearDown() {
        if (gateway != null) {
            gateway.stop();
        }
    }

    @Test
    void startAndStop() {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        gateway.stop();
        gateway.stop();
    }

    @Test
    void doubleStartIsNoOp() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        gateway.start();
        assertEquals(port, gateway.getPort());
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void getStatusReturns200AndJson() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"version\""));
        assertTrue(body.contains("\"ready\""));
        assertTrue(body.contains("true"));
        assertTrue(body.contains("paranoid"));
        assertTrue(body.contains("\"global_offline\""));
        assertTrue(body.contains("false"));
    }

    @Test
    void getConfigReturnsDeviceJson() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mock Cube"));
    }

    @Test
    void patchConfigUpdatesFields() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        String patch = "{\"device_name\":\"Kitchen Cube\",\"default_privacy_mode\":\"normal\"}";
        HttpRequest patchReq = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString(patch))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> patched = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patched.statusCode());
        assertTrue(patched.body().contains("Kitchen Cube"));
        assertTrue(patched.body().contains("normal"));

        HttpRequest statusReq = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .GET()
            .build();
        HttpResponse<String> status = client.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(status.body().contains("normal"));
    }

    @Test
    void getDevicesRoutinesProfilesAndLogsReturnJson() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        for (String path : new String[] {"/devices", "/routines", "/profiles", "/logs", "/internet-activity"}) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().startsWith("{"), path);
        }
        HttpRequest devices = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices"))
            .GET()
            .build();
        HttpResponse<String> devRes = client.send(devices, HttpResponse.BodyHandlers.ofString());
        assertTrue(devRes.body().contains("\"power\""));
    }

    @Test
    void postBackupReturnsOctetStream() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/backup"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("octet-stream"));
        assertArrayEquals("WISDOM-BACKUP-v0\n".getBytes(StandardCharsets.UTF_8), response.body());
    }

    @Test
    void postRestoreReturns200() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/restore"))
            .POST(HttpRequest.BodyPublishers.ofString("blob"))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void statusPostReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void devicesPostReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void backupGetReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/backup"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void restoreGetReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/restore"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void configPutReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void patchConfigWithEmptyBodyStillReturns200() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mock Cube"));
    }

    @Test
    void patchDeviceUpdatesState() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/light-2"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"power\":true,\"brightness\":0.25}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"power\":true"));
        assertTrue(response.body().contains("0.25"));
    }

    @Test
    void getLogsIncludesChainAfterDevicePatch() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/light-1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"power\":false}"))
            .header("Content-Type", "application/json")
            .build();
        assertEquals(200, client.send(patch, HttpResponse.BodyHandlers.ofString()).statusCode());
        HttpRequest logs = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/logs"))
            .GET()
            .build();
        HttpResponse<String> logRes = client.send(logs, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, logRes.statusCode());
        assertTrue(logRes.body().contains("\"chains\":["));
        assertTrue(logRes.body().contains("\"chain_id\":"));
        assertTrue(logRes.body().contains("Device control (companion app)"));
    }

    @Test
    void patchUnknownDeviceDoesNotAppendLogChain() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/unknown-id"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        assertEquals(404, client.send(patch, HttpResponse.BodyHandlers.ofString()).statusCode());
        HttpRequest logs = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/logs"))
            .GET()
            .build();
        assertEquals("{\"chains\":[]}", client.send(logs, HttpResponse.BodyHandlers.ofString()).body());
    }

    @Test
    void postChatAppendsBehaviourLogChain() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/chat"))
            .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hi\"}"))
            .header("Content-Type", "application/json")
            .build();
        assertEquals(200, client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
        HttpRequest logs = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/logs"))
            .GET()
            .build();
        String body = client.send(logs, HttpResponse.BodyHandlers.ofString()).body();
        assertTrue(body.contains("\"chains\":["));
        assertTrue(body.contains("hi"));
    }

    @Test
    void patchUnknownDeviceReturns404() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/unknown"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("DEVICE_NOT_FOUND"));
        assertTrue(response.body().contains("Unknown device"));
    }

    @Test
    void postDevicesDiscoverReturnsCompletePayload() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/discover"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"status\":\"complete\""));
        assertTrue(body.contains("\"added\":0"));
        assertTrue(body.contains("\"devices\":["));
        assertTrue(body.contains("light-1"));
    }

    @Test
    void postChatReturnsReply() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/chat"))
            .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hello\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"reply\""));
        assertTrue(response.body().contains("on_device"));
    }

    @Test
    void postChatWithoutMessageReturns400() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/chat"))
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    void getPortReturnsZeroBeforeStart() {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        assertEquals(0, gateway.getPort());
    }

    @Test
    void startWithNullExecutorStillServesRequests() throws Exception {
        gateway = new HttpServerGateway(0, null);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void chatGetReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/chat"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void internetActivityPostReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/internet-activity"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void getSingleDeviceReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/light-1"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void patchDevicesCollectionReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void patchDevicePathWithExtraSegmentReturns404() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/a/b"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void patchConfigGlobalOfflineReflectedInStatus() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"global_offline\":true}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> patched = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patched.statusCode());
        assertTrue(patched.body().contains("\"global_offline\":true"));
        HttpRequest statusReq = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/status"))
            .GET()
            .build();
        HttpResponse<String> status = client.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(status.body().contains("\"global_offline\":true"));
    }

    @Test
    void postChatUnescapesMessageContent() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"message\":\"say \\\"hi\\\"\"}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/chat"))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("On-device (stub):"));
        assertTrue(response.body().contains("hi"));
    }
}
