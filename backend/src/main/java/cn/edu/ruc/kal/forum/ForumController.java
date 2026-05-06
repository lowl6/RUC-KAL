package cn.edu.ruc.kal.forum;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.common.PageResult;
import cn.edu.ruc.kal.security.AuthPrincipal;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ForumController {

    private final ForumPostRepository repo;
    private final ForumCommentRepository commentRepo;
    private final PostLikeRepository likeRepo;
    private final UserRepository userRepo;

    /* =============== 列表 =============== */

    @GetMapping("/public/forum/posts")
    public ApiResponse<PageResult<ForumPost>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = repo.search(keyword, topic, ForumPost.Status.published,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 50)));
        return ApiResponse.ok(PageResult.of(p));
    }

    /* =============== 详情：文 + 评论 + 是否已点赞 =============== */

    @GetMapping("/public/forum/posts/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> get(@PathVariable("id") String id) {
        var p = repo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        if (p.getStatus() == ForumPost.Status.deleted || p.getStatus() == ForumPost.Status.hidden)
            throw new BizException(404, "帖子不存在");
        p.setViewCount(p.getViewCount() == null ? 1 : p.getViewCount() + 1);
        repo.save(p);

        if (p.getAuthorName() == null && p.getAuthorId() != null) {
            userRepo.findById(p.getAuthorId()).ifPresent(u -> p.setAuthorName(u.getDisplayName()));
        }

        var comments = commentRepo.findByPostIdAndStatusOrderByCreatedAtAsc(id, "published");

        AuthPrincipal me = CurrentUser.getOrNull();
        boolean liked = me != null && likeRepo.existsByPostIdAndUserId(id, me.getUserId());

        Map<String, Object> out = new HashMap<>();
        out.put("post",     p);
        out.put("comments", comments);
        out.put("liked",    liked);
        return ApiResponse.ok(out);
    }

    /* =============== 创建帖子 =============== */

    @PostMapping("/forum/posts")
    public ApiResponse<ForumPost> create(@RequestBody PostReq req) {
        String uid = CurrentUser.requireUserId();
        String name = userRepo.findById(uid).map(User::getDisplayName).orElse(null);
        ForumPost p = ForumPost.builder()
                .postId("fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .title(req.getTitle())
                .content(req.getContent())
                .excerpt(req.getContent() == null ? null
                        : req.getContent().substring(0, Math.min(120, req.getContent().length())))
                .topic(req.getTopic())
                .authorId(uid)
                .authorName(name)
                .pinned(false).essence(false)
                .viewCount(0).replyCount(0).likeCount(0)
                .status(ForumPost.Status.published)
                .createdAt(LocalDateTime.now())
                .lastReplyAt(LocalDateTime.now())
                .build();
        repo.save(p);
        return ApiResponse.ok(p);
    }

    /* =============== 评论 =============== */

    @PostMapping("/forum/posts/{id}/comments")
    @Transactional
    public ApiResponse<ForumComment> addComment(@PathVariable("id") String id, @RequestBody CommentReq req) {
        var post = repo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        if (post.getStatus() != ForumPost.Status.published) throw new BizException("帖子已被关闭");

        String uid = CurrentUser.requireUserId();
        String name = userRepo.findById(uid).map(User::getDisplayName).orElse(null);

        var c = ForumComment.builder()
                .postId(id)
                .authorId(uid)
                .authorName(name)
                .content(req.getContent())
                .likeCount(0)
                .status("published")
                .createdAt(LocalDateTime.now())
                .build();
        commentRepo.save(c);

        post.setReplyCount((post.getReplyCount() == null ? 0 : post.getReplyCount()) + 1);
        post.setLastReplyAt(LocalDateTime.now());
        repo.save(post);

        return ApiResponse.ok(c);
    }

    /* =============== 点赞（toggle） =============== */

    @PostMapping("/forum/posts/{id}/like")
    @Transactional
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable("id") String id) {
        var post = repo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        String uid = CurrentUser.requireUserId();

        boolean now;
        if (likeRepo.existsByPostIdAndUserId(id, uid)) {
            likeRepo.deleteByPostIdAndUserId(id, uid);
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) - 1));
            now = false;
        } else {
            likeRepo.save(PostLike.builder()
                    .postId(id).userId(uid).createdAt(LocalDateTime.now()).build());
            post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
            now = true;
        }
        repo.save(post);

        Map<String, Object> out = new HashMap<>();
        out.put("liked",     now);
        out.put("likeCount", post.getLikeCount());
        return ApiResponse.ok(out);
    }

    @Data public static class PostReq {
        @NotBlank private String title;
        @NotBlank private String content;
        private String topic;
    }

    @Data public static class CommentReq {
        @NotBlank private String content;
    }
}
