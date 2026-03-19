package com.aimentor.domain.voice.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.domain.voice.dto.request.VoiceTextToSpeechRequest;
import com.aimentor.domain.voice.dto.response.VoiceSpeechToTextResponse;
import com.aimentor.domain.voice.dto.response.VoiceTextToSpeechResponse;
import com.aimentor.domain.voice.service.VoiceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping({"/api", "/api/v1"})
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VoiceSpeechToTextResponse> speechToText(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(value = "languageCode", required = false) String languageCode
    ) {
        return ApiResponse.success(voiceService.speechToText(audio, languageCode));
    }

    @PostMapping("/tts")
    public ApiResponse<VoiceTextToSpeechResponse> textToSpeech(
            @Valid @RequestBody VoiceTextToSpeechRequest request
    ) {
        return ApiResponse.success(voiceService.textToSpeech(request));
    }
}
