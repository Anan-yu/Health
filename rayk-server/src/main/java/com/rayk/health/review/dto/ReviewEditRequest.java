package com.rayk.health.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReviewEditRequest(
        @NotEmpty @Valid List<ReviewItemEdit> items,
        @Size(max = 1000) String overallOpinion) {

    public record ReviewItemEdit(
            @NotBlank String modelCode,
            @NotBlank @Pattern(regexp = "INSUFFICIENT_DATA|LOW|ATTENTION|HIGH") String riskLevel,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 500) String> evidence,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 500) String> recommendations) {}
}
