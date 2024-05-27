package com.codingchosun.backend.service;


import com.codingchosun.backend.constants.StateCode;
import com.codingchosun.backend.domain.*;
import com.codingchosun.backend.exception.invalidrequest.InvalidEditorException;
import com.codingchosun.backend.exception.invalidtime.TimeBeforeCurrentException;
import com.codingchosun.backend.exception.notfoundfromdb.HashtagNotFoundFromDB;
import com.codingchosun.backend.exception.notfoundfromdb.PostNotFoundFromDB;
import com.codingchosun.backend.repository.hashtagrepository.HashtagRepository;
import com.codingchosun.backend.repository.hashtagrepository.PostHashRepository;
import com.codingchosun.backend.repository.imagerepository.ImageRepository;
import com.codingchosun.backend.repository.postrepository.PostRepository;
import com.codingchosun.backend.repository.postuserrepository.PostUserRepository;
import com.codingchosun.backend.request.PostUpdateRequest;
import com.codingchosun.backend.request.RegisterPostRequest;
import com.codingchosun.backend.response.NoLoginPostsRequest;
import com.codingchosun.backend.response.PostResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final PostUserRepository postUserRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashRepository postHashRepository;
    private final ImageRepository imageRepository;
    private final ValidateService validateService;

    //post자체가 필요한 경우
    public Optional<Post> getPost(Long postId){
        return postRepository.findById(postId);
    }

    //작성한 모임글의 내용만 가져오기
    public PostResponse getPostResponse(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow( () -> new PostNotFoundFromDB("postId: " + postId + "를 찾지 못했습니다"));

        //post의 조회수 증가
        post.increaseViewCount();

        return new PostResponse(post);
    }

    public Post registerPost(RegisterPostRequest registerPostRequest, User user) {

        //post만들고 영속하는 과정
        Post post = new Post();
        post.setUser(user);
        post.setTitle(registerPostRequest.getTitle());
        post.setContent(registerPostRequest.getContent());
        post.setStateCode(StateCode.ACTIVE);
        post.setViewCount(0L);

        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        //약속시간이 현재 시간보다 늦은지 확인
        if( registerPostRequest.getStartTime().isBefore(now) ){
            throw new TimeBeforeCurrentException("현재 시간: " + now + "설정한 시간: " +registerPostRequest.getStartTime());
        }
        post.setStartTime(registerPostRequest.getStartTime());
        post.setEndTime(registerPostRequest.getStartTime().plusDays(1));

        Post save = postRepository.save(post);

        //작성자는 참여자이기도 하므로 참여인원에 등록
        PostUser postUser = new PostUser();
        postUser.setUser(user);
        postUser.setPost(post);
        postUserRepository.save(postUser);


        //PostHash에 등록하는 과정
        List<String> hashtagStrings = registerPostRequest.getHashtags();
        for (String hashtagString : hashtagStrings) {
            Hashtag hashtag = hashtagRepository.findByHashtagName(hashtagString)
                    .orElseThrow( () ->  new HashtagNotFoundFromDB(hashtagString));
            PostHash postHash = new PostHash();
            postHash.setPost(save);
            postHash.setHashtag(hashtag);
            postHashRepository.save(postHash);
        }

        return save;
    }

    //검증 필요 + state_code가 active인것만 검색되게 할지 결정해야함
    public List<PostResponse> findByTitle(String title) {
        List<Post> posts = postRepository.findByTitle(title);
        return posts.stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
    }


    //ToDo 이미지 경로 추후 수정 바람
    public Page<NoLoginPostsRequest> noLoginGetPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        return posts.map(
                m -> new NoLoginPostsRequest().builder()
                                                .id(m.getPostId())
                                                .contents(m.getContent())
                                                .path(null)
                                                .title(m.getTitle())
                                                .build());
    }

    public Post editPost(Long postId, User user, PostUpdateRequest postUpdateRequest){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundFromDB("postId: " + postId + "를 찾지 못했습니다"));

        log.info("작성자 : {}, 수정자 : {}", post.getUser().getUserId(), user.getUserId());

        if( !(post.getUser().getUserId().equals(user.getUserId())) ){
            throw new InvalidEditorException("작성자와 수정자 불일치 작성자: " + post.getUser().getUserId() + " 수정자: " + user.getUserId() );
        }

        //글 수정

        //기본정보 수정
        post.setTitle(postUpdateRequest.getTitle());
        post.setContent(postUpdateRequest.getContent());

        //시간 수정
        post.setStartTime(postUpdateRequest.getStartTime());
        post.setEndTime(postUpdateRequest.getStartTime().plusDays(1));

        //해쉬태그 수정
            //삭제
        deleteHashtagFromPost(postUpdateRequest, post);
            //추가
        addHashtagToPost(postUpdateRequest, post);

        //이미지 수정
        for (Long removeImage : postUpdateRequest.getRemoveImages()) {
            imageRepository.deleteById(removeImage);
        }

        return post;
    }

















//기타 메서드들


    private void addHashtagToPost(PostUpdateRequest postUpdateRequest, Post post) {
        for (String addTag : postUpdateRequest.getAddTags()) {
            Optional<Hashtag> optionalHashtag = hashtagRepository.findByHashtagName(addTag);

            if( optionalHashtag.isPresent() ){  //이미 있는 해쉬태그의 경우
                Hashtag hashtag = optionalHashtag.get();

                PostHash postHash = new PostHash();
                postHash.setPost(post);
                postHash.setHashtag(hashtag);
                postHashRepository.save(postHash);
            }
            else {
                //해쉬태그 만들기
                Hashtag hashtag = new Hashtag();
                hashtag.setHashtagName(addTag);
                Hashtag savedHashtag = hashtagRepository.save(hashtag);

                //해쉬태그 저장
                PostHash postHash = new PostHash();
                postHash.setPost(post);
                postHash.setHashtag(savedHashtag);
                postHashRepository.save(postHash);
            }

        }
    }

    private void deleteHashtagFromPost(PostUpdateRequest postUpdateRequest, Post post) {
        for (String removeTag : postUpdateRequest.getRemoveTags()) {
            Hashtag hashtag = hashtagRepository.findByHashtagName(removeTag).orElseThrow(
                    () -> new HashtagNotFoundFromDB("없는 해쉬태그"));
            PostHash postHash = postHashRepository.findByPostAndHashtag(post, hashtag)
                    .orElseThrow(() -> new RuntimeException("이 글에 달린 태그가 아님"));

            postHashRepository.delete(postHash); //태그 지우기
        }
    }


}
