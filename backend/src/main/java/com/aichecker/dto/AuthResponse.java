package com.aichecker.dto;

public record AuthResponse(String token, UserSummaryDto user) {
}
