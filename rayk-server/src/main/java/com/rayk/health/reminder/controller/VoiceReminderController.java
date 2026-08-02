package com.rayk.health.reminder.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.reminder.application.VoiceReminderService;
import com.rayk.health.reminder.dto.UpdateVoiceReminderSettingRequest;
import com.rayk.health.reminder.dto.VoiceReminderPreviewRequest;
import com.rayk.health.reminder.vo.VoiceReminderPreviewVo;
import com.rayk.health.reminder.vo.VoiceReminderSettingVo;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/voice-reminders")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class VoiceReminderController {
    private final VoiceReminderService service;

    public VoiceReminderController(VoiceReminderService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    public ApiResponse<VoiceReminderSettingVo> settings() {
        return ApiResponse.success(service.getSetting());
    }

    @PutMapping("/settings")
    public ApiResponse<VoiceReminderSettingVo> updateSettings(
            @Valid @RequestBody UpdateVoiceReminderSettingRequest request) {
        return ApiResponse.success(service.updateSetting(request));
    }

    @PostMapping("/preview")
    public ApiResponse<VoiceReminderPreviewVo> preview(
            @Valid @RequestBody VoiceReminderPreviewRequest request) {
        return ApiResponse.success(service.preview(request.type()));
    }

    @GetMapping("/audio/{id}/content")
    public ResponseEntity<InputStreamResource> audio(@PathVariable long id) {
        VoiceReminderService.DownloadedAudio audio = service.openAudio(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(audio.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(new InputStreamResource(audio.inputStream()));
    }
}
