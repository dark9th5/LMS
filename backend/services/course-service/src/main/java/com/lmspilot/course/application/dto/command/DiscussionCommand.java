package com.lmspilot.course.application.dto.command;

import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import java.util.UUID;

public final class DiscussionCommand {
    private DiscussionCommand() {
    }

    public record CreateThread(String title, UUID lessonId, String content) {
    }

    public record CreatePost(UUID parentPostId, String content) {
    }

    public record ModerateThread(DiscussionThreadStatus status, Boolean pinned) {
    }
}
