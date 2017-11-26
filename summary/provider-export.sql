暴露本地服务 和 暴露远程服务
1. 暴露本地服务: 指暴露在同一个 JVM 里面, 不通过调用zk来进行远程通信, 例如: 在同一个服务, 自己调用自己的接口, 没有必要进行网络 IP 连接来通信
2. 暴露远程服务: 指暴露给远程客户端的 IP 和 端口, 通过网络来执行通讯


ServiceBean.onApplicationEvent
--> export()
  --> ServiceConfig.export()
    --> doExport()
      --> doExportUrls() // 里面有个 for 循环, 代表一个服务可以有多个通许协议
        --> loadRegistries(); // dubbo.properties 里面组装 registry 的 url 信息
          --> doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs)
            --> exportLocal(URL url)
              --> Exporter<?> exporter = protocol.export(proxyFactory.getInvoker(ref, (Class) interfaceClass, local));

