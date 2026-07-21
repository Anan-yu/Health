package com.rayk.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplySupportTicketRequest(
        @NotBlank @Size(max = 1000) String reply, @Size(max = 20) String status) {}
