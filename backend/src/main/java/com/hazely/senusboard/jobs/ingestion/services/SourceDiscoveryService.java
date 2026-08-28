package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.jobs.ingestion.IngestionProperties;
import com.hazely.senusboard.jobs.ingestion.dtos.DownloadedDocument;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers downloadable source documents and stores them in the configured local directory.
 *
 * <p>Discovery supports ordinary HTML links and the immutable release manifest used by the
 * configured investor-relations site. Downloads are restricted to the source host, and existing
 * files are reused by name.</p>
 */
public class SourceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SourceDiscoveryService.class);
    private static final Set<String> EXT = Set.of(
            "csv", "doc", "docx", "ods", "odt", "pdf", "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "zip"
    );
    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);

    private final IngestionProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public SourceDiscoveryService(IngestionProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Discovers and downloads every public document exposed by the source page.
     */
    public List<DownloadedDocument> fetch() throws IOException, InterruptedException {
        URI source = requireSource(props.getSourceUrl());
        LinkedHashSet<URI> urls = new LinkedHashSet<>();
        try {
            urls.addAll(findHtml(source));
        } catch (IOException ex) {
            // Continue because the immutable manifest may still expose all documents.
            log.warn("Could not inspect source HTML: {}", ex.getMessage());
        }
        try {
            urls.addAll(findManifest(source));
        } catch (IOException ex) {
            // Continue because direct HTML links may already contain downloadable documents.
            log.warn("Could not inspect source manifest: {}", ex.getMessage());
        }

        Path dir = props.getDocumentDir().toAbsolutePath().normalize();
        Files.createDirectories(dir);

        List<DownloadedDocument> files = new ArrayList<>();
        for (URI url : urls) {
            try {
                files.add(download(source, url, dir));
            } catch (IOException ex) {
                // Continue so one unavailable document does not block the remaining downloads.
                log.warn("Could not download source document {}: {}", url, ex.getMessage());
            }
        }
        return List.copyOf(files);
    }

    private List<URI> findHtml(URI source) throws IOException, InterruptedException {
        HttpResponse<InputStream> res = send(source);
        requireOk(res, "source page");
        String type = mediaType(res);
        if (!type.equals("text/html")) {
            res.body().close();
            return List.of();
        }

        LinkedHashSet<URI> urls = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            new ParserDelegator().parse(reader, new HTMLEditorKit.ParserCallback() {
                @Override
                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    collectLink(source, tag, attrs, urls);
                }

                @Override
                public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    collectLink(source, tag, attrs, urls);
                }
            }, true);
        }
        return List.copyOf(urls);
    }

    private void collectLink(URI source, HTML.Tag tag, MutableAttributeSet attrs, Set<URI> urls) {
        if (tag != HTML.Tag.A) {
            return;
        }
        Object href = attrs.getAttribute(HTML.Attribute.HREF);
        if (href == null) {
            return;
        }
        URI url = source.resolve(href.toString());
        if (isAllowed(source, url) && isFile(url.getPath())) {
            urls.add(url);
        }
    }

    private List<URI> findManifest(URI source) throws IOException, InterruptedException {
        String slug = slug(source);
        if (slug == null) {
            return List.of();
        }

        URI currentUrl = source.resolve("/sites/" + slug + "/current.json");
        HttpResponse<InputStream> currentRes = send(currentUrl);
        if (currentRes.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            currentRes.body().close();
            return List.of();
        }
        requireOk(currentRes, "release pointer");

        JsonNode current;
        try (InputStream body = currentRes.body()) {
            current = mapper.readTree(body);
        }
        String sitePath = current.path("sitePath").asText("");
        if (sitePath.isBlank() || sitePath.startsWith("/") || sitePath.contains("..")) {
            throw new IOException("The release pointer contains an invalid sitePath");
        }

        URI siteUrl = currentUrl.resolve(sitePath);
        if (!isAllowed(source, siteUrl)) {
            throw new IOException("The release manifest must use the source host");
        }

        HttpResponse<InputStream> siteRes = send(siteUrl);
        requireOk(siteRes, "release manifest");
        JsonNode site;
        try (InputStream body = siteRes.body()) {
            site = mapper.readTree(body);
        }

        LinkedHashSet<URI> urls = new LinkedHashSet<>();
        collectManifest(source, siteUrl, site.path("documents"), urls);
        return List.copyOf(urls);
    }

    private void collectManifest(URI source, URI base, JsonNode node, Set<URI> urls) {
        if (node.isArray()) {
            node.forEach(item -> collectManifest(source, base, item, urls));
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String raw = node.path("url").asText("");
        if (!raw.isBlank()) {
            URI url = base.resolve(raw);
            if (isAllowed(source, url)) {
                urls.add(url);
            }
        }
        node.properties().forEach(field -> collectManifest(source, base, field.getValue(), urls));
    }

    private DownloadedDocument download(URI source, URI url, Path dir)
            throws IOException, InterruptedException {
        if (!isAllowed(source, url)) {
            throw new IOException("A document URL left the source host");
        }
        DownloadedDocument existing = findExisting(url, dir);
        if (existing != null) {
            return existing;
        }

        HttpResponse<InputStream> res = send(url);
        requireOk(res, "document");
        if (!isAllowed(source, res.uri())) {
            res.body().close();
            throw new IOException("A document redirect left the source host");
        }

        long size = res.headers().firstValueAsLong("content-length").orElse(-1);
        if (size > props.getMaxFileBytes()) {
            res.body().close();
            throw new IOException("A source document exceeds the configured size limit");
        }
        String type = mediaType(res);
        if (type.equals("text/html") || type.equals("application/json")) {
            res.body().close();
            throw new IOException("A document URL returned non-file content: " + type);
        }

        String name = fileName(res.uri(), type);
        Path target;
        try {
            target = target(dir, name);
        } catch (IOException ex) {
            res.body().close();
            throw ex;
        }
        DownloadedDocument redirected = reuse(target, res.uri(), type);
        if (redirected != null) {
            res.body().close();
            return redirected;
        }

        Path temp = Files.createTempFile(dir, ".download-", ".tmp");
        try (InputStream body = new BufferedInputStream(res.body())) {
            copy(body, temp, props.getMaxFileBytes());
            move(temp, target);
            return new DownloadedDocument(target, res.uri(), type);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    DownloadedDocument findExisting(URI url, Path dir) throws IOException {
        if (!isFile(url.getPath())) {
            return null;
        }
        Path target = target(dir, fileName(url, "application/octet-stream"));
        return reuse(target, url, null);
    }

    private DownloadedDocument reuse(Path target, URI url, String fallbackType) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("An existing document path is not a regular file: " + target.getFileName());
        }
        String type = Files.probeContentType(target);
        if (type == null || type.isBlank()) {
            type = fallbackType == null || fallbackType.isBlank()
                    ? "application/octet-stream"
                    : fallbackType;
        }
        log.info("Reusing existing source document {}", target.getFileName());
        return new DownloadedDocument(target, url, type);
    }

    private Path target(Path dir, String name) throws IOException {
        Path target = dir.resolve(name).normalize();
        if (!target.getParent().equals(dir)) {
            throw new IOException("The source document has an invalid file name");
        }
        return target;
    }

    private HttpResponse<InputStream> send(URI url) throws IOException, InterruptedException {
        URI current = url;
        for (int count = 0; count <= 5; count++) {
            if (!isAllowed(props.getSourceUrl(), current)) {
                throw new IOException("A request left the configured source host");
            }
            HttpRequest req = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(45))
                    .header("Accept", "text/html,application/json,application/pdf,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("User-Agent", "SenusBoard-Ingestion/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (!REDIRECTS.contains(res.statusCode())) {
                return res;
            }
            String location = res.headers().firstValue("location")
                    .orElseThrow(() -> new IOException("A redirect response has no location"));
            res.body().close();
            current = current.resolve(location);
        }
        throw new IOException("A request exceeded the redirect limit");
    }

    private void requireOk(HttpResponse<InputStream> res, String label) throws IOException {
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            return;
        }
        res.body().close();
        throw new IOException("Failed to load " + label + ": HTTP " + res.statusCode());
    }

    private void copy(InputStream in, Path target, long max) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        int read;
        try (var out = Files.newOutputStream(target)) {
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > max) {
                    throw new IOException("A source document exceeds the configured size limit");
                }
                out.write(buf, 0, read);
            }
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private URI requireSource(URI source) {
        if (!"https".equalsIgnoreCase(source.getScheme()) || source.getHost() == null) {
            throw new IllegalArgumentException("The ingestion source must be an HTTPS URL");
        }
        return source;
    }

    private boolean isAllowed(URI source, URI url) {
        return "https".equalsIgnoreCase(url.getScheme())
                && source.getHost().equalsIgnoreCase(url.getHost())
                && effectivePort(source) == effectivePort(url);
    }

    private int effectivePort(URI uri) {
        return uri.getPort() < 0 ? 443 : uri.getPort();
    }

    private String slug(URI source) {
        String[] parts = source.getPath().split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("[a-z0-9]+(?:[-_][a-z0-9]+)*")) {
                return parts[i];
            }
        }
        return null;
    }

    private boolean isFile(String value) {
        if (value == null) {
            return false;
        }
        String path = value.split("[?#]", 2)[0];
        int dot = path.lastIndexOf('.');
        return dot >= 0 && EXT.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private String mediaType(HttpResponse<?> res) {
        return res.headers().firstValue("content-type")
                .orElse("")
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String fileName(URI url, String type) {
        String path = url.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank()) {
            name = "document";
        }
        if (!isFile(name)) {
            name += extension(type);
        }
        return name;
    }

    private String extension(String type) {
        return switch (type) {
            case "application/pdf" -> ".pdf";
            case "text/csv", "application/csv" -> ".csv";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/rtf" -> ".rtf";
            case "application/vnd.oasis.opendocument.spreadsheet" -> ".ods";
            case "application/vnd.oasis.opendocument.text" -> ".odt";
            case "application/zip" -> ".zip";
            case "text/plain" -> ".txt";
            default -> ".bin";
        };
    }
}
