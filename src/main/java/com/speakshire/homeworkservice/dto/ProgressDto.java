package com.speakshire.homeworkservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;

public record ProgressDto(
        @Min(0) @Max(100) Integer progressPct,
        Map<String, Object> meta
) {}