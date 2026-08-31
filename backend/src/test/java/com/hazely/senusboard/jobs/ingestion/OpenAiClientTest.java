package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientTest {

    @TempDir
    Path dir;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void extractsStructuredResultAndDeletesUpload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> uploadBody = new AtomicReference<>();
        AtomicReference<JsonNode> responseBody = new AtomicReference<>();
        AtomicBoolean isDeleted = new AtomicBoolean();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/files", exchange -> handleFiles(exchange, uploadBody, isDeleted));
        server.createContext("/v1/responses", exchange -> handleResponse(exchange, mapper, responseBody));
        server.start();

        int port = server.getAddress().getPort();
        URI filesUrl = URI.create("http://localhost:" + port + "/v1/files");
        URI responsesUrl = URI.create("http://localhost:" + port + "/v1/responses");
        OpenAiProperties props = new OpenAiProperties();
        props.setApiKey("test-key");
        props.setModel("test-model");
        JsonNode schema = mapper.readTree("{\"type\":\"object\"}");
        OpenAiClient client = new OpenAiClient(
                props,
                mapper,
                HttpClient.newHttpClient(),
                filesUrl,
                responsesUrl,
                "Test extraction rules",
                schema
        );
        Path file = dir.resolve("report.pdf");
        Files.writeString(file, "test document", StandardCharsets.UTF_8);

        AiExtractionResult result = client.extract(file);

        assertThat(result.periods()).hasSize(1);
        assertThat(result.aiSummary()).contains("FY2025 annual results");
        assertThat(result.periods().getFirst().growth().revenue()).isEqualByComparingTo("836991");
        assertThat(uploadBody.get()).contains("name=\"purpose\"").contains("user_data");
        assertThat(uploadBody.get()).contains("filename=\"report.pdf\"").contains("test document");
        assertThat(responseBody.get().path("instructions").asText()).isEqualTo("Test extraction rules");
        assertThat(responseBody.get().path("input").get(0).path("content").get(1).path("file_id").asText())
                .isEqualTo("file-test");
        assertThat(responseBody.get().path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(responseBody.get().path("text").path("format").path("schema")).isEqualTo(schema);
        assertThat(isDeleted).isTrue();
    }

    private void handleFiles(
            HttpExchange exchange,
            AtomicReference<String> uploadBody,
            AtomicBoolean isDeleted
    ) throws IOException {
        if (exchange.getRequestMethod().equals("DELETE")) {
            isDeleted.set(true);
            send(exchange, 200, "{\"deleted\":true}");
            return;
        }
        uploadBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        send(exchange, 200, "{\"id\":\"file-test\"}");
    }

    private void handleResponse(
            HttpExchange exchange,
            ObjectMapper mapper,
            AtomicReference<JsonNode> responseBody
    ) throws IOException {
        responseBody.set(mapper.readTree(exchange.getRequestBody()));
        String output = """
                {
                  "publicationDate": "2025-11-19",
                  "aiSummary": "FY2025 annual results with FY2024 comparative values.",
                  "periods": [{
                    "startDate": "2024-07-01",
                    "endDate": "2025-06-30",
                    "growth": {"revenue": 836991},
                    "profitability": null,
                    "liquidity": null,
                    "capital": null
                  }]
                }
                """;
        var root = mapper.createObjectNode();
        root.put("status", "completed");
        var message = root.putArray("output").addObject();
        message.put("type", "message");
        message.putArray("content").addObject()
                .put("type", "output_text")
                .put("text", output);
        send(exchange, 200, mapper.writeValueAsString(root));
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
