package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 搜索帖子
        Map<String, Object> resultMap = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        // 聚合数据
        List<Map<String, Object>> posts = new ArrayList<>();
        if (resultMap != null) {
            List<DiscussPost> postList = (List<DiscussPost>) resultMap.get("list");
            if (postList != null) {
                for (DiscussPost post : postList) {
                    Map<String, Object> map = new HashMap<>();
                    // 帖子
                    map.put("post", post);
                    // 作者
                    map.put("user", userService.findUserById(post.getUserId()));
                    // 点赞数量
                    map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                    posts.add(map);
                }
            }
        }

        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);

        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(resultMap == null ? 0 : (int) (long) resultMap.get("totalHits"));

        return "/site/search";
    }
}
