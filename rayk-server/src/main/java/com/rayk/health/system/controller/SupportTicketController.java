package com.rayk.health.system.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.system.application.SupportTicketService;
import com.rayk.health.system.dto.CreateSupportTicketRequest;
import com.rayk.health.system.dto.ReplySupportTicketRequest;
import com.rayk.health.system.vo.PlatformSupportTicketVo;
import com.rayk.health.system.vo.SupportTicketVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/support-tickets")
public class SupportTicketController {
    private final SupportTicketService ticketService;

    public SupportTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ApiResponse<List<SupportTicketVo>> listMine() {
        return ApiResponse.success(ticketService.listMine());
    }

    @PostMapping
    public ApiResponse<SupportTicketVo> create(
            @Valid @RequestBody CreateSupportTicketRequest request) {
        return ApiResponse.success(ticketService.create(request));
    }

    @GetMapping("/platform")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<PlatformSupportTicketVo>> listForPlatform() {
        return ApiResponse.success(ticketService.listForPlatform());
    }

    @PutMapping("/platform/{id}/reply")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PlatformSupportTicketVo> replyForPlatform(
            @PathVariable long id, @Valid @RequestBody ReplySupportTicketRequest request) {
        return ApiResponse.success(ticketService.replyForPlatform(id, request));
    }
}
