package com.codingchosun.backend.service;


import com.codingchosun.backend.domain.*;
import com.codingchosun.backend.exception.NotEqualsUserSize;
import com.codingchosun.backend.exception.ObjectNotFound;
import com.codingchosun.backend.repository.postrepository.DataJpaPostRepository;
import com.codingchosun.backend.repository.postuserrepository.DataJpaPostUserRepository;
import com.codingchosun.backend.repository.templaterepository.TemplateRepository;
import com.codingchosun.backend.repository.userrepository.DataJpaUserRepository;
import com.codingchosun.backend.repository.validaterepository.ValidateRepository;
import com.codingchosun.backend.request.UserValidate;
import com.codingchosun.backend.request.ValidateRequest;
import com.codingchosun.backend.response.MembersAndTemplates;
import com.codingchosun.backend.response.UpdateUsersManner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ValidateService {
    private final ValidateRepository validateRepository;
    private final DataJpaUserRepository dataJpaUserRepository;
    private final DataJpaPostRepository dataJpaPostRepository;
    private final TemplateRepository templateRepository;
    private final DataJpaPostUserRepository dataJpaPostUserRepository;
    //ToDo 들어온 유저 갯수 == postUser - 1
    // 평가하기 했을 때 저장되는 경우
    @Transactional
    public List<UpdateUsersManner> saveValidate(Long postId, Long fromUserId, ValidateRequest validateRequest) {
        if (!checkUserList(postId, validateRequest)) {
            throw new NotEqualsUserSize("모든 사용자를 투표하지 않았습니다");
        }
        Post post = dataJpaPostRepository.findById(postId).orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"));
        User fromUser = dataJpaUserRepository.findById(fromUserId).orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 작성자가 없음"));

        for (UserValidate userValidate: validateRequest.getUserValidate()) {
            if (!fromUserId.equals(userValidate.getUserId())) {
                User toUser = dataJpaUserRepository.findById(userValidate.getUserId()).orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 유저 없음"));
                Template template = templateRepository.findTemplateByContent(userValidate.getTemplateName()).orElseThrow(() -> new ObjectNotFound("내용과 일치하는 템플릿 없음"));
                validateRepository.save(Validate.builder()
                        .post(post)
                        .fromUser(fromUser)
                .toUser(toUser)
                .template(template)
                .build());
    }
}
        return calculateManner(postId, validateRequest);
    }

    private Boolean checkUserList(Long postId, ValidateRequest validateRequest) {
        List<PostUser> postUsers = dataJpaPostUserRepository.findAllByPost_PostId(postId);
        List<UserValidate> userValidates = validateRequest.getUserValidate();

        if (postUsers.size() -1 == userValidates.size()) {
            return true;
        }

        return false;
    }

    private List<UpdateUsersManner> calculateManner(Long postId, ValidateRequest validateRequest) {

        List<UpdateUsersManner> updateUsersManner = new ArrayList<>();
        List<PostUser> postUsers = dataJpaPostUserRepository.findAllByPost_PostId(postId);
        List<User> users = new ArrayList<>();
        for (PostUser postUser : postUsers) {
            users.add(postUser.getUser());
        }

        List<UserValidate> userValidates = validateRequest.getUserValidate();

        for (User user : users) {
            for (UserValidate userValidate : userValidates) {
                if (user.getNickname().equals(dataJpaUserRepository.findByUserId(userValidate.getUserId()).getNickname())) {
                    int score = templateRepository.findTemplateByContent(userValidate.getTemplateName()).orElseThrow(() -> new ObjectNotFound("컨텐츠에 해당하는 유저 없음")).getScore();
                    user.calMannerScore(score);
                    updateUsersManner.add(new UpdateUsersManner(user.getNickname(), user.getScore()));
                }
            }

        }

        return updateUsersManner;
    }


//    public List<UpdateUsersManner> calculateManner2(Long postId, ValidatedUserRequest validatedUserRequest) {
//
//        List<UpdateUsersManner> updateUsersManner = new ArrayList<>();
////        포스트 유저를 리스트로 가져온다
//        List<PostUser> postUsers = dataJpaPostUserRepository.findAllByPost_PostId(postId);
//        List<User> users = new ArrayList<>();
//        for (PostUser postUser : postUsers) {
//            users.add(postUser.getUser());
//        }
//
//        List<ValidatedUser> validatedUsers = validatedUserRequest.getValidatedUserList();
//
//        for (User user : users) {
//            for (ValidatedUser validatedUser : validatedUsers) {
//                if (user.getNickname().equals(validatedUser.getUserName())) {
//                    int score = templateRepository.findTemplateByContent(validatedUser.getScriptTitle()).orElseThrow(() -> new ObjectNotFound("컨텐츠에 해당하는 유저 없음")).getScore();
//                    user.calMannerScore(score);
//                    updateUsersManner.add(new UpdateUsersManner(user.getNickname(), user.getScore()));
//                }
//            }
//
//        }
//
//        return updateUsersManner;
//    }


    // 포스트 아이디를 받아서 포함된 멈버를 다 가져오고, 템플릿도 모두 받아와서 보내준다
    public MembersAndTemplates getParticipateMember(Long postId) {
//        Post post = dataJpaPostRepository.findById(postId).orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"));
//        User writer = post.getUser();
//        List<PostUser> postUsers = dataJpaPostRepository.findById(postId)
//                .orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"))
//                .getPostUsers();
//        List<String> userNames = new ArrayList<>();
//        for(PostUser p : postUsers) {
//            if(p != null && p.getUser() != writer) {
//                userNames.add(p.getUser().getName());
//            }
//        }
//        Post post = dataJpaPostRepository.findById(postId).orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"));
//        User writer = post.getUser();

        Post post = dataJpaPostRepository.findById(postId)
                .orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"));
        User writer = post.getUser();
        List<String> userNickNames = dataJpaPostRepository.findById(postId)
                .orElseThrow(() -> new ObjectNotFound("아이디에 해당하는 포스트가 없음"))
                .getPostUsers().stream()
                .filter(p -> p != null && !p.getUser().equals(writer))
                .map(p -> p.getUser().getName())
                .toList();


//        List<Template> templates = templateRepository.findAll();
//        List<String> templateNames = new ArrayList<>();
//        for(Template template : templates) {
//            if(Objects.nonNull(template)) {
//                templateNames.add(template.getContent());
//            }
//        }

        List<String> templateNames = templateRepository.findAll().stream()
                .filter(Objects::nonNull)
                .map(Template::getContent)
                .toList();

        return MembersAndTemplates.builder().writer(writer.getNickname()).members(userNickNames).templateNames(templateNames).build();

    }

    // 모임 시작됐을 때 저장되는 경우
    public void saveValidate2(Long postId, Long fromUserId) {
        Post post = dataJpaPostRepository.findById(postId).get();
        User fromUser = dataJpaUserRepository.findById(fromUserId).get();

        validateRepository.save(Validate.builder()
                        .post(post)
                        .fromUser(fromUser)
                        .build());

    }

}
