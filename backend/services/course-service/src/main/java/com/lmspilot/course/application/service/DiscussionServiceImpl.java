package com.lmspilot.course.application.service;

import com.lmspilot.course.application.dto.command.DiscussionCommand;
import com.lmspilot.course.application.dto.response.DiscussionPostResponse;
import com.lmspilot.course.application.dto.response.DiscussionThreadResponse;
import com.lmspilot.course.application.interfaces.repository.IDiscussionPostRepository;
import com.lmspilot.course.application.interfaces.repository.IDiscussionThreadRepository;
import com.lmspilot.course.application.interfaces.service.IDiscussionService;
import com.lmspilot.course.domain.model.DiscussionPost;
import com.lmspilot.course.domain.enums.DiscussionPostStatus;
import com.lmspilot.course.domain.model.DiscussionThread;
import com.lmspilot.course.domain.enums.DiscussionThreadStatus;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiscussionServiceImpl implements IDiscussionService {
    // Application layer orchestrates discussion use cases; persistence details stay
    // behind repository interfaces.
    private final IDiscussionThreadRepository threads;
    private final IDiscussionPostRepository posts;

    public DiscussionServiceImpl(IDiscussionThreadRepository threads, IDiscussionPostRepository posts) {
        this.threads = threads;
        this.posts = posts;
    }

    @Override
    public List<DiscussionThreadResponse> threads(UUID courseId) {
        return threads
                .findAllByCourseIdAndStatusNotOrderByPinnedDescUpdatedAtDesc(courseId, DiscussionThreadStatus.HIDDEN)
                .stream()
                .map(thread -> view(thread, List.of()))
                .toList();
    }

    @Override
    public DiscussionThreadResponse createThread(UUID courseId, DiscussionCommand.CreateThread in) {
        DiscussionThread thread = new DiscussionThread();
        thread.courseId = courseId;
        thread.lessonId = in.lessonId();
        thread.title = in.title().trim();
        thread.authorId = currentUser();
        threads.save(thread);
        reply(thread.id, new DiscussionCommand.CreatePost(null, in.content()));
        return thread(thread.id);
    }

    @Override
    public DiscussionThreadResponse thread(UUID id) {
        DiscussionThread thread = requireThread(id);
        List<DiscussionPostResponse> visiblePosts = posts
                .findAllByThreadIdAndStatusNotOrderByCreatedAtAsc(id, DiscussionPostStatus.DELETED)
                .stream()
                .map(this::post)
                .toList();
        return view(thread, visiblePosts);
    }

    @Override
    public DiscussionPostResponse reply(UUID id, DiscussionCommand.CreatePost in) {
        DiscussionThread thread = requireThread(id);
        if (thread.status != DiscussionThreadStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "DISCUSSION_LOCKED", "Chu de da khoa");
        }
        DiscussionPost post = new DiscussionPost();
        post.threadId = id;
        post.authorId = currentUser();
        post.parentPostId = in.parentPostId();
        post.content = in.content().trim();
        posts.save(post);
        thread.postCount++;
        thread.updatedAt = Instant.now();
        threads.save(thread);
        return post(post);
    }

    @Override
    public DiscussionThreadResponse moderateThread(UUID id, DiscussionCommand.ModerateThread in) {
        DiscussionThread thread = requireThread(id);
        if (in.status() != null) {
            thread.status = in.status();
        }
        if (in.pinned() != null) {
            thread.pinned = in.pinned();
        }
        thread.updatedAt = Instant.now();
        return view(threads.save(thread), List.of());
    }

    @Override
    public void deletePost(UUID id) {
        DiscussionPost post = posts.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DISCUSSION_POST_NOT_FOUND",
                        "Khong tim thay bai viet"));
        post.status = DiscussionPostStatus.DELETED;
        post.content = "";
        post.updatedAt = Instant.now();
        posts.save(post);
    }

    private DiscussionThread requireThread(UUID id) {
        return threads.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DISCUSSION_THREAD_NOT_FOUND",
                        "Khong tim thay chu de"));
    }

    private UUID currentUser() {
        try {
            return CurrentUser.id();
        } catch (Exception e) {
            return new UUID(0, 1);
        }
    }

    private DiscussionPostResponse post(DiscussionPost post) {
        return new DiscussionPostResponse(post.id, post.authorId, post.parentPostId, post.content, post.status,
                post.createdAt);
    }

    private DiscussionThreadResponse view(DiscussionThread thread, List<DiscussionPostResponse> postViews) {
        return new DiscussionThreadResponse(thread.id, thread.courseId, thread.lessonId, thread.title, thread.authorId,
                thread.status, thread.pinned, thread.postCount, thread.createdAt, thread.updatedAt, postViews);
    }
}
