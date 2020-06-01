package com.baizhi.controller;

import com.baizhi.service.OrderService;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {

    @Autowired
    private OrderService orderService;

    //创建令牌桶实例
    private RateLimiter rateLimiter =  RateLimiter.create(30);

    //开发秒杀方法  使用悲观锁防止超卖
    @GetMapping("kill")
    public String kill(Integer id){
        System.out.println("秒杀商品的id = " + id);
        try {
                //根据秒杀商品id 去调用秒杀业务
                int orderId = orderService.kill(id);
                return "秒杀成功,订单id为: " + String.valueOf(orderId);

        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发一个秒杀方法 乐观锁防止超卖+ 令牌桶算法限流
    @GetMapping("killtoken")
    public String killtoken(Integer id){
        System.out.println("秒杀商品的id = " + id);
        //加入令牌桶的限流措施
        if(!rateLimiter.tryAcquire(3, TimeUnit.SECONDS)){
            log.info("抛弃请求: 抢购失败,当前秒杀活动过于火爆,请重试");
            return "抢购失败,当前秒杀活动过于火爆,请重试!";
        }
        try {
            //根据秒杀商品id 去调用秒杀业务
            int orderId = orderService.kill(id);
            return "秒杀成功,订单id为: " + String.valueOf(orderId);
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }



//    @GetMapping("sale")
//    public String sale(Integer id){
//        //1.没有获取到token请求一直知道获取到token 令牌
//        //log.info("等待的时间: "+  rateLimiter.acquire());
//
//        //2.设置一个等待时间,如果在等待的时间内获取到了token 令牌,则处理业务,如果在等待时间内没有获取到响应token则抛弃
//        if(!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
//            System.out.println("当前请求被限流,直接抛弃,无法调用后续秒杀逻辑....");
//            return "抢购失败!";
//        }
//        System.out.println("处理业务.....................");
//        return "抢购成功";
//    }
}
