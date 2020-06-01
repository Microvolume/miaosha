# 笔记  
                                                     **解决秒杀系统超卖问题**  
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
   *这里有个细节需要注意，不要把 synchronized 代码块直接写到业务层方法上，如果业务层上还有@Transactional 的话，仍然会出现超卖的情况，原因就是@Transactional 的作用域比 synchronized 关键字要大，若一个线程在执行完synchronized之后，@Transactional 却没提交，而这时又有一个线程进来继续执行，变会出现多个线程一起提交的情况；解决的办法是 synchronized 写在业务方法的调用处。*  
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
*乐观锁的思想：使用乐观锁解决商品的超卖问题,实际上是把主要防止超卖问题交给数据库解决,利用数据库中定义的`version字段`以及数据库中的`事务`实现在并发情况下商品的超卖问题。*  
*（1）更新库存方法改造*  
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
*（2）用Jmeter进行压力测试，2000并发请求，该方式的时间花费是34s*  
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%88%AA%E5%9B%BE%20(9).png?raw=true) 

***总结：解决商品超卖有两种解决方案，一种就是用synchronized 悲观锁这种方式，让请求一直阻塞等待这种方式；另一种是用数据库中乐观锁这种方式，
加上版本号的方式来解决商品超卖。***



                                                      **解决秒杀系统限流问题**  
                                            =========================================  
***1、什么是接口限流***  
    *`限流:是对某一时间窗口内的请求数进行限制，保持系统的可用性和稳定性，防止因流量暴增而导致的系统运行缓慢或宕机`*

***2、为什么要做接口限流***  
    *在面临高并发的抢购请求时，我们如果不对接口进行限流，可能会对后台系统造成极大的压力。大量的请求抢购成功时需要调用下单的接口，过多的请求打到数据库会对系统的稳定性造成影响。*

***3、如何解决接口限流***  
    *常用的限流算法有`令牌桶`和和`漏桶(漏斗算法)`，而Google开源项目Guava中的RateLimiter使用的就是令牌桶控制算法。*
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E6%BC%8F%E6%96%97%E7%AE%97%E6%B3%95.jpeg?raw=true)  
    *漏桶算法比较简单，就是将流量放入桶中，漏桶同时也按照一定的速率流出，如果流量过快的话就会溢出(漏桶并不会提高流出速率)。溢出的流量则直接丢弃。这种做法简单粗暴。漏桶算法虽说简单，但却不能应对实际场景，比如突然暴增的流量。*  
![alt](https://github.com/Microvolume/miaosha/blob/master/src/main/resources/static/image/%E4%BB%A4%E7%89%8C%E6%A1%B6%E7%AE%97%E6%B3%95.jpeg?raw=true)  
    *最初来源于计算机网络。在网络传输数据时，为了防止网络拥塞，需限制流出网络的流量，使流量以比较均匀的速度向外发送。令牌桶算法就实现了这个功能，可控制发送到网络上数据的数目，并允许突发数据的发送。大小固定的令牌桶可自行以恒定的速率源源不断地产生令牌。如果令牌不被消耗，或者被消耗的速度小于产生的速度，令牌就会不断地增多，直到把桶填满。后面再产生的令牌就会从桶中溢出。最后桶中可以保存的最大令牌数永远不会超过桶的大小。这意味，面对瞬时大流量，该算法可以在短时间内请求拿到大量令牌，而且拿令牌的过程并不是消耗很大的事情。*  
*用乐观锁的方式，在controller层上，秒杀业务逻辑前面加上令牌桶进行限流*  
```java
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
```
                                     


