package com.nowcoder.community.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public void saveDiscussPost(DiscussPost post) {
        discussPostRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    public Map<String, Object> searchDiscussPost(String keyword, int current, int limit) {
        Query searchQuery = NativeQuery.builder()
                .withFields("title", "content")
                .withFilter(QueryBuilders.multiMatch(f -> f.query(keyword)))
                .withSort(Sort.by("type").descending())
                .withSort(Sort.by("score").descending())
                .withSort(Sort.by("createTime").descending())
                .withPageable(PageRequest.of(current, limit))
                .build();
        SearchHits<DiscussPost> searchHits = elasticsearchTemplate.search(searchQuery, DiscussPost.class);

        if (searchHits.getTotalHits() <= 0) {
            return null;
        }

        List<DiscussPost> list = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            DiscussPost post = (DiscussPost) hit.getContent();

            // 处理高亮显示
            post.setTitle(highlight(post.getTitle(), keyword));
            post.setContent(highlight(post.getContent(), keyword));
            list.add(post);
        }
        // 封装帖子以及查询所得总数量
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalHits", searchHits.getTotalHits());

        return result;
    }

    /**
     * 高亮字符串中的关键字
     * @param content 待检索的内容
     * @param keyword 要高亮显示的关键字
     * @return
     */
    private String highlight(String content, String keyword) {
        StringBuilder result = new StringBuilder();
        Map<Integer, String> map = new HashMap<>();
        int startIndex = 0; // 关键字起始索引
        int endIndex = 0; // 关键字结尾索引
        boolean isOpen = false; // 进入关键字匹配标志
        for (int i = 0; i < content.length(); i++) {
            for (char keyChar : keyword.toCharArray()) {
                if (Character.toLowerCase(content.charAt(i)) == Character.toLowerCase(keyChar)) {
                    if (!isOpen) { // 匹配到关键字字符相等
                        startIndex = i; // 将起始索引从当前字符串开始进行遍历
                        endIndex = i;
                        isOpen = true; // 标记进入匹配模式
                    }
                    if (isOpen) {
                        endIndex++; // 匹配时依次移动结尾索引
                    }
                    i = endIndex; // 将原始字符串索引往后移动一位与keyword循环的下一个字符比较
                } else {
                    isOpen = false; // 如果不相等则匹配结束
                }
            }
            if (endIndex - startIndex == keyword.length()) { // 结束索引与起始索引相减等于关键字长度说明匹配到了
                // 将起始索引和结束索引对应到高亮标签put到hash表里，并且重置匹配标志
                map.put(startIndex, "<em>");
                map.put(endIndex, "</em>");
                isOpen = false;
            }
        }
        // 遍历原始字符串，通过哈希表存储的高亮索引，将标签拼接到原始字符串里
        for (int i = 0; i < content.length(); i++) {
            result.append(map.getOrDefault(i, "")).append(content.charAt(i));
        }
        // 处理特殊情况，关键字在最后的情况
        result.append(map.getOrDefault(content.length(), ""));

        return result.toString();
    }
}
