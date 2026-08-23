package com.dailymate.lostfound.service;

import com.dailymate.core.dto.response.PageResponse;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.core.util.PaginationUtils;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
import com.dailymate.lostfound.dto.response.LostItemPostResponse;
import com.dailymate.lostfound.entity.LostItemPost;
import com.dailymate.lostfound.repository.LostItemPostRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LostFoundService {

    private final LostItemPostRepository posts;

    public LostFoundService(LostItemPostRepository posts) {
        this.posts = posts;
    }

    public PageResponse<LostItemPostResponse> getAllPosts(String type, String search, int page, int size) {
        String cleanType = (type != null && !type.trim().isBlank() && !type.equalsIgnoreCase("all")) ? type.trim() : null;
        String cleanSearch = (search != null && !search.trim().isBlank()) ? search.trim() : null;
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PaginationUtils.createPageable(page, size, sort);
        Page<LostItemPostResponse> responsePage = posts.findFiltered(cleanType, cleanSearch, pageable)
                .map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    public List<LostItemPostResponse> getAllPosts() {
        return posts.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<LostItemPostResponse> getMyPosts(String userId, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PaginationUtils.createPageable(page, size, sort);
        Page<LostItemPostResponse> responsePage = posts.findByUserId(userId, pageable)
                .map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    public List<LostItemPostResponse> getMyPosts(String userId) {
        return posts.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public LostItemPostResponse createPost(String userId, LostItemPostRequest request) {
        LostItemPost post = new LostItemPost();
        post.setUserId(userId);
        applyChanges(post, request);
        return toResponse(posts.save(post));
    }

    @Transactional
    public LostItemPostResponse updatePost(String userId, String postId, LostItemPostRequest request) {
        LostItemPost post = findPost(userId, postId);
        applyChanges(post, request);
        return toResponse(posts.save(post));
    }

    @Transactional
    public void deletePost(String userId, String postId) {
        LostItemPost post = findPost(userId, postId);
        posts.delete(post);
    }

    private void applyChanges(LostItemPost post, LostItemPostRequest request) {
        post.setTitle(request.title().trim());
        post.setItemType(request.itemType().trim());
        post.setLocation(request.location().trim());
        post.setDescription(request.description().trim());
        post.setContactName(request.contactName().trim());
        post.setContactPhone(request.contactPhone().trim());
    }

    private LostItemPost findPost(String userId, String postId) {
        return posts.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new NotFoundException("Lost and found post not found"));
    }

    private LostItemPostResponse toResponse(LostItemPost post) {
        return new LostItemPostResponse(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                post.getItemType(),
                post.getLocation(),
                post.getDescription(),
                post.getContactName(),
                post.getContactPhone(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
