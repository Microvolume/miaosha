package com.baizhi.service;

import com.baizhi.dao.OrderDAO;
import com.baizhi.dao.StockDAO;
import com.baizhi.entity.Order;
import com.baizhi.entity.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private StockDAO stockDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Override
    public int kill(Integer id) {
        //校验库存
        Stock stock = checkStock(id);
        //更新库存
        updateSale(stock);
        //创建订单
        return createOrder(stock);
    }

    //校验库存
    private Stock checkStock(Integer id){
        Stock stock = stockDAO.checkStock(id);
        if(stock.getSale().equals(stock.getCount())){
            throw  new RuntimeException("库存不足!!!");
        }
        return stock;
    }

    //扣除库存
    private void updateSale(Stock stock){
        //在sql层面完成销量的+1  和 版本号的+  并且根据商品id和版本号同时查询更新的商品
        int updateRows = stockDAO.updateSale(stock);
        if (updateRows==0){
            throw new RuntimeException("请购失败,请重试!!!");
        }
    }

    //创建订单，往数据库中插入订单信息
    private Integer createOrder(Stock stock){
        Order order = new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
        orderDAO.createOrder(order);
        return order.getId();
    }

}
