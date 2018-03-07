package com.alibaba.dubbo.cache;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class CacheFactory$Adaptive implements com.alibaba.dubbo.cache.CacheFactory {
    public com.alibaba.dubbo.cache.Cache getCache(com.alibaba.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("cache", "lru"); // 这里的 cache 是 接口 CacheFactory中方法上的 @Adaptive 的 value, 而 lru 则是接口上 @SPI,value, 表示默认的 Adaptive类
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.cache.CacheFactory) name from url(" + url.toString() + ") use keys([cache])");
        com.alibaba.dubbo.cache.CacheFactory extension = (com.alibaba.dubbo.cache.CacheFactory) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.cache.CacheFactory.class).getExtension(extName);
        return extension.getCache(arg0);
    }
}