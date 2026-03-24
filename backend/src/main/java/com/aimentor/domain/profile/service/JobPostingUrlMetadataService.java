package com.aimentor.domain.profile.service;

import com.aimentor.domain.profile.dto.response.JobPostingUrlPreviewResponse;
import com.aimentor.external.ai.AiServerProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class JobPostingUrlMetadataService {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_TITLE_PATTERN = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_DESCRIPTION_PATTERN = Pattern.compile("<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_DESCRIPTION_PATTERN = Pattern.compile("<meta[^>]*name=[\"']description[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CHARSET_IN_CONTENT_TYPE_PATTERN = Pattern.compile("charset=([a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_CHARSET_PATTERN = Pattern.compile("<meta[^>]*charset=[\"']?([a-zA-Z0-9_\\-]+)[\"']?[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_HTTP_EQUIV_CHARSET_PATTERN = Pattern.compile("<meta[^>]*http-equiv=[\"']content-type[\"'][^>]*content=[\"'][^\"']*charset=([a-zA-Z0-9_\\-]+)[^\"']*[\"'][^>]*>", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final RestTemplate restTemplate;
    private final AiServerProperties aiServerProperties;

    public JobPostingUrlMetadataService(AiServerProperties aiServerProperties) {
        this.restTemplate = new RestTemplate();
        this.aiServerProperties = aiServerProperties;
    }

    public JobPostingUrlPreviewResponse preview(String rawUrl) {
        String normalizedUrl = rawUrl == null ? "" : rawUrl.trim();
        URI uri = URI.create(normalizedUrl);
        String siteName = resolveSiteName(uri.getHost());

        JobPostingUrlPreviewResponse aiPreview = fetchFromAiServer(normalizedUrl, siteName);
        if (aiPreview != null) {
            return aiPreview;
        }

        return previewWithHttpFallback(normalizedUrl, siteName, uri);
    }

    public String resolveSiteName(String host) {
        if (!StringUtils.hasText(host)) {
            return "기타";
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.contains("jobkorea")) {
            return "잡코리아";
        }
        if (normalizedHost.contains("saramin")) {
            return "사람인";
        }
        if (normalizedHost.contains("wanted")) {
            return "원티드";
        }
        if (normalizedHost.contains("jumpit")) {
            return "점핏";
        }
        return "기타";
    }

    private JobPostingUrlPreviewResponse fetchFromAiServer(String url, String fallbackSiteName) {
        if (!StringUtils.hasText(aiServerProperties.url())) {
            return null;
        }

        try {
            ResponseEntity<CrawlPreviewResponse> response = restTemplate.postForEntity(
                    aiServerProperties.url() + "/crawl/job-posting-preview",
                    new CrawlPreviewRequest(url),
                    CrawlPreviewResponse.class
            );
            CrawlPreviewResponse body = response.getBody();
            if (body == null) {
                return null;
            }

            String resolvedSiteName = StringUtils.hasText(body.siteName()) ? body.siteName() : fallbackSiteName;
            String resolvedUrl = StringUtils.hasText(body.jobUrl()) ? body.jobUrl() : url;

            return new JobPostingUrlPreviewResponse(
                    resolvedSiteName,
                    resolvedUrl,
                    trimToNull(body.companyName()),
                    trimToNull(body.positionTitle()),
                    trimToNull(body.description()),
                    trimToNull(body.deadline()),
                    body.extracted(),
                    trimToNull(body.failureReason())
            );
        } catch (RestClientException ex) {
            return null;
        }
    }

    private JobPostingUrlPreviewResponse previewWithHttpFallback(String normalizedUrl, String siteName, URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String html = decodeHtml(response);
            String title = firstNonBlank(extract(OG_TITLE_PATTERN, html), extract(TITLE_PATTERN, html));
            String description = firstNonBlank(extract(OG_DESCRIPTION_PATTERN, html), extract(META_DESCRIPTION_PATTERN, html));

            return new JobPostingUrlPreviewResponse(
                    siteName,
                    normalizedUrl,
                    null,
                    title,
                    description,
                    null,
                    StringUtils.hasText(title) || StringUtils.hasText(description),
                    null
            );
        } catch (Exception ex) {
            return new JobPostingUrlPreviewResponse(
                    siteName,
                    normalizedUrl,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "URL 메타데이터를 자동으로 읽지 못했습니다. 직접 입력해 주세요."
            );
        }
    }

    private String extract(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String decodeHtml(HttpResponse<byte[]> response) {
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            return "";
        }

        String asciiView = new String(body, StandardCharsets.ISO_8859_1);
        Optional<Charset> responseHeaderCharset = response.headers()
                .firstValue("content-type")
                .flatMap(this::extractCharsetFromContentType);
        Optional<Charset> metaCharset = extractCharsetFromMeta(asciiView);

        Charset[] candidates = new Charset[]{
                responseHeaderCharset.orElse(null),
                metaCharset.orElse(null),
                StandardCharsets.UTF_8,
                Charset.forName("EUC-KR"),
                Charset.forName("MS949")
        };

        for (Charset charset : candidates) {
            if (charset == null) {
                continue;
            }
            try {
                return new String(body, charset);
            } catch (Exception ignored) {
                // try next charset
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private Optional<Charset> extractCharsetFromContentType(String contentType) {
        Matcher matcher = CHARSET_IN_CONTENT_TYPE_PATTERN.matcher(contentType);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return toCharset(matcher.group(1));
    }

    private Optional<Charset> extractCharsetFromMeta(String htmlAsciiView) {
        Matcher charsetMatcher = META_CHARSET_PATTERN.matcher(htmlAsciiView);
        if (charsetMatcher.find()) {
            return toCharset(charsetMatcher.group(1));
        }

        Matcher httpEquivMatcher = META_HTTP_EQUIV_CHARSET_PATTERN.matcher(htmlAsciiView);
        if (httpEquivMatcher.find()) {
            return toCharset(httpEquivMatcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<Charset> toCharset(String charsetName) {
        if (!StringUtils.hasText(charsetName)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(charsetName.trim()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private record CrawlPreviewRequest(
            String url
    ) {
    }

    private record CrawlPreviewResponse(
            String siteName,
            String jobUrl,
            String companyName,
            String positionTitle,
            String description,
            String deadline,
            boolean extracted,
            String failureReason
    ) {
    }
}
