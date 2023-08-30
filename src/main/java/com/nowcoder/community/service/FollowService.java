package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;

    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);

                operations.multi();

                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    public void unFollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    // 查询关注的实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体的粉丝数量
    public long findFollowerCount(int entityId, int entityType) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // 查询当前用户关注的实体列表
    public List<Map<Integer, Double>> findFolloweeList(int userId, int entityType, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        List<Map<Integer, Double>> followeeList = new ArrayList<>();
        // 倒转排序，实现关注时间往后的显示在前
        Set<Integer> set = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit -1);
        for (Integer entityId : set) {
            Map<Integer, Double> map = new HashMap<>();
            map.put(entityId, redisTemplate.opsForZSet().score(followeeKey, entityId));
            followeeList.add(map);
        }
        return followeeList;
    }

    // 查询实体的粉丝列表
    public List<Map<Integer, Double>> findFollowerList(int entityId, int entityType, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);
        List<Map<Integer, Double>> followerList = new ArrayList<>();
        // 倒转排序，实现关注时间往后的显示在前
        Set<Integer> set = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit -1);
        for (Integer id : set) {
            Map<Integer, Double> map = new HashMap<>();
            map.put(id, redisTemplate.opsForZSet().score(followerKey, id));
            followerList.add(map);
        }

        return followerList;
    }
}
