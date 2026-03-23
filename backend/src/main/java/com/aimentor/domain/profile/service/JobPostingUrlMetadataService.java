package com.aimentor.domain.profile.service;

import com.aimentor.domain.profile.dto.response.JobPostingUrlPreviewResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobPostingUrlMetadataService {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_TITLE_PATTERN = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_DESCRIPTION_PATTERN = Pattern.compile("<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_DESCRIPTION_PATTERN = Pattern.compile("<meta[^>]*name=[\"']description[\"'][^>]*content=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public JobPostingUrlPreviewResponse preview(String rawUrl) {
        URI uri = URI.create(rawUrl.trim());
        String siteName = resolveSiteName(uri.getHost());

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();
            String title = firstNonBlank(extract(OG_TITLE_PATTERN, html), extract(TITLE_PATTERN, html));
            String description = firstNonBlank(extract(OG_DESCRIPTION_PATTERN, html), extract(META_DESCRIPTION_PATTERN, html));

            return new JobPostingUrlPreviewResponse(
                    siteName,
                    rawUrl.trim(),
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
                    rawUrl.trim(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    "URL 메타정보를 자동으로 읽지 못했습니다. 수동으로 내용을 입력해 주세요."
            );
        }
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
        return "기타";
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
}
