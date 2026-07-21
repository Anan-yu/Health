package com.rayk.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
    @NotBlank @Size(max = 30) String category,
    @NotBlank @Size(max = 1000) String content,
    @Size(max = 100) String contact) {}
