package com.baizhi.service;

public interface OrderService {
    //用来处理秒杀的下单方法 并返回订单id
    int kill(Integer id);
}
