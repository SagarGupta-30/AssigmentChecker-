package com.aichecker.dto;

import com.aichecker.model.Role;

public record UserSummaryDto(Long id, String name, String email, Role role) {
}
