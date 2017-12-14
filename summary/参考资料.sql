1. 很系统的 dubbo 源码分析
  http://www.cnblogs.com/java-zhao/category/1090034.html


  消费者请求流程:
  1. 通过一开始消费端启动 已经基于接口生成一 Proxy0 类
  2. 直接调用 MockClusterInvoker invoke 进行调用 invoke
    2.1 这里有容错降级的判断, 若有容错 或 降级(通过 URL 里面的 mock 参数), 则直接走 DubboAdimin 里面配置的信息
  3.代码直接到 AbstractClusterInvoker 里面 invoker (PS: 这里的 AbstractClusterInvoker 其实就是 FailOverClusterInvoker)
    3.0 通过 directry.doList 来进行获取对应的 DubboInvoker
      3.0.1 通过 router 来进行筛选 需要的 DubboInvoker
    3.1 通过 ExtensionLoader 来获取默认的 负载均衡 LoadBalance (权重+随机, 权重+轮询, 权重+最小活跃, 一致性 Hash)
    3.2 通过负载均衡器找到对应的 DubboInvoker (这时的DubboInvoker 是被对应 ProtocolFilterWrapper, ProtocolListenWrapper )
    3.3 通过 DubboInvoker 进行调用 netty client 进行调用远程服务, 最后返回一个 DefaultFuture 通过 get() 来进行异步转同步


