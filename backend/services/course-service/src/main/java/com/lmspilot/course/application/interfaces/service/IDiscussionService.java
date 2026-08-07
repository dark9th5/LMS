package com.lmspilot.course.application.interfaces.service;

import com.lmspilot.course.application.dto.command.DiscussionCommand;
import com.lmspilot.course.application.dto.response.DiscussionPostResponse;
import com.lmspilot.course.application.dto.response.DiscussionThreadResponse;
import java.util.List;
import java.util.UUID;

public interface IDiscussionService {
    List<DiscussionThreadResponse> threads(UUID courseId);
    DiscussionThreadResponse createThread(UUID courseId, DiscussionCommand.CreateThread command);
    DiscussionThreadResponse thread(UUID id);
    DiscussionPostResponse reply(UUID id, DiscussionCommand.CreatePost command);
    DiscussionThreadResponse moderateThread(UUID id, DiscussionCommand.ModerateThread command);
    void deletePost(UUID id);
}
