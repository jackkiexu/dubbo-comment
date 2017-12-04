
Directory: 目录服务
  StaticDirectory: 静态目录, 他的Invoker是固定的
  RegistryDirectory: 注册目录服务, 它的 invoker 集合数据来源于 ZK 注册中心, 它实现了 NotifyListener, 并且实现了 notify(List<URL> urls)
    整个过程有一个重要的 map 变量, methodInvokerMap(它是数据的来源, 同时也是 notify 的重要操作对象, 重点是写操作)