package wisdom.cube.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.device.NoOpDeviceDiscoveryService;
import wisdom.cube.logging.InMemoryBehaviourLogStore;
import wisdom.cube.profile.FixtureProfileStore;
import wisdom.cube.routine.FixtureRoutineCatalog;
import wisdom.cube.routine.RoutineActionKind;
import wisdom.cube.routine.RoutineDefinition;
import wisdom.cube.routine.RoutineStepResult;

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
        HttpRequest routines = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines"))
            .GET()
            .build();
        HttpResponse<String> routinesRes = client.send(routines, HttpResponse.BodyHandlers.ofString());
        assertTrue(routinesRes.body().contains("Evening lights"));
        assertTrue(routinesRes.body().contains("\"id\":\"r1\""));
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
    void patchUnreachableDeviceReturns503() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        ((InMemoryLightDeviceRegistry) ds.registry()).setReachable("light-1", false);
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log, ds);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/light-1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"power\":false}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("DEVICE_UNREACHABLE"));
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
    void deviceStoreAndBehaviourLogAccessorsReturnInjectedInstances() {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log, ds);
        assertSame(log, gateway.behaviourLog());
        assertSame(ds, gateway.deviceStore());
    }

    @Test
    void fiveArgConstructorUsesNoOpDiscoveryWhenNull() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log, ds, null);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/devices/discover"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        assertEquals(200, client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void startStopWithPeriodicHealthEnabledDoesNotThrow() {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(
            0,
            Executors.newSingleThreadExecutor(),
            log,
            ds,
            new NoOpDeviceDiscoveryService(),
            600L
        );
        gateway.start();
        gateway.stop();
    }

    @Test
    void startStopWithRoutineTickEnabledDoesNotThrow() {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(
            0,
            Executors.newSingleThreadExecutor(),
            log,
            ds,
            new NoOpDeviceDiscoveryService(),
            0L,
            null,
            600L,
            null
        );
        gateway.start();
        gateway.stop();
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
    void accessorsExposeGlobalOfflineAndPrivacyMode() {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        assertFalse(gateway.isGlobalOffline());
        assertEquals("paranoid", gateway.getDefaultPrivacyMode());
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

    @Test
    void getRoutinesHistoryReturnsRunsArray() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log);
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> empty = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/routines/history"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, empty.statusCode());
        assertTrue(empty.body().contains("\"runs\":[]"));

        log.recordRoutineRun(
            new RoutineDefinition("r1", "Evening", "p1", List.of(), List.of(), List.of(), List.of()),
            List.of(
                new RoutineStepResult(0, RoutineActionKind.DEVICE_STATE, true, "ok", null, null)
            )
        );
        HttpResponse<String> withRuns = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/routines/history?limit=5"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, withRuns.statusCode());
        assertTrue(withRuns.body().contains("\"routine_id\":\"r1\""));
        assertTrue(withRuns.body().contains("\"ok\":true"));
    }

    @Test
    void patchRoutineNameUpdatesList() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/r1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"Sunset\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"id\":\"r1\""));
        assertTrue(res.body().contains("Sunset"));
        HttpResponse<String> list = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/routines")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertTrue(list.body().contains("Sunset"));
    }

    @Test
    void patchRoutineUnknownReturns404() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/unknown"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"X\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("ROUTINE_NOT_FOUND"));
    }

    @Test
    void postRoutinesHistoryReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest post = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/history"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        assertEquals(405, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void postRoutinesListReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest post = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        assertEquals(405, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void getRoutinesSubPathWithSlashReturns404() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest get = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/r1/extra"))
            .GET()
            .build();
        assertEquals(404, client.send(get, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void getRoutinesByIdReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest get = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/r1"))
            .GET()
            .build();
        assertEquals(405, client.send(get, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void patchRoutineMissingNameReturns400() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/r1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("ROUTINE_PATCH_INVALID"));
    }

    @Test
    void getRoutinesHistoryToleratesBadLimitQuery() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest get = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/history?limit=not-a-number"))
            .GET()
            .build();
        HttpResponse<String> res = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"runs\""));
    }

    @Test
    void patchRoutineOnFixtureCatalogReturns501() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(
            0,
            Executors.newSingleThreadExecutor(),
            log,
            ds,
            new NoOpDeviceDiscoveryService(),
            0L,
            new FixtureRoutineCatalog(),
            0L,
            null
        );
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/routines/r1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"X\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(501, res.statusCode());
        assertTrue(res.body().contains("ROUTINE_PATCH_UNSUPPORTED"));
    }

    @Test
    void patchProfileDisplayNameUpdatesList() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/profiles/p1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"display_name\":\"Household lead\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Household lead"));
        HttpResponse<String> list = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/profiles")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertTrue(list.body().contains("Household lead"));
    }

    @Test
    void patchProfileUnknownReturns404() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/profiles/unknown"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"display_name\":\"X\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("PROFILE_NOT_FOUND"));
    }

    @Test
    void patchProfileOnFixtureStoreReturns501() throws Exception {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore ds = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        gateway = new HttpServerGateway(
            0,
            Executors.newSingleThreadExecutor(),
            log,
            ds,
            new NoOpDeviceDiscoveryService(),
            0L,
            null,
            0L,
            new FixtureProfileStore()
        );
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest patch = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/profiles/p1"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"display_name\":\"X\"}"))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> res = client.send(patch, HttpResponse.BodyHandlers.ofString());
        assertEquals(501, res.statusCode());
        assertTrue(res.body().contains("PROFILE_PATCH_UNSUPPORTED"));
    }

    @Test
    void postProfilesReturns405() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest post = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/profiles"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        assertEquals(405, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
