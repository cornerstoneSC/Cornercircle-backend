package com.cornercircle.backend.membership.dto;

import java.util.List;

public record AdminMembersResponse(AdminMemberSummary summary, List<AdminMemberResponse> members) {}
