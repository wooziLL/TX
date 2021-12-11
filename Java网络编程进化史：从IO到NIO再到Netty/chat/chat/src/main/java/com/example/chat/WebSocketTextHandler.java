package com.example.chat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class WebSocketTextHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // 是否握手成功，升级为 Websocket 协议
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            // 握手成功，移除 HttpRequestHandler，因此将不会接收到任何消息
            // 并把握手成功的 Channel 加入到 ChannelGroup 中
            new SocketSession(ctx.channel());
        } else if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if (stateEvent.state() == IdleState.READER_IDLE) {
                System.out.println("bb22");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        SocketSession session = SocketSession.getSession(channelHandlerContext);
        TypeToken<HashMap<String, String>> typeToken = new TypeToken<HashMap<String, String>>() {
        };

        Gson gson=new Gson();
        java.util.Map<String,String> map = gson.fromJson(textWebSocketFrame.text(), typeToken.getType());
        User user = null;
        switch (map.get("type")) {
            case "msg":
                Map<String, String> result = new HashMap<>();
                user = session.getUser();
                result.put("type", "msg");
                result.put("msg", map.get("msg"));
                result.put("sendUser", user.getNickname());
                SessionGroup.inst().sendToOthers(result, session);
                break;
            case "init":
                String room = map.get("room");
                session.setGroup(room);
                String nick = map.get("nick");
                user = new User(session.getId(), nick);
                session.setUser(user);
                SessionGroup.inst().addSession(session);
                break;
        }
    }
}

