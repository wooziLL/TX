package com.example.chat;

import com.google.gson.Gson;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionGroup {

    private static SessionGroup singleInstance = new SessionGroup();

    // 组的映射
    private ConcurrentHashMap<String, ChannelGroup> groupMap = new ConcurrentHashMap<>();

    public static SessionGroup inst() {
        return singleInstance;
    }

    public void shutdownGracefully() {

        Iterator<ChannelGroup> groupIterator = groupMap.values().iterator();
        while (groupIterator.hasNext()) {
            ChannelGroup group = groupIterator.next();
            group.close();
        }
    }

    public void sendToOthers(Map<String, String> result, SocketSession s) {
        // 获取组
        ChannelGroup group = groupMap.get(s.getGroup());
        if (null == group) {
            return;
        }
        Gson gson=new Gson();
        String json = gson.toJson(result);
        // 自己发送的消息不返回给自己
//      Channel channel = s.getChannel();
        // 从组中移除通道
//      group.remove(channel);
        ChannelGroupFuture future = group.writeAndFlush(new TextWebSocketFrame(json));
        future.addListener(f -> {
            System.out.println("完成发送："+json);
//          group.add(channel);//发送消息完毕重新添加。

        });
    }

    public void addSession(SocketSession session) {

        String groupName = session.getGroup();
        if (StringUtils.isEmpty(groupName)) {
            // 组为空，直接返回
            return;
        }
        ChannelGroup group = groupMap.get(groupName);
        if (null == group) {
            group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
            groupMap.put(groupName, group);
        }
        group.add(session.getChannel());
    }

    /**
     * 关闭连接， 关闭前发送一条通知消息
     */
    public void closeSession(SocketSession session, String echo) {
        ChannelFuture sendFuture = session.getChannel().writeAndFlush(new TextWebSocketFrame(echo));
        sendFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                System.out.println("关闭连接："+echo);
                future.channel().close();
            }
        });
    }

    /**
     * 关闭连接
     */
    public void closeSession(SocketSession session) {

        ChannelFuture sendFuture = session.getChannel().close();
        sendFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                System.out.println("发送所有完成："+session.getUser().getNickname());
            }
        });

    }

    /**
     * 发送消息
     * @param ctx 上下文
     * @param msg 待发送的消息
     */
    public void sendMsg(ChannelHandlerContext ctx, String msg) {
        ChannelFuture sendFuture = ctx.writeAndFlush(new TextWebSocketFrame(msg));
        sendFuture.addListener(f -> {//发送监听
            System.out.println("对所有发送完成："+msg);
        });
    }
}

