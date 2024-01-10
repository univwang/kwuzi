package com.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread{

    private final static ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition(); //条件变量
    private final Queue<Bot> bots = new LinkedList<>();

    public void addBot(Integer userId, String botCode, String input) {
        lock.lock();
        try {
            bots.add(new Bot(userId, botCode, input));
            condition.signalAll(); //缓存所有线程
        } finally {
            lock.unlock();
        }
    }
    public void consume(Bot bot) {
        //比较简单，使用java代码
        //可以使用docker沙箱
        Consumer consumer = new Consumer();
        consumer.startTimeout(50 * 1000, bot);

    }
    @Override
    public void run() {
        while (true) {
            lock.lock();
            if (bots.isEmpty()) {
                try {
                    condition.await();
                    //自动锁释放
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    break;
                }
            } else {
                Bot bot = bots.remove();
                lock.unlock();
                consume(bot);//比较耗时
            }
        }
    }
}
