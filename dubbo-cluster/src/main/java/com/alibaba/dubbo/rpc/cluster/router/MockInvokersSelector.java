/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.router;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * mock invoker 选择器
 *
 * @author chao.liuc
 */
public class MockInvokersSelector implements Router {

    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers, URL url, final Invocation invocation) throws RpcException {
        if (invocation.getAttachments() == null) { // 若 invoker 中的 attachment 是 null, 则直接获取普通的 invoker
            return getNormalInvokers(invokers);
        } else {
            String value = invocation.getAttachments().get(Constants.INVOCATION_NEED_MOCK); // 检查 invoker 是否需要 invoker
            if (value == null)
                return getNormalInvokers(invokers); // 直接 获取 普通的 invoker
            else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {  // 若 invocation.need.mock = true, 则获取 mock 类型的 invoker
                return getMockedInvokers(invokers);
            }
        }
        return invokers;
    }

    private <T> List<Invoker<T>> getMockedInvokers(final List<Invoker<T>> invokers) { // 获取 mock 类型的 invoker
        if (!hasMockProviders(invokers)) {  // 判断所有 invoker 中是否有 mock 类型的数据
            return null;
        }
        List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(1);
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) { // 若是 mock 类型的 invoker, 则直接加入其中 (PS: 这里建议加个优先级, 像 Spring 中的 Ordered 接口)
                sInvokers.add(invoker);
            }
        }
        return sInvokers;
    }

    private <T> List<Invoker<T>> getNormalInvokers(final List<Invoker<T>> invokers) {  // 只获取普通的 invoker, 排除 mock 类型的
        if (!hasMockProviders(invokers)) {
            return invokers;
        } else {
            List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(invokers.size());
            for (Invoker<T> invoker : invokers) {
                if (!invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) {  // 若协议是 mock 则直接跳过
                    sInvokers.add(invoker);
                }
            }
            return sInvokers;
        }
    }

    private <T> boolean hasMockProviders(final List<Invoker<T>> invokers) {  // 判断是否在 invoker 中是否有 协议是 mock 类型的
        boolean hasMockProvider = false;
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) { //  invoker 的协议是 mock
                hasMockProvider = true;
                break;
            }
        }
        return hasMockProvider;
    }

    public URL getUrl() {
        return null;
    }

    public int compareTo(Router o) {
        return 1;
    }

}
