package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread {
    final private Integer rows;
    final private Integer cols;
    final private int[][] g;
    private final Player playerA, playerB;

    private int order = 0; //0是A(黑) 1是B（白）
    //两个线程同时读写
    private Integer nextStepA = null;
    private Integer nextStepB = null;

    private ReentrantLock lock = new ReentrantLock();

    private final static String addBotUrl = "http://127.0.0.1:3002/bot/add/";

    private String status = "playing";
    private String loser = "";

    public Game(Integer rows, Integer cols, Integer idA, Integer idB, Bot botA, Bot botB, String uuidA, String uuidB) {

        Integer botIdA = -1, botIdB = -1;
        String botCodeA = "", botCodeB = "";
        if (botA != null) {
            botIdA = botA.getId();
            botCodeA = botA.getContent();
        }
        if (botB != null) {
            botIdB = botB.getId();
            botCodeB = botB.getContent();
        }
        playerA = new Player(idA, 0, new ArrayList<>(), botIdA, botCodeA, new Timer());
        playerB = new Player(idB, 1, new ArrayList<>(), botIdB, botCodeB, new Timer());
        playerA.timer.setUuid(uuidA);
        playerB.timer.setUuid(uuidB);
        this.rows = rows;
        this.cols = cols;
        this.g = new int[rows][cols];
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public int[][] getG() {
        return g;
    }

    private void updateUserRating(Player player, Integer rating) {
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }

    private void saveToDatabase() {
        Integer ratingA = WebSocketServer.userMapper.selectById(playerA.getId()).getRating();
        Integer ratingB = WebSocketServer.userMapper.selectById(playerB.getId()).getRating();
        if (loser.equals("A")) {
            ratingA -= 2;
            ratingB += 5;
        } else if (loser.equals("B")) {
            ratingA += 5;
            ratingB -= 2;
        }
        updateUserRating(playerA, ratingA);
        updateUserRating(playerB, ratingB);
        Record record = new Record(
                null,
                playerA.getId(),
                playerB.getId(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );
        WebSocketServer.recordMapper.insert(record);
    }

    private String getMapString() {
        //地图信息 0空 1是黑子 2是白子
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(g[i][j]);
            }
        }
        return sb.toString();
    }

    public void createMap() {

    }

    private String getInput(Player player) {
        //地图信息#
        return order + "#" + getMapString();
    }

    private void sendBotCode(Player player) {
        if (player.getBotId() == -1) {
            return;
        }
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", String.valueOf(player.getId()));
        data.add("bot_code", player.getBotCode());
        data.add("input", getInput(player));
        System.out.println(getInput(player));
        WebSocketServer.restTemplate.postForObject(addBotUrl, data, String.class);
    }

    private boolean nextStep() {
        //等待两名玩家的下一步操作
        //每一步200ms，前端画的比较慢，所以这里等待200ms
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (order == 0) {
            sendBotCode(playerA);
        } else {
            sendBotCode(playerB);
        }
        //如果order对应的player没有超时，就等待
        while (true) {
            //如果order对应的player没有超时，就等待
            if (order == 0 && playerA.getTimer().getTime() > 0 || order == 1 && playerB.getTimer().getTime() > 0) {
                try {
                    Thread.sleep(200);
                    //判断
                    lock.lock();
                    try {
                        //轮到A
                        if (order == 0 && nextStepA != null) {
                            //判断地图是否为空
                            int x = nextStepA / this.cols, y = nextStepA % this.cols;
                            if (g[x][y] != 0) {
                                continue;
                            }
                            playerA.getSteps().add(nextStepA);
                            //接收到了输入，停止计时
                            playerA.timer.stop();

                            return true;
                        }
                        //轮到B
                        if (order == 1 && nextStepB != null) {
                            int x = nextStepB / this.cols, y = nextStepB % this.cols;
                            if (g[x][y] != 0) {
                                continue;
                            }
                            playerB.getSteps().add(nextStepB);
                            //接收到了输入，停止计时
                            playerB.timer.stop();
                            return true;
                        }
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                break;
            }
        }
        return false;
    }

    private void judge() {
        //判断输赢
        //地图信息 0空 1是黑子 2是白子
        //五子棋判断输赢
        //横向
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols - 4; j++) {
                if (g[i][j] != 0 && g[i][j] == g[i][j + 1] && g[i][j] == g[i][j + 2] && g[i][j] == g[i][j + 3] && g[i][j] == g[i][j + 4]) {
                    if (g[i][j] == 1) {
                        loser = "B";
                    } else {
                        loser = "A";
                    }
                    status = "finished";
                    return;
                }
            }
        }
        //纵向
        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows - 4; i++) {
                if (g[i][j] != 0 && g[i][j] == g[i + 1][j] && g[i][j] == g[i + 2][j] && g[i][j] == g[i + 3][j] && g[i][j] == g[i + 4][j]) {
                    if (g[i][j] == 1) {
                        loser = "B";
                    } else {
                        loser = "A";
                    }
                    status = "finished";
                    return;
                }
            }
        }
        //斜向
        for (int i = 0; i < rows - 4; i++) {
            for (int j = 0; j < cols - 4; j++) {
                if (g[i][j] != 0 && g[i][j] == g[i + 1][j + 1] && g[i][j] == g[i + 2][j + 2] && g[i][j] == g[i + 3][j + 3] && g[i][j] == g[i + 4][j + 4]) {
                    if (g[i][j] == 1) {
                        loser = "B";
                    } else {
                        loser = "A";
                    }
                    status = "finished";
                    return;
                }
            }
        }

        //斜向
        for (int i = 0; i < rows - 4; i++) {
            for (int j = 4; j < cols; j++) {
                if (g[i][j] != 0 && g[i][j] == g[i + 1][j - 1] && g[i][j] == g[i + 2][j - 2] && g[i][j] == g[i + 3][j - 3] && g[i][j] == g[i + 4][j - 4]) {
                    if (g[i][j] == 1) {
                        loser = "B";
                    } else {
                        loser = "A";
                    }
                    status = "finished";
                    return;
                }
            }
        }

    }

    private void sendMove() {
        //发送移动
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            if (order == 0) {
                resp.put("a_direction", nextStepA);
            } else {
                resp.put("b_direction", nextStepB);
            }
            //清空
            nextStepA = nextStepB = null;
            order = 1 - order;

            sendAllMessage(resp.toJSONString());
            //开始记时
            if (order == 0) {
                playerA.getTimer().start();
            } else {
                playerB.getTimer().start();
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendResult() {
        //发送结果
        JSONObject result = new JSONObject();
        result.put("event", "result");
        result.put("loser", loser);
        saveToDatabase();
        sendAllMessage(result.toJSONString());
    }

    private void sendAllMessage(String message) {
        //发送所有信息
        if (WebSocketServer.users.get(playerA.getId()) != null && playerA.getTimer().getUuid().equals(WebSocketServer.users.get(playerA.getId()).uuid))
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        if (WebSocketServer.users.get(playerB.getId()) != null && playerB.getTimer().getUuid().equals(WebSocketServer.users.get(playerB.getId()).uuid))
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }

    private void sendTime() {
        //发送时间
        JSONObject time = new JSONObject();
        time.put("event", "time");
        try {
            playerA.getTimer().getLock().lock();
            if (playerA.timer.isStart() && playerA.getTimer().getUuid().equals(WebSocketServer.users.get(playerA.getId()).uuid)) {
                time.put("a_time", playerA.getTimer().getTime());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            playerA.getTimer().getLock().unlock();
        }
        try {
            playerB.getTimer().getLock().lock();
            if (playerB.timer.isStart() && playerB.getTimer().getUuid().equals(WebSocketServer.users.get(playerB.getId()).uuid)) {
                time.put("b_time", playerB.getTimer().getTime());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            playerB.getTimer().getLock().unlock();
        }
        sendAllMessage(time.toJSONString());
    }

    @Override
    public void run() {
        //一回合一回合的操作
        //首先应该
        //每500ms发送一次时间
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    sendTime();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        for (int i = 0; i < 1000; i++) {
            //每一回合
            //等待两名玩家的输入
            if (nextStep()) {
                //更新地图
                updateG();
                //判断输赢
                judge();
                //如果还在进行中
                if (status.equals("playing")) {
                    //广播两名玩家的输入
                    sendMove();
                } else {
                    sendMove();
                    sendResult();
                    playerA.timer.setStart(false);
                    playerB.timer.setStart(false);
                    break;
                }
            } else {
                status = "finished";
                playerA.timer.setStart(false);
                playerB.timer.setStart(false);
                lock.lock();
                try {
                    if (nextStepA == null && order == 0) {
                        loser = "A";
                    } else if (nextStepB == null && order == 1) {
                        loser = "B";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }
        }
        super.run();
    }

    private void updateG() {
        //更新地图
        //地图信息 0空 1是黑子 2是白子
        for (int i = 0; i < playerA.getSteps().size(); i++) {
            int d = playerA.getSteps().get(i);

            int x = d / this.cols, y = d % this.cols;
            g[x][y] = 1;
        }
        for (int i = 0; i < playerB.getSteps().size(); i++) {
            int d = playerB.getSteps().get(i);
            int x = d / this.cols, y = d % this.cols;
            g[x][y] = 2;
        }
    }
}
