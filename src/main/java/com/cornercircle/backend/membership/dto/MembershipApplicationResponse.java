package com.cornercircle.backend.membership.dto;

import com.cornercircle.backend.membership.model.MembershipStatus;
import java.util.UUID;

public record MembershipApplicationResponse(UUID applicationId, MembershipStatus status) {}
