package basic;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import state.IState;
import state.BaseStateMachine;

import static basic.BaseTask.TaskState.IDLE;
import static basic.BaseTask.TaskState.CANCEL;
import static basic.BaseTask.TaskState.FAIL;
import static basic.BaseTask.TaskState.PAUSE;
import static basic.BaseTask.TaskState.RUNNING;
import static basic.BaseTask.TaskState.SUCCEED;
import static basic.BaseTask.TaskState.WAIT;


/**
 * Created by zhaojian on 2017/9/12.
 */

public abstract class BaseTask<T extends TaskResult> {

    public enum TaskState implements IState {
        IDLE, WAIT, RUNNING, PAUSE, SUCCEED, FAIL,CANCEL;
    }

    private Handler handler = new Handler(Looper.getMainLooper());

    protected final Object taskLock = new Object();

    private TaskSubscriber taskSubscriber;

    protected Exception failException = null;

//    protected ReentrantLock lock = new ReentrantLock();
//    protected final Object block = new Object();
    /**
    *
    * child mast set right sate when doint task
    * */
    public final BaseStateMachine<TaskState> stateMachine = new BaseStateMachine<TaskState>() {
        @Override
        public TaskState startState() {
            return IDLE;
        }


        /**
         * SUCCEED, CANCEL, FAIL can only be changed to IDLE
         * */
        @Override
        public boolean onStateChanged(final Stack<TaskState> stateStack, final TaskState currentState) {
            if (currentState != IDLE && currentState != WAIT && (stateStack.peek() == SUCCEED || stateStack.peek() == CANCEL || stateStack.peek() == FAIL)) {
                return false;
            }
            switch (currentState) {
                case SUCCEED:
                    if (taskSubscriber != null && taskSubscriber.succeed != null) {
                        taskSubscriber.succeed.run();
                    }
                    synchronized (taskLock) {
                        taskLock.notifyAll();
                    }
                    break;
                case CANCEL:
                    if (taskSubscriber != null && taskSubscriber.cancel != null) {
                        taskSubscriber.cancel.run();
                    }
                    synchronized (taskLock) {
                        taskLock.notifyAll();
                    }
                    break;
                case FAIL:
                    if (taskSubscriber != null && taskSubscriber.fail != null) {
                        taskSubscriber.fail.run();
                    }
                    synchronized (taskLock) {
                        taskLock.notifyAll();
                    }
                    break;
                default:
                    break;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (TaskStateCallBack tTaskStateCallBack : stateCallBacks) {
                        switch (currentState) {
                            case WAIT:
                                tTaskStateCallBack.onWait();
                                break;
                            case RUNNING:
                                tTaskStateCallBack.onStart();
                                break;
                            case PAUSE:
                                tTaskStateCallBack.onPause();
                                break;
                            case SUCCEED:

                                tTaskStateCallBack.onSucceed(getResult());

                                break;
                            case CANCEL:
                                if (stateStack.peek() == RUNNING) {
                                    tTaskStateCallBack.onCancel(true);
                                } else {
                                    tTaskStateCallBack.onCancel(false);
                                }
//
                                break;
                            case FAIL:
                                tTaskStateCallBack.onFail(getException());
                                break;

                        }
                    }
                }
            });

            return true;
        }

        @Override
        public void onStateChangeFailed(Stack<TaskState> stateStack, TaskState illegalState) {
            for (TaskStateCallBack tTaskStateCallBack : stateCallBacks) {
                tTaskStateCallBack.onFail(new Exception("onStateChangeFailed, " + illegalState));
            }
        }
    };

    protected List<TaskStateCallBack<T>> stateCallBacks = Collections.synchronizedList(new ArrayList<TaskStateCallBack<T>>());

    public final String taskId = UUID.randomUUID().toString();

    public void setTaskSubscriber(TaskSubscriber taskSubscriber) {
        this.taskSubscriber = taskSubscriber;
    }

    public void setStateFail(Exception e) {
        failException = e;
        stateMachine.setCurrentState(FAIL);
    }

    //    protected final Object lock = new Object();
    public T execute() throws Exception {
        if (stateMachine.getCurrentState() == CANCEL) {
            throw new TaskCancelException();
        } else if (stateMachine.getCurrentState() != IDLE && stateMachine.getCurrentState() != WAIT) {
            throw new IllegalStateException("current state is " + stateMachine.getCurrentState() + ", task can not be execute");
        }
        stateMachine.setCurrentState(RUNNING);
        return getResult();
    }

    /**
     * 在execute之前执行，可以获取上一个task的结果
     * */
    protected void onPreTask(@Nullable BaseTask baseTask) {

    }

    protected void blockWait() throws Exception {
        synchronized (taskLock) {
            taskLock.wait();
            switch (stateMachine.getCurrentState()) {
                case RUNNING:
                case IDLE:
                case WAIT:
                case PAUSE:
                    break;
                case SUCCEED:
                    return;
                case CANCEL:
                    throw new TaskCancelException();
                case FAIL:
                    if (getException() != null) {
                        throw getException();
                    } else {
                        throw new Exception("unknown");
                    }
            }
        }
    }



//    /**
//    * 如果execute中的方法不是阻塞的，而是异步任务，则利用该方法阻塞
//    * */
//    protected void blockWait() throws Exception {
//
//        while (true) {
//            try {
//                Thread.sleep(10);
//                switch (stateMachine.getCurrentState()) {
//                    case RUNNING:
//                    case IDLE:
//                    case WAIT:
//                    case PAUSE:
//                        break;
//                    case SUCCEED:
//                        return;
//                    case CANCEL:
//                        throw new TaskCancelException();
//                    case FAIL:
//                        if (getException() != null) {
//                            throw getException();
//                        } else {
//                            throw new Exception("unknown");
//                        }
//                }
//            } catch (InterruptedException e) {
//
//            }
//        }
//    }


    public abstract void destroy();

    public abstract boolean cancel();
//    {
//        stateMachine.setCurrentState(CANCEL);
//    }

    public void pause() {
        stateMachine.setCurrentState(PAUSE);
    }

    public void inQueue() {
        stateMachine.setCurrentState(WAIT);
    }

    public void addStateCallBack(TaskStateCallBack<T> taskStateCallBack) {
        stateCallBacks.add(taskStateCallBack);
    }

    @Nullable
    public Exception getException() {
        return failException;
    }

    protected void setFailException(Exception e) {
        this.failException = e;
        stateMachine.setCurrentState(TaskState.FAIL);
    }

    protected void setFailException(String msg) {
        this.failException = new Exception(msg);
        stateMachine.setCurrentState(TaskState.FAIL);
    }

    @Nullable
    public abstract T getResult();

    public abstract float getProgress();


    public interface TaskStateCallBack<T extends TaskResult> {
        void onWait();
        void onStart();
        void onPause();
        void onSucceed(T result);
        void onFail(Throwable e);
        void onCancel(boolean isStated);
        void onFinish();
    }


    public static class TaskCancelException extends Exception {
        public TaskCancelException() {
            super("Task Cancel");
        }
    }
}
