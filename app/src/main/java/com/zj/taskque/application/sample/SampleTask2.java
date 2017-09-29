package com.zj.taskque.application.sample;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import basic.BaseTask;
import basic.TaskResult;

/**
 * Created by zhaojian on 2017/9/29.
 */

public class SampleTask2 extends BaseTask {
    private final String[] strings = {"测试", "耗时", "任务"};

    private Handler taskHandler;

    public SampleTask2(Handler taskHandler) {
        this.taskHandler = taskHandler;
    }

    @Override
    public TaskResult execute() throws Exception {
        super.execute();
        for (String s : strings) {
            if (taskHandler != null) {
                Message msg = taskHandler.obtainMessage();
                msg.what = 0;
                msg.obj = s;
                taskHandler.sendMessage(msg);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    setFailException(e);
                }
            }
        }
        return getResult();
    }

    @Override
    public void destroy() {
        taskHandler = null;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Nullable
    @Override
    public TaskResult getResult() {
        return null;
    }

    @Override
    public float getProgress() {
        return 0;
    }
}
