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
package com.alibaba.dubbo.remoting.exchange.codec;

import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.StreamUtils;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferInputStream;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferOutputStream;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.remoting.telnet.codec.TelnetCodec;
import com.alibaba.dubbo.remoting.transport.CodecSupport;
import com.alibaba.dubbo.remoting.transport.ExceedPayloadLimitException;

import java.io.IOException;
import java.io.InputStream;

/**
 * ExchangeCodec. MAGIC: 2字节, FLAG: 1字节, STATUS: 1字节, InvokeID: 8字节, Body_Length: 4字节, Body_Content: xxxx
 *
 * @author qianlei
 * @author william.liangf
 */
public class ExchangeCodec extends TelnetCodec {

    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header. 10进制: 55995, 2进制: 11011010 10111011 (1 byte = 8 bit, char 在java中是2个字节, java采用unicode，2个字节（16位）来表示一个字符, short 2个字节, int 4个字节, long 8个字节)
    protected static final short MAGIC = (short) 0xdabb;      // 二进制  1101 1010 1011 1011
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;   // 二进制 1000 0000
    protected static final byte FLAG_TWOWAY = (byte) 0x40;    // 二进制  100 0000
    protected static final byte FLAG_EVENT = (byte) 0x20;     // 二进制   10 0000
    protected static final int SERIALIZATION_MASK = 0x1f;     // 二进制    1 1111
    private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

    public Short getMagicCode() {
        return MAGIC;
    }

