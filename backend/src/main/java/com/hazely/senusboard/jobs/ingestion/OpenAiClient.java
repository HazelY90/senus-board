package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.jobs.ingestion.dtos.AiAnalyticsResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AnalyticsDataset;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.core.JacksonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Calls OpenAI Files and Responses APIs for structured document extraction.
 *
 * <p>Uploaded files are deleted after each response attempt. API keys and extracted source
 * content are never written to application logs.</p>
 */
public class OpenAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final URI DEFAULT_FILES_URL = URI.create("https://api.openai.com/v1/files");
    private static final URI DEFAULT_RESPONSES_URL = URI.create("https://api.openai.com/v1/responses");
    private static final String PROMPT_PATH = "ai/extraction-prompt.txt";
    private static final String SCHEMA_PATH = "ai/extraction-schema.json";
    private static final String ANALYTICS_PROMPT_PATH = "ai/analytics-prompt.txt";
    private static final String ANALYTICS_SCHEMA_PATH = "ai/analytics-schema.json";
    private static final long RETRY_DELAY_MILLIS = 500;

    private final OpenAiProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI filesUrl;
    private final URI responsesUrl;
    private final String prompt;
    private final JsonNode schema;
    private final String analyticsPrompt;
    private final JsonNode analyticsSchema;

    public OpenAiClient(OpenAiProperties props, ObjectMapper mapper) throws IOException {
        this(
                props,
                mapper,
                createHttpClient(),
                DEFAULT_FILES_URL,
                DEFAULT_RESPONSES_URL,
                readText(PROMPT_PATH),
                mapper.readTree(readText(SCHEMA_PATH)),
                readText(ANALYTICS_PROMPT_PATH),
                mapper.readTree(readText(ANALYTICS_SCHEMA_PATH))
        );
    }

    OpenAiClient(
            OpenAiProperties props,
            ObjectMapper mapper,
            HttpClient http,
            URI filesUrl,
            URI responsesUrl,
            String prompt,
            JsonNode schema
    ) {
        this(props, mapper, http, filesUrl, responsesUrl, prompt, schema, prompt, schema);
    }

    OpenAiClient(
            OpenAiProperties props,
            ObjectMapper mapper,
            HttpClient http,
            URI filesUrl,
            URI responsesUrl,
            String prompt,
            JsonNode schema,
            String analyticsPrompt,
            JsonNode analyticsSchema
    ) {
        this.props = props;
        this.mapper = mapper;
        this.http = http;
        this.filesUrl = filesUrl;
        this.responsesUrl = responsesUrl;
        this.prompt = prompt;
        this.schema = schema;
        this.analyticsPrompt = analyticsPrompt;
        this.analyticsSchema = analyticsSchema;
    }

    @Override
    public AiExtractionResult extract(Path file) throws IOException, InterruptedException {
        Path source = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new IOException("Source document is not a regular file: " + source.getFileName());
        }

        String fileId = upload(source);
        try {
            JsonNode response = createExtractionResponse(fileId, source.getFileName().toString());
            String output = outputText(response);
            return mapper.readerFor(AiExtractionResult.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(output);
        } finally {
            delete(fileId);
        }
    }

    @Override
    public AiAnalyticsResult analyze(AnalyticsDataset data) throws IOException, InterruptedException {
        if (data == null || data.periods() == null) {
            throw new IllegalArgumentException("analytics dataset is required");
        }
        ObjectNode body = baseBody(analyticsPrompt);
        ObjectNode message = body.putArray("input").addObject();
        message.put("role", "user");
        message.putArray("content").addObject()
                .put("type", "input_text")
                .put("text", mapper.writeValueAsString(data));
        addFormat(body, "period_analytics", analyticsSchema);
        JsonNode response = sendResponse(body, "structured analytics");
        return mapper.readerFor(AiAnalyticsResult.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(outputText(response));
    }

    private String upload(Path file) throws IOException, InterruptedException {
        String boundary = "----SenusBoard" + UUID.randomUUID();
        String name = safeName(file.getFileName().toString());
        String type = Files.probeContentType(file);
        if (type == null || type.isBlank()) {
            type = "application/octet-stream";
        }

        byte[] head = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n"
                + "user_data\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + name + "\"\r\n"
                + "Content-Type: " + type + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        HttpRequest req = request(filesUrl)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(head),
                        HttpRequest.BodyPublishers.ofFile(file),
                        HttpRequest.BodyPublishers.ofByteArray(tail)
                ))
                .build();
        HttpResponse<String> res = send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode body = requireOk(res, "file upload");
        String id = body.path("id").asText("");
        if (id.isBlank()) {
            throw new IOException("OpenAI file upload returned no file id");
        }
        return id;
    }

    private JsonNode createExtractionResponse(String fileId, String name)
            throws IOException, InterruptedException {
        ObjectNode body = baseBody(prompt);

        ObjectNode message = body.putArray("input").addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject()
                .put("type", "input_text")
                .put("text", "Extract the stable category values from " + name + ".");
        content.addObject()
                .put("type", "input_file")
                .put("file_id", fileId);

        addFormat(body, "metric_extraction", schema);
        return sendResponse(body, "structured extraction");
    }

    private ObjectNode baseBody(String instructions) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.getModel());
        body.put("instructions", instructions);
        body.put("store", false);
        body.put("max_output_tokens", props.getMaxOutputTokens());
        body.put("truncation", "disabled");
        return body;
    }

    private void addFormat(ObjectNode body, String name, JsonNode formatSchema) {
        ObjectNode format = body.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", name);
        format.put("strict", true);
        format.set("schema", formatSchema.deepCopy());
    }

    private JsonNode sendResponse(ObjectNode body, String action)
            throws IOException, InterruptedException {
        HttpRequest req = request(responsesUrl)
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = send(req, HttpResponse.BodyHandlers.ofString());
        return requireOk(res, action);
    }

    private String outputText(JsonNode response) throws IOException {
        String status = response.path("status").asText("");
        if (!status.equals("completed")) {
            String reason = response.path("incomplete_details").path("reason").asText(status);
            throw new IOException("OpenAI response did not complete: " + reason);
        }

        for (JsonNode item : response.path("output")) {
            for (JsonNode content : item.path("content")) {
                String type = content.path("type").asText("");
                if (type.equals("refusal")) {
                    throw new IOException("OpenAI refused the extraction request");
                }
                if (type.equals("output_text")) {
                    String text = content.path("text").asText("");
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        throw new IOException("OpenAI response contained no structured output");
    }

    private void delete(String fileId) {
        try {
            URI url = URI.create(filesUrl + "/" + fileId);
            HttpRequest req = request(url)
                    .timeout(Duration.ofSeconds(30))
                    .DELETE()
                    .build();
            HttpResponse<Void> res = send(req, HttpResponse.BodyHandlers.discarding());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("Could not delete uploaded OpenAI file: HTTP {}", res.statusCode());
            }
        } catch (IOException ex) {
            log.warn("Could not delete uploaded OpenAI file: {}", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("OpenAI file deletion was interrupted");
        }
    }

    private HttpRequest.Builder request(URI url) {
        return HttpRequest.newBuilder(url)
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Accept", "application/json");
    }

    private <T> HttpResponse<T> send(
            HttpRequest req,
            HttpResponse.BodyHandler<T> handler
    ) throws IOException, InterruptedException {
        for (int attempt = 0; ; attempt++) {
            try {
                return http.send(req, handler);
            } catch (IOException ex) {
                if (attempt >= props.getMaxRetries()) {
                    throw ex;
                }
                long delay = RETRY_DELAY_MILLIS << attempt;
                log.warn(
                        "OpenAI transport failed with {}; retrying in {} ms ({}/{})",
                        ex.getClass().getSimpleName(),
                        delay,
                        attempt + 1,
                        props.getMaxRetries()
                );
                Thread.sleep(delay);
            }
        }
    }

    private JsonNode requireOk(HttpResponse<String> res, String action) throws IOException {
        JsonNode body;
        try {
            body = mapper.readTree(res.body());
        } catch (JacksonException ex) {
            throw new IOException("OpenAI " + action + " returned invalid JSON", ex);
        }
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            return body;
        }
        String message = body.path("error").path("message").asText("HTTP " + res.statusCode());
        throw new IOException("OpenAI " + action + " failed: " + message);
    }

    private static String readText(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    private static HttpClient createHttpClient() {
        SSLParameters ssl = new SSLParameters();
        ssl.setProtocols(new String[]{"TLSv1.2"});
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .version(HttpClient.Version.HTTP_1_1)
                .sslParameters(ssl)
                .build();
    }

    private String safeName(String name) {
        return name.replaceAll("[\\r\\n\\\"\\\\/]", "_");
    }
}
