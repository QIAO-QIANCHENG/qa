package com.bingo.qa.controller;

import com.bingo.qa.async.EventModel;
import com.bingo.qa.async.EventProducer;
import com.bingo.qa.async.EventType;
import com.bingo.qa.model.*;
import com.bingo.qa.service.CommentService;
import com.bingo.qa.service.FollowService;
import com.bingo.qa.service.QuestionService;
import com.bingo.qa.service.UserService;
import com.bingo.qa.util.QaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bingo in 2018/5/31.
 */

@Controller
public class FollowController {
    @Autowired
    FollowService followService;

    @Autowired
    CommentService commentService;

    @Autowired
    QuestionService questionService;

    @Autowired
    UserService userService;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    EventProducer eventProducer;

    @PostMapping(value = {"/followUser"})
    @ResponseBody
    public String follow(@RequestParam("userId") int userId) {
        System.out.println("调用了此方法, 参数为id:" + userId);
        if (hostHolder.getUser() == null) {
            return QaUtil.getJSONString(999);
        }

        // 当前登录的用户，关注了另一个人
        boolean ret = followService.follow(hostHolder.getUser().getId(), EntityType.ENTITY_USER, userId);
        eventProducer.fireEvent(new EventModel(EventType.FOLLOW)
                .setActorId(hostHolder.getUser().getId())
                .setEntityId(userId)
                .setEntityOwnerId(userId)
                .setEntityType(EntityType.ENTITY_USER)
        );

        // 若成功，返回0 和 当前登录用户所关注的用户数量
        System.out.println(followService.getFolloweeCount(
                hostHolder.getUser().getId(), EntityType.ENTITY_USER));
        System.out.println(ret);
        return QaUtil.getJSONString(ret ? 0 : 1, followService.getFolloweeCount(
                                                hostHolder.getUser().getId(), EntityType.ENTITY_USER) + "");

    }

    @PostMapping(value = {"/unfollowUser"})
    @ResponseBody
    public String unfollow(@RequestParam("userId") int userId) {
        if (hostHolder.getUser() == null) {
            return QaUtil.getJSONString(999);
        }

        boolean ret = followService.unfollow(hostHolder.getUser().getId(), EntityType.ENTITY_USER, userId);
        eventProducer.fireEvent(new EventModel(EventType.UNFOLLOW)
                .setActorId(hostHolder.getUser().getId())
                .setEntityId(userId)
                .setEntityOwnerId(userId)
                .setEntityType(EntityType.ENTITY_USER)
        );

        // 若成功，返回0 和 当前登录用户所关注的用户数量
        return QaUtil.getJSONString(ret ? 0 : 1, followService.getFolloweeCount(
                hostHolder.getUser().getId(), EntityType.ENTITY_USER) + "");

    }

    /**
     * 关注问题
     * @param questionId
     * @return
     */
    @PostMapping(value = {"/followQuestion"})
    @ResponseBody
    public String followQuestion(@RequestParam("questionId") int questionId) {
        if (hostHolder.getUser() == null) {
            return QaUtil.getJSONString(999);
        }

        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            return QaUtil.getJSONString(1, "问题不存在");
        }

        // 当前登录的用户，关注了某一个问题
        boolean ret = followService.follow(hostHolder.getUser().getId(), EntityType.ENTITY_QUESTION, questionId);
        eventProducer.fireEvent(new EventModel(EventType.FOLLOW)
                .setActorId(hostHolder.getUser().getId())
                .setEntityId(questionId)
                .setEntityOwnerId(question.getUserId())
                .setEntityType(EntityType.ENTITY_QUESTION)
        );


        Map<String, Object> info = new HashMap<>();
        info.put("headUrl", hostHolder.getUser().getHeadUrl());
        info.put("name", hostHolder.getUser().getName());
        info.put("id", hostHolder.getUser().getId());
        // 当前问题的关注者数量
        info.put("count", followService.getFollowerCount(EntityType.ENTITY_QUESTION, questionId));

        // 若成功，返回0 和 当前登录用户所关注的问题数量
        return QaUtil.getJSONString(ret ? 0 : 1, info);

    }

    /**
     * 取消关注问题
     * @param questionId
     * @return
     */
    @PostMapping(value = {"/unfollowQuestion"})
    @ResponseBody
    public String unfollowQuestion(@RequestParam("questionId") int questionId) {
        if (hostHolder.getUser() == null) {
            return QaUtil.getJSONString(999);
        }

        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            return QaUtil.getJSONString(1, "问题不存在");
        }

        // 当前登录的用户，关注了某一个问题
        boolean ret = followService.unfollow(hostHolder.getUser().getId(), EntityType.ENTITY_QUESTION, questionId);
        eventProducer.fireEvent(new EventModel(EventType.UNFOLLOW)
                .setActorId(hostHolder.getUser().getId())
                .setEntityId(questionId)
                .setEntityOwnerId(question.getUserId())
                .setEntityType(EntityType.ENTITY_QUESTION)
        );

        Map<String, Object> info = new HashMap<>();
        info.put("headUrl", hostHolder.getUser().getHeadUrl());
        info.put("name", hostHolder.getUser().getName());
        info.put("id", hostHolder.getUser().getId());
        // 当前问题的关注者数量
        info.put("count", followService.getFollowerCount(EntityType.ENTITY_QUESTION, questionId));

        // 若成功，返回0 和 当前登录用户所关注的问题数量
        return QaUtil.getJSONString(ret ? 0 : 1, info);

    }

    @GetMapping(value = {"/user/{uid}/followees"})
    public String followees(Model model,
                            @PathVariable("uid") int userId) {
        List<Integer> ids = followService.getFollowees(userId, EntityType.ENTITY_USER, 0, 10);
        if (hostHolder.getUser() != null) {
            model.addAttribute("followees", getUsersInfo(hostHolder.getUser().getId(), ids));
        } else {
            model.addAttribute("followees", getUsersInfo(0, ids));
        }
        model.addAttribute("followeeCount", followService.getFolloweeCount(userId, EntityType.ENTITY_USER));
        model.addAttribute("curUser", userService.selectById(userId));

        return "followees";
    }

    @GetMapping(value = {"/user/{uid}/followers"})
    public String followers(Model model,
                            @PathVariable("uid") int userId) {
        List<Integer> ids = followService.getFollowers(userId, EntityType.ENTITY_USER, 0, 10);
        if (hostHolder.getUser() != null) {
            model.addAttribute("followers", getUsersInfo(hostHolder.getUser().getId(), ids));
        } else {
            model.addAttribute("followers", getUsersInfo(0, ids));
        }
        model.addAttribute("followerCount", followService.getFollowerCount(EntityType.ENTITY_USER, userId));
        model.addAttribute("curUser", userService.selectById(userId));
        return "followers";
    }


    public List<ViewObject> getUsersInfo(int localUserId, List<Integer> userIds) {
        List<ViewObject> userInfos = new ArrayList<>();
        for (Integer uid : userIds) {
            User user = userService.selectById(uid);
            if (user == null) {
                continue;
            }

            ViewObject vo = new ViewObject();
            vo.set("user", user);
            vo.set("followerCount", followService.getFollowerCount(EntityType.ENTITY_USER, uid));
            vo.set("followeeCount", followService.getFolloweeCount(EntityType.ENTITY_USER, uid));

            if (localUserId != 0) {
                vo.set("followed", followService.isFollower(localUserId, EntityType.ENTITY_USER, uid));

            } else {
                vo.set("followed", false);
            }

            userInfos.add(vo);
        }
        return userInfos;
    }


}
