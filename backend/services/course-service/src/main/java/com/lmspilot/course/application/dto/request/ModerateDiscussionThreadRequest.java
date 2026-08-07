package com.lmspilot.course.application.dto.request;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;

public record ModerateDiscussionThreadRequest(DiscussionThreadStatus status, Boolean pinned) {
}
