package com.nowcoder.community;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert() {
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101,0,100, 0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102,0,100, 0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134,0,100,0));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水。");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete() {
        discussPostRepository.deleteById(231);
    }

    @Test
    public void testSearchByTemplate() {
        String keyword = "互联网寒冬";
        Query searchQuery = NativeQuery.builder()
                .withFields("title", "content")
                .withFilter(QueryBuilders.multiMatch(f -> f.query(keyword)))
                .withSort(Sort.by("type").descending())
                .withSort(Sort.by("score").descending())
                .withSort(Sort.by("createTime").descending())
                .withPageable(PageRequest.of(0, 10))
                .build();
        SearchHits<DiscussPost> searchHits = elasticsearchTemplate.search(searchQuery, DiscussPost.class);

        if (searchHits.getTotalHits() <= 0) {
            throw new RuntimeException("未检索到匹配内容");
        }

        // 获取搜索关键词的分词集合
        Set<String> words = getAnalyzeWords(keyword);
        List<DiscussPost> list = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            DiscussPost post = (DiscussPost) hit.getContent();

            // 处理高亮显示结果
            for (String word : words) {
                post.setTitle(highlight(post.getTitle(), word));
                post.setContent(highlight(post.getContent(), word));
            }
            System.out.println(post);
            list.add(post);
        }
    }

    // 获取查询关键字的分词集合
    public Set<String> getAnalyzeWords(String keyword) {
        Set<String> words = new HashSet<>();
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:9200/_analyze?pretty=true";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> map = new HashMap<>();
        map.put("analyzer", "ik_smart");
        map.put("text", keyword);
        HttpEntity<String> httpEntity = new HttpEntity<>(JSON.toJSONString(map), headers);
        ResponseEntity<String> result = restTemplate.postForEntity(url, httpEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(result.getBody());
        JSONArray tokens = jsonObject.getJSONArray("tokens");
        if (!tokens.isEmpty() && tokens.size() > 0) {
            for (int i = 0; i < tokens.size(); i++) {
                JSONObject word = tokens.getJSONObject(i);
                words.add(word.getString("token"));
            }
        }
        return words;
    }

    /**
     * 高亮字符串中的关键字
     * @param content 待检索的内容
     * @param keyword 要高亮显示的关键字
     * @return
     */
    public String highlight(String content, String keyword) {
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
