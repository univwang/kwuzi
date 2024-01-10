package com.kob.backend.consumer.utils;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.locks.ReentrantLock;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Timer {
    // 用于计时
    private long time = 60; //默认60s
    private boolean isStart = false;
    private String uuid;
    private ReentrantLock lock = new ReentrantLock();
    // 开启
    public void start() {
        try {
            lock.lock();
            time = 60;
            isStart = true;
        } finally {
            lock.unlock();
        }
    }
    public void run() {
        new Thread(() -> {
            while (time > 0) {
                try {
                    Thread.sleep(1000);
                    lock.lock();
                    if (isStart) {
                        time--;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    // 停止
    public void stop() {
        try {
            lock.lock();
            isStart = false;
        } finally {
            lock.unlock();
        }
    }
}
