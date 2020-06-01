package com.baizhi.dao;
import com.baizhi.entity.Stock;

public interface StockDAO {

    //根据商品id查询库存信息的方法
    Stock checkStock(Integer id);

    //根据商品id扣除库存
    int updateSale(Stock stock);
}
