/**
 * @author Dobby
 * Feb 24, 2020
 */
package com.dobby.lock.util;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


/**
 * @author Dobby
 *
 * Feb 24, 2020
 */
@Component
public class JedisCom {
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	public boolean setnx(String key, String val) {
		try {
		return redisTemplate.opsForValue().setIfAbsent(key, val, 1000*60, TimeUnit.SECONDS);
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
		}
		return false;
	}
	
	public boolean delnx(String key) {
		try {
			if(redisTemplate.hasKey(key)) {
				return stringRedisTemplate.delete(key);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
		}
		return false;
	}
}