    public void encode(Channel channel, ChannelBuffer buffer, Object msg) throws IOException {
        if (msg instanceof Request) {
            encodeRequest(channel, buffer, (Request) msg);
        } else if (msg instanceof Response) {
            encodeResponse(channel, buffer, (Response) msg);
        } else {
            super.encode(channel, buffer, msg);
        }
    }

    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        int readable = buffer.readableBytes();                          // 获取 ChannelBuffer 可读数据的大小
        byte[] header = new byte[Math.min(readable, HEADER_LENGTH)];    // 读取 dubbo 数据包大小的数据
        buffer.readBytes(header);                                       // 从 ChannelBuffer 中读取数据到 header 中
        return decode(channel, buffer, readable, header);
    }

    /** 堆包头进行编码
     *
     * @param channel
     * @param buffer
     * @param readable  channelBuffer 中可读取数据包的大小
     * @param header    数据包头的大小
     * @return
     * @throws IOException
     */
    protected Object decode(Channel channel, ChannelBuffer buffer, int readable, byte[] header) throws IOException {
        // check magic number.
        // 对数据包的 magic 数字, 针对第一|二个字节的校验
        if (readable > 0 && header[0] != MAGIC_HIGH             // 检测 MAGIC_HIGH 与 MAGIC_LOW (2个字节 -> 11011010 10111011)
                || readable > 1 && header[1] != MAGIC_LOW) {
            int length = header.length;
            if (header.length < readable) {                     // 数据包的大小大于 包头大小
                header = Bytes.copyOf(header, readable);
                buffer.readBytes(header, length, readable - length);
            }
            for (int i = 1; i < header.length - 1; i++) {
                if (header[i] == MAGIC_HIGH && header[i + 1] == MAGIC_LOW) {  // 合适是真正的数据包的开始
                    buffer.readerIndex(buffer.readerIndex() - header.length + i); // 设置 ChannelBuffer 的 ReadIndex 从这里开始计算, 这里开始才是正真的数据包的开始
                    header = Bytes.copyOf(header, i);
                    break;
                }
            }
            return super.decode(channel, buffer, readable, header);
        }
        // 可读长度小于协议头长度, 说明: 可读字段都不够协议头的数据量, 直接返回 DecodeResult.NEED_MORE_INPUT
        // check length.
        if (readable < HEADER_LENGTH) {  // HEADER_LENGTH = 16
            return DecodeResult.NEED_MORE_INPUT;
        }

        // get data length.
        int len = Bytes.bytes2int(header, 12);  // 读取最后4位, 转换成 int
        checkPayload(channel, len);             // 检测数据包是否超过最大大小

        int tt = len + HEADER_LENGTH;           // 检测现在可读数据是否超过数据包的大小
        if (readable < tt) {                    // 可读的数据包大小还没有整个数据包的大小
            return DecodeResult.NEED_MORE_INPUT;
        }

        // limit input stream.
        ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, len);

        try {
            return decodeBody(channel, is, header);
        } finally {
            if (is.available() > 0) {
                try {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Skip input stream " + is.available());
                    }
                    StreamUtils.skipUnusedStream(is);  // 跳过那些没用的数据
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    // 对包体进行编码
    protected Object decodeBody(Channel channel, InputStream is, byte[] header) throws IOException {
        byte flag = header[2], proto = (byte) (flag & SERIALIZATION_MASK);  // header 中第三位是显示消息的类型,
        Serialization s = CodecSupport.getSerialization(channel.getUrl(), proto);   // 获取序列化机制
        ObjectInput in = s.deserialize(channel.getUrl(), is);               // 通过序列化机制, 统一封装的对象
        // get request id.
        long id = Bytes.bytes2long(header, 4);  // 获取请求的 Id 值, 8位 long 类型
        if ((flag & FLAG_REQUEST) == 0) {       // 显示是否是 request  类型的请求
            // decode response.
            Response res = new Response(id);    // 创建 Response
            if ((flag & FLAG_EVENT) != 0) {     // 是否是事件类型的请求
                res.setEvent(Response.HEARTBEAT_EVENT);
            }
            // get status.
            byte status = header[3];            // 数据的状态, 一个字节
            res.setStatus(status);
            if (status == Response.OK) {
                try {
                    Object data;
                    if (res.isHeartbeat()) {
                        data = decodeHeartbeatData(channel, in);
                    } else if (res.isEvent()) {
                        data = decodeEventData(channel, in);
                    } else {
                        data = decodeResponseData(channel, in, getRequestData(id));
                    }
                    res.setResult(data);
                } catch (Throwable t) {
                    res.setStatus(Response.CLIENT_ERROR);           // 设置 请求的状态
                    res.setErrorMessage(StringUtils.toString(t));   // 设置 处理结果
                }
            } else {
                res.setErrorMessage(in.readUTF());  // 直接设置错误信息
            }
            return res;
        } else {
            // decode request.
            Request req = new Request(id);
            req.setVersion("2.0.0");
            req.setTwoWay((flag & FLAG_TWOWAY) != 0);
            if ((flag & FLAG_EVENT) != 0) {
                req.setEvent(Request.HEARTBEAT_EVENT);
            }
            try {
                Object data;
                if (req.isHeartbeat()) {
                    data = decodeHeartbeatData(channel, in);
                } else if (req.isEvent()) {
                    data = decodeEventData(channel, in);
                } else {
                    data = decodeRequestData(channel, in);
                }
                req.setData(data);
            } catch (Throwable t) {
                // bad request
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    protected Object getRequestData(long id) {
        DefaultFuture future = DefaultFuture.getFuture(id);
        if (future == null)
            return null;
        Request req = future.getRequest();
        if (req == null)
            return null;
        return req.getData();
    }

    protected void encodeRequest(Channel channel, ChannelBuffer buffer, Request req) throws IOException {
        Serialization serialization = getSerialization(channel);
        // header.
        byte[] header = new byte[HEADER_LENGTH];      // 创建 请求包头
        // set magic number.
        Bytes.short2bytes(MAGIC, header);             // 设置 MAGIC 数据

        // set request and serialization flag.        // 设置请求标示
        header[2] = (byte) (FLAG_REQUEST | serialization.getContentTypeId());

        if (req.isTwoWay()) header[2] |= FLAG_TWOWAY; // 在请求标示中加入 twoWay | flagEvent
        if (req.isEvent()) header[2] |= FLAG_EVENT;

        // set request id.
        Bytes.long2bytes(req.getId(), header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH); // 设置 buffer 中属性的位置, 将 buffer 中 write的索引位置后移 12 位置
        ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
        ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
        if (req.isEvent()) {
            encodeEventData(channel, out, req.getData());
        } else {
            encodeRequestData(channel, out, req.getData()); // 写入 request 的数据
        }
        out.flushBuffer();
        bos.flush();
        bos.close();
        int len = bos.writtenBytes();
        checkPayload(channel, len);
        Bytes.int2bytes(len, header, 12);  // 在 header 中的最后加上 body 的长度

        // write
        buffer.writerIndex(savedWriteIndex);        // 将 buffer 的 writeIndex 前移动, 写入 header 中的数据
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len); // 设置一下 writeIndex 的位置
    }

    protected void encodeResponse(Channel channel, ChannelBuffer buffer, Response res) throws IOException {
        int savedWriteIndex = buffer.writerIndex();
        try {
            Serialization serialization = getSerialization(channel);
            // header.   创建 header
            byte[] header = new byte[HEADER_LENGTH];
            // set magic number.        在 header 中设置 MAGIC
            Bytes.short2bytes(MAGIC, header);
            // set request and serialization flag.
            header[2] = serialization.getContentTypeId();  // 设置 序列化标示位
            if (res.isHeartbeat()) header[2] |= FLAG_EVENT;
            // set response status.
            byte status = res.getStatus();  // 设置 返回数据的状态
            header[3] = status;
            // set request id.
            Bytes.long2bytes(res.getId(), header, 4);  // 在 header 中设置 请求的唯一标示

            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);  // 将 ChannelBuffer 的 writeIndex 后移动 12 位置
            ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
            ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
            // encode response data or error message.
            if (status == Response.OK) {
                if (res.isHeartbeat()) {
                    encodeHeartbeatData(channel, out, res.getResult());
                } else {
                    encodeResponseData(channel, out, res.getResult());
                }
            } else out.writeUTF(res.getErrorMessage());
            out.flushBuffer();
            bos.flush();
            bos.close();

            int len = bos.writtenBytes();           // 获取 body 数据的大小
            checkPayload(channel, len);
            Bytes.int2bytes(len, header, 12);
            // write
            buffer.writerIndex(savedWriteIndex);     // 设置上次 wirteIndex 的位置
            buffer.writeBytes(header); // write header.   写入 header 中的数据
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
        } catch (Throwable t) {
            // 将buffer内容清空
            buffer.writerIndex(savedWriteIndex);
            // 发送失败信息给Consumer，否则Consumer只能等超时了
            if (!res.isEvent() && res.getStatus() != Response.BAD_RESPONSE) {
                Response r = new Response(res.getId(), res.getVersion());
                r.setStatus(Response.BAD_RESPONSE);  // 设置 返回值状态

                if (t instanceof ExceedPayloadLimitException) {
                    logger.warn(t.getMessage(), t);
                    try {
                        r.setErrorMessage(t.getMessage());
                        channel.send(r);
                        return;
                    } catch (RemotingException e) {
                        logger.warn("Failed to send bad_response info back: " + t.getMessage() + ", cause: " + e.getMessage(), e);
                    }
                } else {
                    // FIXME 在Codec中打印出错日志？在IoHanndler的caught中统一处理？
                    logger.warn("Fail to encode response: " + res + ", send bad_response info instead, cause: " + t.getMessage(), t);
                    try {
                        r.setErrorMessage("Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
                        channel.send(r);
                        return;
                    } catch (RemotingException e) {
                        logger.warn("Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), e);
                    }
                }
            }

            // 重新抛出收到的异常
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }
    }

    @Override
    protected Object decodeData(ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    @Deprecated
    protected Object decodeHeartbeatData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeResponseData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Override
    protected void encodeData(ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    private void encodeEventData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Deprecated
    protected void encodeHeartbeatData(ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    protected void encodeRequestData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected void encodeResponseData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Override
    protected Object decodeData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(channel, in);
    }

    protected Object decodeEventData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Deprecated
    protected Object decodeHeartbeatData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in) throws IOException {
        return decodeResponseData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in, Object requestData) throws IOException {
        return decodeResponseData(channel, in);
    }

    @Override
    protected void encodeData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(channel, out, data);
    }

    private void encodeEventData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    @Deprecated
    protected void encodeHeartbeatData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeHeartbeatData(out, data);
    }

    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(out, data);
    }

}
