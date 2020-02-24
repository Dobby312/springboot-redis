/**
 * @author Dobby
 * Feb 24, 2020
 */
package com.dobby.lock.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
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

	private long nKuCun = 0;

	private String key = "computer_key";

	// 获取锁的超时时间
	private int timeout = 30 * 1000;

	@GetMapping("/order")
	public List<String> order() {
		// 抢到商品的用户
		List<String> shopUsers = new ArrayList<String>();
		// 构造很多用户
		List<String> users = new ArrayList<>();
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
}
