package com.dailymate.lostfound.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
import com.dailymate.lostfound.dto.response.LostItemPostResponse;
import com.dailymate.lostfound.service.LostFoundService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lost-found")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    public LostFoundController(LostFoundService lostFoundService) {
        this.lostFoundService = lostFoundService;
    }

    @GetMapping("/posts")
    public List<LostItemPostResponse> getAllPosts() {
        return lostFoundService.getAllPosts();
    }

    @GetMapping("/my-posts")
    public List<LostItemPostResponse> getMyPosts(@AuthenticationPrincipal UserPrincipal principal) {
        return lostFoundService.getMyPosts(principal.user().getId());
    }

    @PostMapping("/posts")
    public ResponseEntity<LostItemPostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LostItemPostRequest request) {
        LostItemPostResponse response = lostFoundService.createPost(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/posts/{id}")
    public LostItemPostResponse updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody LostItemPostRequest request) {
        return lostFoundService.updatePost(principal.user().getId(), id, request);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        lostFoundService.deletePost(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
