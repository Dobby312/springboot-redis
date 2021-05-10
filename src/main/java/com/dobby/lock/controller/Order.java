/**
 * @author Dobby
 * Feb 24, 2020
 */
package com.dobby.lock.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dobby.lock.util.JedisCom;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Dobby
 *
 *         Feb 24, 2020
 */
@Slf4j
@RestController
public class Order {
	@Autowired
	private JedisCom jedisCom;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

	private long nKuCun = 0;

	private String key = "computer_key";

	// 获取锁的超时时间
	private int timeout = 30 * 1000;

	@GetMapping("/order")
	public List<String> order() {
		// 抢到商品的用户
		List<String> shopUsers = new ArrayList<String>();
		// 构造很多用户
		List<String> users = Collections.synchronizedList(new ArrayList<>());
		IntStream.range(0, 100000).parallel().forEach(b -> {
			users.add("Dobby_" + b);
		});
		// 初始化库存
		nKuCun = 10;
		//模拟开抢
		users.parallelStream().forEach(b -> {
			String shopUser = qiang(b);
			if (!StringUtils.isEmpty(shopUser)) {
				shopUsers.add(shopUser);
			}
		});
		return shopUsers;
	}
	
	/**
     * 模拟抢单动作
     *
     * @param b
     * @return
     */
    private String qiang(String b) {
        //用户开抢时间
        long startTime = System.currentTimeMillis();

        //未抢到的情况下，30秒内继续获取锁
        while ((startTime + timeout) >= System.currentTimeMillis()) {
            //商品是否剩余
            if (nKuCun <= 0) {
                break;
            }
            if (jedisCom.setnx(key, b)) {
                //用户b拿到锁
                log.info("用户{}拿到锁...", b);
                try {
                    //商品是否剩余
                    if (nKuCun <= 0) {
                        break;
                    }

                    //模拟生成订单耗时操作，方便查看：Dobby-50 多次获取锁记录
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //抢购成功，商品递减，记录用户
                    nKuCun -= 1;

                    //抢单成功跳出
                    log.info("用户{}抢单成功跳出...所剩库存：{}", b, nKuCun);

                    return b + "抢单成功，所剩库存：" + nKuCun;
                } finally {
                    log.info("用户{}释放锁...", b);
                    //释放锁
                    jedisCom.delnx(key);
                }
            } else {
                //用户b没拿到锁，在超时范围内继续请求锁，不需要处理
//                if (b.equals("Dobby-50") || b.equals("Dobby-69")) {
//                    logger.info("用户{}等待获取锁...", b);
//                }
            }
        }
        return "";
    }

    /**
     * API调用频率
     * @param apiName 接口名称
     * @param rate 频率/秒
     * @return
     */
    public boolean apiRate(String apiName, int rate) {
        try {
            /**
             * redis-cli 命令
             eval "return redis.call('cl.throttle',KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])[1];" 1 key 3 3 10 1
             */
            DefaultRedisScript script = new DefaultRedisScript();
            script.setResultType(Long.class);
            /*
                cl.throttle命令返回的是一个table，这里只关注第一个元素0表示正常，1表示过载
                KEYS[1]需要设置的key值，结合业务需要可以是接口名称+用户ID+购买的资源包等等等等
                ARGV[1]参数是漏斗肚子的大小  个人理解加上实测：此值越小越接近设置的rate,越大越有令牌桶的味道会有瞬时超过rate
                ARGV[2]频率次数，结合ARGV[3]一起使用
                ARGV[3]周期（秒），结合ARGV[2]一起使用
                ARGV[4] 不知道是什么没用上
             */
            script.setScriptText("return redis.call('cl.throttle',KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])[1]");
            Long rst = (Long)redisTemplate.execute(script, Arrays.asList(apiName), 1,rate,1,1);
            return rst == 0 ;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
