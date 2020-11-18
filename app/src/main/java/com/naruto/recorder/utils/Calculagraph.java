package com.naruto.recorder.utils;

/**
 * @Purpose 计时器
 * @Author Naruto Yang
 * @CreateDate 2020/11/14 0014
 * @Note
 */
public abstract class Calculagraph {
    public static final int STATE_READY = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;
    private int hours;//时
    private int minutes;//分
    private int seconds;//秒
    private int milliseconds;//毫秒
    private long pauseTime;//记录改变状态的时间，用于计算由于thread与按钮响应不同步所导致的时间差

    private int state = STATE_READY;
    private Runnable runnable;
    private boolean needPause = false;//控制runnable
    private boolean needDestroy = false;
    private int interval = 123;//刷新间隔（ms)

    /**
     * 更新UI
     *
     * @param hours
     * @param minutes
     * @param seconds
     */
    protected abstract void updateUI(int hours, int minutes, int seconds, int milliseconds);

    private void startThread() {
        if (runnable == null) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (needPause) {
                            addMilliseconds(interval - (System.currentTimeMillis() - pauseTime));
                        } else {
                            addMilliseconds(interval);
                            updateUI(hours, minutes, seconds, milliseconds);
                        }
                    } while (!needPause);
                    if (needDestroy) runnable = null;
                }
            };
        }
        needPause = false;
        pauseTime = 0;
        setState(STATE_RUNNING);
        new Thread(runnable).start();
    }

    /**
     * 开始
     */
    public void start() {
        startThread();
    }

    /**
     * 暂停
     */
    public void pause() {
        needPause = true;
        pauseTime = System.currentTimeMillis();
        setState(STATE_PAUSED);
    }

    /**
     * 继续
     */
    public void resume() {
        startThread();
    }

    /**
     * 重新开始
     */
    public void restart() {
        reset();
        start();
    }

    /**
     * 重置
     */
    public void reset() {
        hours = minutes = seconds = 0;
        milliseconds = 0;
        updateUI(hours, minutes, seconds, milliseconds);
    }

    /**
     * 销毁
     */
    public void destroy() {
        needDestroy = true;
        pause();
    }


    private void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * 刷新毫秒数
     *
     * @param offsetTime
     */
    private void addMilliseconds(long offsetTime) {
        milliseconds += offsetTime;
        if (milliseconds > 1000) {
            addSeconds((int) (milliseconds / 1000));
            milliseconds %= 1000;
        }
    }

    /**
     * 刷新秒数
     *
     * @param offsetTime
     */
    private void addSeconds(int offsetTime) {
        seconds += offsetTime;
        if (seconds > 60) {
            addMinutes(seconds / 60);
            seconds %= 60;
        }
    }

    /**
     * 刷新分钟数
     *
     * @param offsetTime
     */
    private void addMinutes(int offsetTime) {
        minutes += offsetTime;
        if (minutes > 60) {
            addHours(minutes / 60);
            minutes %= 60;
        }
    }

    /**
     * 刷新小时数
     *
     * @param offsetTime
     */
    private void addHours(int offsetTime) {
        hours += offsetTime;
    }

}
