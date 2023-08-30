package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已关注");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unFollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unFollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注");
    }

    @RequestMapping(path = "/followee/{userId}", method = RequestMethod.GET)
    public String getFolloweePage(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 设置分页信息
        page.setLimit(5);
        page.setPath("/followee/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<Integer, Double>> followList = followService.findFolloweeList(userId, ENTITY_TYPE_USER, page.getOffset(), page.getLimit());
        List<Map<String, Object>> followVoList = new ArrayList<>();
        if (followList != null) {
            for (Map<Integer, Double> map : followList) {
                Map<String, Object> voMap = new HashMap<>();
                for (Integer key : map.keySet()) {
                    voMap.put("user", userService.findUserById(key));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String followTime = dateFormat.format(map.get(key));
                    voMap.put("followTime", followTime);
                }
                followVoList.add(voMap);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("followees", followVoList);

        return "/site/followee";
    }

    @RequestMapping(path = "/follower/{userId}", method = RequestMethod.GET)
    public String getFollowerPage(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 设置分页信息
        page.setLimit(5);
        page.setPath("/follower/" + userId);
        page.setRows((int) followService.findFollowerCount(userId, ENTITY_TYPE_USER));

        List<Map<Integer, Double>> followerList = followService.findFollowerList(userId, ENTITY_TYPE_USER, page.getOffset(), page.getLimit());
        List<Map<String, Object>> followerVoList = new ArrayList<>();
        if (followerList != null) {
            for (Map<Integer, Double> map : followerList) {
                Map<String, Object> voMap = new HashMap<>();
                for (Integer key : map.keySet()) {
                    voMap.put("user", userService.findUserById(key));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String followTime = dateFormat.format(map.get(key));
                    voMap.put("followedTime", followTime);
                }
                followerVoList.add(voMap);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("followers", followerVoList);

        return "/site/follower";
    }
}
