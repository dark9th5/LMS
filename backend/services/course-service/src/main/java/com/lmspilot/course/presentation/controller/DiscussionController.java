package com.lmspilot.course.presentation.controller;

import com.lmspilot.course.application.dto.request.CreateDiscussionPostRequest;
import com.lmspilot.course.application.dto.request.CreateDiscussionThreadRequest;
import com.lmspilot.course.application.dto.response.DiscussionPostResponse;
import com.lmspilot.course.application.dto.response.DiscussionThreadResponse;
import com.lmspilot.course.application.dto.request.ModerateDiscussionThreadRequest;
import com.lmspilot.course.application.interfaces.service.IDiscussionService;
import com.lmspilot.course.application.mapper.DiscussionDtoMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discussions")
public class DiscussionController {
    private final IDiscussionService service;
    private final DiscussionDtoMapper mapper;

    public DiscussionController(IDiscussionService service, DiscussionDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/courses/{courseId}/threads")
    public List<DiscussionThreadResponse> threads(@PathVariable UUID courseId) {
        return service.threads(courseId);
    }

    @PostMapping("/courses/{courseId}/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscussionThreadResponse create(@PathVariable UUID courseId, @Valid @RequestBody CreateDiscussionThreadRequest in) {
        return service.createThread(courseId, mapper.toCreateThreadCommand(in));
    }

    @GetMapping("/threads/{id}")
    public DiscussionThreadResponse thread(@PathVariable UUID id) {
        return service.thread(id);
    }

    @PostMapping("/threads/{id}/posts")
    public DiscussionPostResponse reply(@PathVariable UUID id, @Valid @RequestBody CreateDiscussionPostRequest in) {
        return service.reply(id, mapper.toCreatePostCommand(in));
    }

    @PatchMapping("/threads/{id}")
    public DiscussionThreadResponse moderate(@PathVariable UUID id, @RequestBody ModerateDiscussionThreadRequest in) {
        return service.moderateThread(id, mapper.toModerateThreadCommand(in));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.deletePost(id);
    }
}
