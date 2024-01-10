package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Component
@ServerEndpoint("/websocket/pk/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {


    // 将timer的uuid和对应的WebSocketServer绑定
    final public static ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>();
    private User user;
    private Session session = null;

    public String uuid = UUID.randomUUID().toString();

    public static UserMapper userMapper;
    public static RecordMapper recordMapper;

    private static BotMapper botMapper;
    public Game game = null;

    public static RestTemplate restTemplate;

    private  final static String addPlayerUrl = "http://127.0.0.1:3001/player/add/";
    private  final static String removePlayerUrl = "http://127.0.0.1:3001/player/remove/";

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    @Autowired
    public void setRecordMapper(RecordMapper recordMapper) {
        WebSocketServer.recordMapper = recordMapper;
    }

    @Autowired
    public void setBotMapper(BotMapper botMapper) {
        WebSocketServer.botMapper = botMapper;
    }
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        //如果加了autowired，检查唯一的一个@Bean函数，初始化后复制过来
        WebSocketServer.restTemplate = restTemplate;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        this.session = session;
        Integer userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if (this.user != null) {
            users.put(userId, this);
        } else {
            this.session.close();
        }

    }

    @OnClose
    public void onClose() {
        if (this.user != null) {
            users.remove(this.user.getId());
        }
    }
    public static void startGame(Integer aId, Integer bId, Integer aBotId, Integer bBotId) {
        System.out.println("start game!");

        User a = userMapper.selectById(aId);
        User b = userMapper.selectById(bId);

        Bot botA = botMapper.selectById(aBotId);
        Bot botB = botMapper.selectById(bBotId);
        String uuid_A = WebSocketServer.users.get(a.getId()).uuid;
        String uuid_B = WebSocketServer.users.get(b.getId()).uuid;
        Game game = new Game(
                15,
                15,
                a.getId(),
                b.getId(),
                botA,
                botB,
                uuid_A,
                uuid_B
                );
        game.createMap();
        if(users.get(a.getId()) != null)
            users.get(a.getId()).game = game;
        if(users.get(b.getId()) != null)
            users.get(b.getId()).game = game;

        JSONObject respGame = new JSONObject();
        respGame.put("a_id", game.getPlayerA().getId());
        respGame.put("b_id", game.getPlayerB().getId());
        respGame.put("map", game.getG());

        JSONObject respA = new JSONObject();
        respA.put("event", "start-matching");
        respA.put("opponent_username", b.getUsername());
        respA.put("opponent_photo", b.getPhoto());
        respA.put("game", respGame);
        if (users.get(a.getId()) != null)
            users.get(a.getId()).sendMessage(respA.toJSONString());

        JSONObject respB = new JSONObject();
        respB.put("event", "start-matching");
        respB.put("opponent_username", a.getUsername());
        respB.put("opponent_photo", a.getPhoto());
        respB.put("game", respGame);
        if(users.get(b.getId()) != null)
            users.get(b.getId()).sendMessage(respB.toJSONString());

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        game.start();
        // 开始游戏
        game.getPlayerA().timer.run();
        game.getPlayerA().timer.start();
        game.getPlayerB().timer.run();

    }
    private void startMatching(Integer botId) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        data.add("rating", this.user.getRating().toString());
        data.add("bot_id", botId.toString());
        restTemplate.postForObject(addPlayerUrl, data, String.class);
    }

    private void stopMatching() {
        LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    @OnMessage
    public void onMessage(String message, Session session) {  // 当做路由
        JSONObject data = JSONObject.parseObject(message);
        String event = data.getString("event");
        if ("start-matching".equals(event)) {
            startMatching(data.getInteger("bot_id"));
        } else if ("stop-matching".equals(event)) {
            stopMatching();
        } else if("move".equals(event)) {
            move(data.getInteger("direction"));
        }
    }

    private void move(int direction) {
        if(game.getPlayerA().getId().equals(user.getId())) {
            if(game.getPlayerA().getBotId().equals(-1))
                game.setNextStepA(direction);
        } else if(game.getPlayerB().getId().equals(user.getId())) {
            if(game.getPlayerB().getBotId().equals(-1))
                game.setNextStepB(direction);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}