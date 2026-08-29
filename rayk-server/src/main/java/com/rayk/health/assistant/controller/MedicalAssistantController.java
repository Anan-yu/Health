package com.rayk.health.assistant.controller;

import com.rayk.health.assistant.application.MedicalAssistantApplicationService;
import com.rayk.health.assistant.dto.CreateMedicalAssistantConversationRequest;
import com.rayk.health.assistant.dto.SendMedicalAssistantMessageRequest;
import com.rayk.health.assistant.vo.MedicalAssistantConversationVo;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/client/medical-assistant")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class MedicalAssistantController {
    private final MedicalAssistantApplicationService service;

    public MedicalAssistantController(MedicalAssistantApplicationService service) {
        this.service = service;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<MedicalAssistantConversationVo>> conversations() {
        return ApiResponse.success(service.listConversations());
    }

    @PostMapping("/conversations")
    public ApiResponse<MedicalAssistantConversationVo> create(
            @Valid @RequestBody(required = false) CreateMedicalAssistantConversationRequest request) {
        return ApiResponse.success(service.createConversation(request));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<MedicalAssistantConversationVo> conversation(
            @PathVariable long conversationId) {
        return ApiResponse.success(service.getConversation(conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable long conversationId) {
        service.deleteConversation(conversationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<MedicalAssistantConversationVo> message(
            @PathVariable long conversationId,
            @Valid @RequestBody SendMedicalAssistantMessageRequest request) {
        return ApiResponse.success(service.sendMessage(conversationId, request));
    }

    @PostMapping(
            value = "/conversations/{conversationId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable long conversationId,
            @Valid @RequestBody SendMedicalAssistantMessageRequest request,
            HttpServletResponse response) {
        // Prepare the stream before changing the response content type. The
        // preparation step validates the customer's entitlement and may throw
        // a BusinessException (for example, an exhausted assistant quota).
        try {
            SseEmitter emitter = service.streamMessage(conversationId, request);
            configureStreamResponse(response);
            return emitter;
        } catch (BusinessException exception) {
            // Entitlement and other business checks run before the asynchronous
            // stream starts.  Return the same SSE envelope the client already
            // knows how to parse instead of letting content negotiation turn a
            // useful error into a generic "unable to answer" message.
            configureStreamResponse(response);
            SseEmitter emitter = new SseEmitter();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "error");
            payload.put("code", exception.getErrorCode().code());
            payload.put("message", exception.getErrorCode().message());
            try {
                emitter.send(SseEmitter.event().data(payload));
            } catch (Exception sendException) {
                emitter.completeWithError(sendException);
                return emitter;
            }
            emitter.complete();
            return emitter;
        }
    }

    private void configureStreamResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
    }
}
