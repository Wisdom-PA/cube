package wisdom.cube.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServerGatewayTest {

    private HttpServerGateway gateway;

    @AfterEach
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
        for (String path : new String[] {"/devices", "/routines", "/profiles", "/logs"}) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().startsWith("{"), path);
        }
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
}
