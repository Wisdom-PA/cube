package wisdom.cube.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

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
        gateway.stop(); // no-op second time
    }

    @Test
    void doubleStartIsNoOp() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        gateway.start(); // no-op; same server
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
    }

    @Test
    void otherPathReturns501() throws Exception {
        gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor());
        gateway.start();
        int port = gateway.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/config"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(501, response.statusCode());
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
}
