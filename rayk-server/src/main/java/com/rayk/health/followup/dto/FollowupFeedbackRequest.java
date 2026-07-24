package com.rayk.health.followup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FollowupFeedbackRequest(
        @Size(max = 1000) String feedback,
        @NotEmpty @Size(max = 30) List<@Valid FollowupActionFeedback> actions) {}

