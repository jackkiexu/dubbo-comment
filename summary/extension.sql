getAdaptiveExtension() 获取一个扩展类, 如果 @Adaptive 注解在类上, 就是一个装饰类, 如果注解在方法上就是一个动态代理类, 例如 Protocol$Adaptive 对象
getExtension(String name) 获取一个指定对象

adaptive 注解在类与方法上的区别
1. 注解在类上代表人工编码, 即实现了一个装饰类 (例如 ExtensionFactory)
2. 注解在方法上: 代表自动生成和编译一个动态的 adaptive类 (例如 Protocol$Adaptive)

--> getAdaptiveExtension() // 为 cachedAdaptiveInstance 赋值
    --> createAdaptiveExtension()
        --> getAdaptiveExtensionClass()
            --> getExtensionClasses() // 为cachedClass 赋值(从 SPI 文件中获取所有类, 缓存起来)
                --> loadExtensionClasses()
                    --> loadFile
            --> createAdaptiveExtensionClass // 自动生成动态 adaptive 类, 这个类是一个代理类
                --> com.alibaba.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
                --> compiler.compile(code, classloader)
        --> injectExtension() // 作用进入 IOC 的反转控制模式, 实现了动态入注

关于 loadFile 的一些细节
目的: 通过把配置文件 META-INF/dubbo/internal/com.alibaba.dubbo.prc.Protocol 的内容, 存储在缓存里面
cachedAdaptiveClass // 如果这个 class 含有 Adaptive 注解就赋值, 例如 ExtensionFactory, Protocol 在这个环节是没有的
cacheWrapperClasses // 只有当 class 没有adaptive注解, 并且构造函数包含目标接口(type) 类型, 例如 protocol 里面的 SPI 只有 ProtocolListenWrapper, ProtocolFilterWrapper

cachedActives: 剩下的类, 包含 Active 注解
cachedName:    剩下的类就存储在这里


--> getExtension(String name) 指定对象换存在 cachedInstance 里面， get出来的对象 wrapper 对象, 例如 Protocol 获取出来的就是 ProtocolListenerWrapper 或 ProtocolFilterWrapper 对象 (PS: 这里有可能 ProtocolListenerWrapper 包裹  ProtocolFilterWrapper)
    --> createExtension(String name)
      --> getExtensionClass()
        --> injectExtension(T instance)
          --> objectFactory.getExtension(pt, property)
            --> SpiExtensionFactory.getExtension(type, name)
              --> ExtensionLoader.getExtensionLoader(type)
              --> loader.getAdaptiveExtension()
            --> SpringExtensionFactory.getExtension(type, name)
              --> ApplicationContext.getBean(type, name)
        --> injectExtension((T)wrapperClass.getConstructor(type).newInstance(instance)) // AOP 的简单设计模式


ExtensionLoader
    构造函数: 单例模式, 工厂模式
    Adaptive:
        IOC, AOP (最终是 Wrapper 对象)
        使用装饰器模式(@Adaptive注释在类上面), 动态代理模式(@注释在方法上)