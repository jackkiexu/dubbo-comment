/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.MultiMessage;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

import java.io.IOException;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public final class DubboCountCodec implements Codec2 {

    private DubboCodec codec = new DubboCodec();

    public void encode(Channel channel, ChannelBuffer buffer, Object msg) throws IOException { // dubbo 中 编码器
        codec.encode(channel, buffer, msg);                                 // DubboCodec
    }

    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {  //  dubbo 中的解码器
        int save = buffer.readerIndex();
        MultiMessage result = MultiMessage.create();
        do {
            Object obj = codec.decode(channel, buffer);                         // DubboCodec
            if (Codec2.DecodeResult.NEED_MORE_INPUT == obj) {                   // 返回值若是 NEED_MORE_INPUT, 则表示 解码没成功, 要么是包不完整
                buffer.readerIndex(save);
                break;
            } else {
                result.addMessage(obj);                                         // 将数据包加入到 通过迭代器模式设计的一个集合中
                logMessageLength(obj, buffer.readerIndex() - save);             // 更新数据包的大小
                save = buffer.readerIndex();                                    // 更新这次读取后, 数据读取的位置
            }
        } while (true);
        if (result.isEmpty()) {
            return Codec2.DecodeResult.NEED_MORE_INPUT;
        }
        if (result.size() == 1) {
            return result.get(0);
        }
        return result;
    }

    private void logMessageLength(Object result, int bytes) { // 更新数据包大小
        if (bytes <= 0) {
            return;
        }
        if (result instanceof Request) {
            try {
                ((RpcInvocation) ((Request) result).getData()).setAttachment(
                        Constants.INPUT_KEY, String.valueOf(bytes));   // 设置 Request 的数据包大小
            } catch (Throwable e) {
                /* ignore */
            }
        } else if (result instanceof Response) {
            try {
                ((RpcResult) ((Response) result).getResult()).setAttachment(
                        Constants.OUTPUT_KEY, String.valueOf(bytes));  // 设置 Response 的数据包大小
            } catch (Throwable e) {
                /* ignore */
            }
        }
    }

}
