package com.dobby.lock.config;
/**
 * redis配置类
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

	@Bean
	@SuppressWarnings("all")
	public  RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(factory);
		//开启Redis事务权限
		//template.setEnableTransactionSupport(true);
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringRedisTemplater = new StringRedisSerializer();
		//key采用String的序列化方式
		template.setKeySerializer(stringRedisTemplater);
		//hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisTemplater);
		//value序列化方式采用Jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		//hash的value的序列化方式也采用Jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}
	
}
