# 笔记  
                                                    解决秒杀系统超卖问题  
                                            =========================================  
***1、项目一开始时报错的地方:***
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE0.png?raw=true)  
*通过日志信息，最后分析了一段时间竟然是密码填的不对。。。改成123456就可以了*  

![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE.png?raw=true)  
***2、正常测试：系统在没有很高并发请求访问情况下，不会出现超卖的现象***  

![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(1).png?raw=true)  
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(2).png?raw=true)  
***3、当再次购买商品时，提示库存不足的提示，库存数量也符合预期情况***  

![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(3).png?raw=true)  
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(4).png?raw=true)  
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(5).png?raw=true)  

***4、用Jmeter压力测试工具模拟高并发，进行压力测试:***  
  *系统处于并发的场景下的话，必须需要对系统进行保护措施*  
  *（1）使用悲观锁的方式解决超卖*
```java
@GetMapping("kill")
    public String kill(Integer id){
        System.out.println("秒杀商品的id = " + id);
        try {
            synchronized (this){
                //根据秒杀商品id 去调用秒杀业务
                int orderId = orderService.kill(id);
                return "秒杀成功,订单id为: " + String.valueOf(orderId);
            }
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }
```  
   *这里有个细节需要注意，不要把 synchronized 代码块直接写到业务层方法上，如果业务层上还有@Transactional 的话，
仍然会出现超卖的情况，原因就是@Transactional 的作用域比 synchronized 关键字要大，若一个线程在执行完synchronized之后，
@Transactional 却没提交，而这时又有一个线程进来继续执行，变会出现多个线程一起提交的情况；解决的办法是 synchronized 
写在业务方法的调用处。*  
```java
@Override
public synchronized int kill(Integer id) {
    //校验库存
    Stock stock = checkStock(id);
    //更新库存
    updateSale(stock);
    //创建订单
    return createOrder(stock);
}
```  
  *（2）用Jmeter进行压力测试，当线程的数量设为2000时，可以观察到系统不会出现问题，虽然这种使用synchronized 悲观锁的的方式可以解决超卖问题，
但在实际中不建议使用，为了保证线程的安全，线程需要排队，当一个线程拿到锁之后，其他线程都需要等待，直到该线程锁释放后，其他线程才有机
会执行，对于用户的体验来讲是非常差的。*

![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(6).png?raw=true) 
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(7).png?raw=true) 
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(8).png?raw=true)  

***5、用乐观锁，版本号的方式解决秒杀业务中商品超卖的问题***
```xml
<!--根据商品id扣除库存-->
<update id="updateSale" parameterType="Stock">
    update stock set
        sale=sale+1,
        version=version+1
     where
        id =#{id}
        and
        version = #{version}
</update>
```  
*用Jmeter进行压力测试，2000并发请求，该方式的时间花费是34s*
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(9).png?raw=true) 

***总结：解决商品超卖有两种解决方案，一种就是用synchronized 悲观锁这种方式，让请求一直阻塞等待这种方式；另一种是用数据库中乐观锁这种方式，
加上版本号的方式来解决商品超卖。***

 


