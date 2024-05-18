package com.codingchosun.backend.controller;

import com.codingchosun.backend.constants.ExceptionConstants;
import com.codingchosun.backend.domain.Comment;
import com.codingchosun.backend.domain.Post;
import com.codingchosun.backend.domain.User;
import com.codingchosun.backend.exception.LoggedInUserNotFound;
import com.codingchosun.backend.exception.emptyrequest.EmptyCommentException;
import com.codingchosun.backend.exception.notfoundfromdb.PostNotFoundFromDB;
import com.codingchosun.backend.request.RegisterCommentRequest;
import com.codingchosun.backend.response.ApiResponse;
import com.codingchosun.backend.service.CommentService;
import com.codingchosun.backend.service.PostService;
import com.codingchosun.backend.web.argumentresolver.Login;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Long> registerComment(@Login User user, @PathVariable Long postId,
                                             @RequestBody RegisterCommentRequest registerCommentRequest){

        //user 못 가져올경우
        if(user == null){
            throw new LoggedInUserNotFound(ExceptionConstants.LOGGED_IN_USER_NOT_FOUND);
        }

        //빈 댓글 예외처리
        if(registerCommentRequest.getContents() == null){
            throw new EmptyCommentException(ExceptionConstants.EMPTY_COMMENT);
        }

        //포스트 가져오며 예외처리
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new PostNotFoundFromDB("postId: " + postId + "를 찾지 못했습니다"));

        Comment comment = commentService.registerComments(user, post, registerCommentRequest);

        return new ApiResponse<>(HttpStatus.OK, true, comment.getCommentId());
    }
}