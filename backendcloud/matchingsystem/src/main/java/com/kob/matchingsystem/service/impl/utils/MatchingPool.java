package com.kob.matchingsystem.service.impl.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MatchingPool extends Thread{
    private static List<Player> players = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    private final static String startGameUrl = "http://127.0.0.1:3000/pk/start/game/";
    private static RestTemplate restTemplate;
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        MatchingPool.restTemplate = restTemplate;
    }
    public void addPlayer(Integer userId, Integer rating, Integer botId) {
        lock.lock();
        try {
            players.add(new Player(userId, rating, 0, botId));
        } finally {
            lock.unlock();
        }
    }
    public void remvoePlayer(Integer userId) {
        lock.lock();
        try {
            List<Player> newPlayers = new ArrayList<>();
            for (Player player: players) {
                if(!player.getUserId().equals(userId)) {
                    newPlayers.add(player);
                }
            }
            players = newPlayers;
        } finally {
            lock.unlock();
        }
    }

    private void increaseWatingTime() {
        for(Player player: players) {
            player.setWaitingTime(player.getWaitingTime() + 1);
        }
    }

    private void matchPlayers() {
//        System.out.println(players.toString());
        boolean[] used = new boolean[players.size()];
        //枚举老光棍
        for(int i = 0; i < players.size(); i ++ ) {
            if(used[i]) continue;
            for(int j = i + 1; j < players.size(); j ++ ) {
                if (used[j]) continue;
                Player a = players.get(i), b = players.get(j);
                if(checkMatched(a, b)) {
                    used[i] = used[j] = true;
                    sendResult(a, b);
                    break;
                }
            }
        }
        List<Player> newPlayers = new ArrayList<>();
        for(int i = 0 ; i < players.size(); i ++ ) {
            if(!used[i]) {
                newPlayers.add(players.get(i));
            }
        }
        players = newPlayers;
    }

    private boolean checkMatched(Player a, Player b) {
        int resultDelta = Math.abs(a.getRating() - b.getRating());
        int waitingTime = Math.min(a.getWaitingTime() , b.getWaitingTime());
        return resultDelta <= waitingTime * 10;
    }

    private void sendResult(Player a, Player b) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("a_id", String.valueOf(a.getUserId()));
        data.add("a_bot_id", String.valueOf(a.getBotId()));
        data.add("b_bot_id", String.valueOf(b.getBotId()));
        data.add("b_id", String.valueOf(b.getUserId()));
        restTemplate.postForObject(startGameUrl, data, String.class);
    }
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                lock.lock();
                try {
                    increaseWatingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
