package com.aimentor.external.speech;

import com.aimentor.domain.voice.exception.VoiceErrorCode;
import com.aimentor.domain.voice.exception.VoiceException;
import com.aimentor.external.ai.AiServerProperties;
import com.aimentor.external.speech.dto.SpeechToTextRequest;
import com.aimentor.external.speech.dto.SpeechToTextResponse;
import com.aimentor.external.speech.dto.TextToSpeechRequest;
import com.aimentor.external.speech.dto.TextToSpeechResponse;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class StubSpeechIntegrationService implements SpeechIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(StubSpeechIntegrationService.class);
    private static final String PROVIDER_PYTHON = "python";
    private static final String PROVIDER_MOCK = "mock-speech";
    private static final String PROVIDER_STUB = "stub-speech";
    private static final String MIME_AUDIO_WAV = "audio/wav";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000;
    private static final int SAMPLE_RATE = 16000;
    private static final double DURATION_SECONDS = 0.35;
    private static final double TONE_FREQUENCY_HZ = 440.0;
    private static final double AMPLITUDE = 0.25;

    private final SpeechIntegrationProperties properties;
    private final AiServerProperties aiServerProperties;
    private final RestTemplate restTemplate;

    public StubSpeechIntegrationService(
            SpeechIntegrationProperties properties,
            AiServerProperties aiServerProperties
    ) {
        this.properties = properties;
        this.aiServerProperties = aiServerProperties;
        this.restTemplate = createRestTemplate(properties);
    }

    @PostConstruct
    void logProviderSelection() {
        String configuredProvider = properties.provider();
        String resolvedProvider = resolveProviderName();
        log.info(
                "[Voice][Config] integration.speech.provider(raw)={}, resolved={}, aiServerUrl={}",
                configuredProvider,
                resolvedProvider,
                aiServerProperties.url()
        );
    }

    @Override
    public SpeechToTextResponse speechToText(SpeechToTextRequest request) {
        long startedAt = System.currentTimeMillis();
        if (request == null || !StringUtils.hasText(request.audioUrl())) {
            VoiceException ex = new VoiceException(VoiceErrorCode.VOICE_STT_EMPTY_AUDIO);
            logSttFailure(resolveProviderName(), -1L, startedAt, ex);
            throw ex;
        }
        String providerName = resolveProviderName();
        long audioSizeBytes = estimateAudioSizeBytes(request.audioUrl());
        try {
            SpeechToTextResponse response;
            if (PROVIDER_PYTHON.equalsIgnoreCase(providerName)) {
                response = requestPythonStt(request);
            } else {
                response = new SpeechToTextResponse(
                        "Speech-to-text stub mode is enabled.",
                        providerName,
                        true
                );
            }
            logSttSuccess(providerName, audioSizeBytes, startedAt);
            return response;
        } catch (VoiceException ex) {
            logSttFailure(providerName, audioSizeBytes, startedAt, ex);
            throw ex;
        } catch (RuntimeException ex) {
            VoiceException wrapped = new VoiceException(
                    VoiceErrorCode.VOICE_STT_PROVIDER_FAILED,
                    ex.getClass().getSimpleName()
            );
            logSttFailure(providerName, audioSizeBytes, startedAt, wrapped);
            throw wrapped;
        }
    }

    @Override
    public TextToSpeechResponse textToSpeech(TextToSpeechRequest request) {
        String providerName = resolveProviderName();
        if (PROVIDER_PYTHON.equalsIgnoreCase(providerName)) {
            return requestPythonTts(request);
        }
        return new TextToSpeechResponse(
                buildStubAudioDataUrl(),
                providerName,
                true
        );
    }

    private TextToSpeechResponse requestPythonTts(TextToSpeechRequest request) {
        try {
            ResponseEntity<PythonTextToSpeechResponse> response = restTemplate.exchange(
                    aiServerProperties.url() + "/tts",
                    HttpMethod.POST,
                    new HttpEntity<>(new PythonTextToSpeechRequest(
                            request.text(),
                            request.voiceName(),
                            request.languageCode()
                    )),
                    PythonTextToSpeechResponse.class
            );
            PythonTextToSpeechResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.audioUrl())) {
                throw new VoiceException(VoiceErrorCode.VOICE_TTS_INVALID_RESPONSE);
            }
            return new TextToSpeechResponse(
                    body.audioUrl(),
                    StringUtils.hasText(body.providerName()) ? body.providerName() : "python",
                    false
            );
        } catch (RestClientException ex) {
            throw new VoiceException(
                    VoiceErrorCode.VOICE_TTS_PROVIDER_FAILED,
                    buildRestClientDetails(ex)
            );
        }
    }

    private SpeechToTextResponse requestPythonStt(SpeechToTextRequest request) {
        try {
            DecodedAudio decodedAudio = decodeDataUrl(request.audioUrl());
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("audio", new ByteArrayResource(decodedAudio.bytes()) {
                @Override
                public String getFilename() {
                    return decodedAudio.filename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<PythonSpeechToTextResponse> response = restTemplate.exchange(
                    aiServerProperties.url() + "/stt",
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    PythonSpeechToTextResponse.class
            );
            PythonSpeechToTextResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.text())) {
                throw new VoiceException(VoiceErrorCode.VOICE_STT_EMPTY_TRANSCRIPT);
            }
            return new SpeechToTextResponse(
                    body.text(),
                    "python",
                    false
            );
        } catch (VoiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new VoiceException(
                    VoiceErrorCode.VOICE_STT_PROVIDER_FAILED,
                    buildRestClientDetails(ex)
            );
        } catch (IllegalArgumentException ex) {
            throw new VoiceException(
                    VoiceErrorCode.VOICE_STT_PROVIDER_FAILED,
                    "INVALID_AUDIO_DATA_URL"
            );
        }
    }

    private DecodedAudio decodeDataUrl(String audioUrl) {
        int commaIndex = audioUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("Invalid data URL.");
        }

        String meta = audioUrl.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        String payload = audioUrl.substring(commaIndex + 1);
        if (!meta.startsWith("data:") || !meta.contains(";base64")) {
            throw new IllegalArgumentException("Unsupported data URL format.");
        }

        String mimeType = meta.substring(5, meta.indexOf(';'));
        byte[] decodedBytes = Base64.getDecoder().decode(payload);
        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("Audio payload is empty.");
        }

        String extension = switch (mimeType) {
            case "audio/webm" -> ".webm";
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/mpeg", "audio/mp3" -> ".mp3";
            case "audio/mp4", "audio/x-m4a" -> ".m4a";
            default -> ".webm";
        };

        return new DecodedAudio(decodedBytes, "recording" + extension);
    }

    private String buildStubAudioDataUrl() {
        return "data:" + MIME_AUDIO_WAV + ";base64," + Base64.getEncoder().encodeToString(buildToneWavBytes());
    }

    private byte[] buildToneWavBytes() {
        int sampleCount = (int) (SAMPLE_RATE * DURATION_SECONDS);
        int dataSize = sampleCount * 2;
        int byteRate = SAMPLE_RATE * 2;
        int chunkSize = 36 + dataSize;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(44 + dataSize);
            DataOutputStream dataStream = new DataOutputStream(outputStream);

            writeAscii(dataStream, "RIFF");
            writeIntLE(dataStream, chunkSize);
            writeAscii(dataStream, "WAVE");
            writeAscii(dataStream, "fmt ");
            writeIntLE(dataStream, 16);
            writeShortLE(dataStream, (short) 1);
            writeShortLE(dataStream, (short) 1);
            writeIntLE(dataStream, SAMPLE_RATE);
            writeIntLE(dataStream, byteRate);
            writeShortLE(dataStream, (short) 2);
            writeShortLE(dataStream, (short) 16);
            writeAscii(dataStream, "data");
            writeIntLE(dataStream, dataSize);

            for (int index = 0; index < sampleCount; index++) {
                double t = (double) index / SAMPLE_RATE;
                short sample = (short) (Math.sin(2 * Math.PI * TONE_FREQUENCY_HZ * t) * Short.MAX_VALUE * AMPLITUDE);
                writeShortLE(dataStream, sample);
            }

            dataStream.flush();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate stub audio bytes.", ex);
        }
    }

    private void writeAscii(DataOutputStream dataStream, String value) throws IOException {
        dataStream.writeBytes(value);
    }

    private void writeIntLE(DataOutputStream dataStream, int value) throws IOException {
        dataStream.writeByte(value & 0xFF);
        dataStream.writeByte((value >> 8) & 0xFF);
        dataStream.writeByte((value >> 16) & 0xFF);
        dataStream.writeByte((value >> 24) & 0xFF);
    }

    private void writeShortLE(DataOutputStream dataStream, short value) throws IOException {
        dataStream.writeByte(value & 0xFF);
        dataStream.writeByte((value >> 8) & 0xFF);
    }

    private String resolveProviderName() {
        if (!StringUtils.hasText(properties.provider())) {
            return PROVIDER_PYTHON;
        }
        String normalized = properties.provider().trim().toLowerCase(Locale.ROOT);
        if (PROVIDER_PYTHON.equals(normalized) || PROVIDER_MOCK.equals(normalized) || PROVIDER_STUB.equals(normalized)) {
            return normalized;
        }
        log.warn("[Voice][Config] Unknown integration.speech.provider='{}'. Falling back to '{}'.", properties.provider(), PROVIDER_PYTHON);
        return PROVIDER_PYTHON;
    }

    private RestTemplate createRestTemplate(SpeechIntegrationProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                props.connectTimeoutMs() != null ? props.connectTimeoutMs() : DEFAULT_CONNECT_TIMEOUT_MS
        );
        requestFactory.setReadTimeout(
                props.readTimeoutMs() != null ? props.readTimeoutMs() : DEFAULT_READ_TIMEOUT_MS
        );
        return new RestTemplate(requestFactory);
    }

    private String buildRestClientDetails(RestClientException ex) {
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause != null && rootCause != ex) {
            return ex.getClass().getSimpleName() + ":" + rootCause.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName();
    }

    private long estimateAudioSizeBytes(String audioUrl) {
        try {
            int commaIndex = audioUrl.indexOf(',');
            if (commaIndex < 0) {
                return -1L;
            }
            String payload = audioUrl.substring(commaIndex + 1);
            return Base64.getDecoder().decode(payload).length;
        } catch (IllegalArgumentException ex) {
            return -1L;
        }
    }

    private void logSttSuccess(String providerName, long audioSizeBytes, long startedAt) {
        log.info(
                "[Voice][STT] providerName={}, audioSizeBytes={}, latencyMs={}, success={}",
                providerName,
                audioSizeBytes,
                System.currentTimeMillis() - startedAt,
                true
        );
    }

    private void logSttFailure(String providerName, long audioSizeBytes, long startedAt, VoiceException ex) {
        log.warn(
                "[Voice][STT] providerName={}, audioSizeBytes={}, latencyMs={}, success={}, errorCode={}",
                providerName,
                audioSizeBytes,
                System.currentTimeMillis() - startedAt,
                false,
                ex.getErrorCode()
        );
    }

    private record PythonTextToSpeechRequest(
            String text,
            String voiceName,
            String languageCode
    ) {
    }

    private record PythonTextToSpeechResponse(
            String audioUrl,
            String providerName
    ) {
    }

    private record PythonSpeechToTextResponse(
            String text
    ) {
    }

    private record DecodedAudio(
            byte[] bytes,
            String filename
    ) {
    }
}
