package com.rayk.health.system.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.system.application.PrivacyConsentService;
import com.rayk.health.system.entity.PrivacyConsentEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/consents")
public class PrivacyConsentController {
    private final PrivacyConsentService consentService;

    public PrivacyConsentController(PrivacyConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    public ApiResponse<PrivacyConsentEntity> grant(
            @PathVariable long patientId, @Valid @RequestBody GrantConsentRequest request) {
        PrivacyConsentEntity entity =
                consentService.grantConsent(patientId, request.consentType(), request.policyVersion());
        return ApiResponse.success(entity);
    }

    @DeleteMapping("/{type}")
    public ApiResponse<Void> revoke(@PathVariable long patientId, @PathVariable String type) {
        consentService.revokeConsent(patientId, type);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<PrivacyConsentEntity>> list(@PathVariable long patientId) {
        return ApiResponse.success(consentService.listConsents(patientId));
    }

    public record GrantConsentRequest(
            @NotBlank
                    @Pattern(
                            regexp =
                                    "DATA_COLLECTION|HEALTH_ASSESSMENT|DATA_SHARING|MARKETING")
                    String consentType,
            @NotBlank @Size(max = 30) String policyVersion) {}
}
