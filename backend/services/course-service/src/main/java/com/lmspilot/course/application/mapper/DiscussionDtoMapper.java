package com.lmspilot.course.application.mapper;

import com.lmspilot.course.application.dto.command.DiscussionCommand;
import com.lmspilot.course.application.dto.request.CreateDiscussionPostRequest;
import com.lmspilot.course.application.dto.request.CreateDiscussionThreadRequest;
import com.lmspilot.course.application.dto.request.ModerateDiscussionThreadRequest;
import org.springframework.stereotype.Component;

@Component
public class DiscussionDtoMapper {
    public DiscussionCommand.CreateThread toCreateThreadCommand(CreateDiscussionThreadRequest request) {
        return new DiscussionCommand.CreateThread(request.title(), request.lessonId(), request.content());
    }

    public DiscussionCommand.CreatePost toCreatePostCommand(CreateDiscussionPostRequest request) {
        return new DiscussionCommand.CreatePost(request.parentPostId(), request.content());
    }

    public DiscussionCommand.ModerateThread toModerateThreadCommand(ModerateDiscussionThreadRequest request) {
        return new DiscussionCommand.ModerateThread(request.status(), request.pinned());
    }
}
