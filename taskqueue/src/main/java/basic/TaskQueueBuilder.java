package basic;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zhaojian on 2017/9/12.
 */

public class TaskQueueBuilder {

    protected final LinkedHashMap<String, BaseTask> taskMap = new LinkedHashMap<>();
    protected final Map<String, TaskSubscriber> subscriberMap = new HashMap<>();
    private final ConcurrentLinkedQueue<String> workingQueue = new ConcurrentLinkedQueue<>();


    private final List<String> consumedPool = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> failedPool = Collections.synchronizedList(new ArrayList<String>());
    private final List<String> cancelPool = Collections.synchronizedList(new ArrayList<String>());

//    private final ConcurrentLinkedQueue<String> consumedQueue = new ConcurrentLinkedQueue<>();
//    private final ConcurrentLinkedQueue<String> failQueue = new ConcurrentLinkedQueue<>();
//    private final ConcurrentLinkedQueue<String> cancelQueue = new ConcurrentLinkedQueue<>();
    private boolean isPaused = false;

    protected final static int MSG_CONSUME = 0;
    protected final Handler consumeHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            consume();
            return false;
        }
    });
//    private ConcurrentLinkedQueue<String> consumeQueue = new ConcurrentLinkedQueue<>();

    protected BaseTask currentTask = null;

    protected TaskChainStateCallBack taskChainStateCallBack;

    public synchronized TaskSubscriber add(@NonNull BaseTask baseTask) {
        taskMap.put(baseTask.taskId, baseTask);
        workingQueue.add(baseTask.taskId);
        TaskSubscriber taskSubscriber = new TaskSubscriber(this);
        subscriberMap.put(baseTask.taskId, taskSubscriber);
        baseTask.inQueue();
        baseTask.setTaskSubscriber(taskSubscriber);
        return taskSubscriber;
    }

    /**
     * 消费 workingQueue 中的任务
     * */
    protected synchronized void consume() {
        if (isPaused) {
            return;
        }
        if (workingQueue.isEmpty()) {
            if (taskChainStateCallBack != null) {
                taskChainStateCallBack.onSucceed();
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<BaseTask>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<BaseTask> e) throws Exception {

//                String id = workingQueue.poll();
//                currentTask = taskMap.get(id);
                BaseTask preTask = currentTask;
                currentTask = peekTask();
                if (currentTask == null) {
                    return;
                }
                if (taskChainStateCallBack != null) {
                    taskChainStateCallBack.onNextTask(currentTask);
                }
                currentTask.onPreTask(preTask);
                currentTask.execute();
                e.onNext(currentTask);
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(new Observer<Object>() {

                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Object o) {
//                        pollTask();
//                        if (currentTask != null) {
//                            currentTask.stateMachine.setCurrentState(BaseTask.TaskState.SUCCEED);
//                        }
//                        if (taskChainStateCallBack != null) {
//                            taskChainStateCallBack.onTaskSucceed(currentTask);
//                        }
//                        execute();
                        doOnSucceed();
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        synchronized (taskMap) {
                            if (e instanceof BaseTask.TaskCancelException) {
                                doOnCancel((BaseTask.TaskCancelException)e);
                            } else {
                                doOnError(e);
//                                taskMap.forEach((s, honeyTask) -> {
//                                    honeyTask.stateMachine.setCurrentState(BaseTask.TaskState.FAIL);
//                                });
                            }


                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 一个Task 完成后执行
     * */
    protected void doOnSucceed() {
        pollTask();
        if (currentTask != null) {
            currentTask.stateMachine.setCurrentState(BaseTask.TaskState.SUCCEED);
        }
        if (taskChainStateCallBack != null) {
            taskChainStateCallBack.onTaskSucceed(currentTask);
        }
        execute();
    }

    /**
     * 一个Task cancel 后执行
     * */
    protected void doOnCancel(BaseTask.TaskCancelException cancelException) {
        currentTask.stateMachine.setCurrentState(BaseTask.TaskState.CANCEL);
//        consume();
    }

    /**
     * 一个Task 失败 后执行
     * */
    protected void doOnError(Throwable e) {
        castOffTask();
        currentTask.stateMachine.setCurrentState(BaseTask.TaskState.FAIL);
        if (taskChainStateCallBack != null) {
            taskChainStateCallBack.onFail(currentTask, e);
        }
        consume();
    }

    public synchronized void execute() {

        if (currentTask == null) {
            consumeHandler.removeMessages(MSG_CONSUME);
            consumeHandler.sendEmptyMessage(MSG_CONSUME);
        } else if (currentTask.stateMachine.getCurrentState() == BaseTask.TaskState.RUNNING) {
            return;
        } else {
            consumeHandler.removeMessages(MSG_CONSUME);
            consumeHandler.sendEmptyMessage(MSG_CONSUME);
        }
    }

    private synchronized BaseTask pollTask() {
        if (workingQueue.isEmpty()) {
            return null;
        }
        String id = workingQueue.poll();
//        consumedQueue.add(id);
        consumedPool.add(id);
        return taskMap.get(id);
    }

    public boolean isPaused() {
        return isPaused;
    }

    private synchronized BaseTask castOffTask() {
        if (workingQueue.isEmpty()) {
            return null;
        }
        String id = workingQueue.poll();
        failedPool.add(id);
//        failQueue.add(id);
        return taskMap.get(id);
    }

    private synchronized BaseTask peekTask() {
        if (workingQueue.isEmpty()) {
            return null;
        }
        String id = workingQueue.peek();
        return taskMap.get(id);
    }

    public synchronized void removeTask(BaseTask task) {
        if (task == null) {
            return;
        }
        task.destroy();
        workingQueue.remove(task.taskId);
        consumedPool.remove(task.taskId);
        failedPool.remove(task.taskId);
        cancelPool.remove(task.taskId);
        taskMap.remove(task.taskId);
    }

    /**
     * 取消一个task，队列中剩余的task会继续执行
     * */
    public synchronized void cancelTask(BaseTask task) {
        if (task == null) {
            return;
        }
        task.cancel();
        workingQueue.remove(task.taskId);
        cancelPool.add(task.taskId);
//        execute();
        consume();
    }

    /**
     * 恢复一个task，放在任务队列的最后
     * */
    public synchronized void resumeTask(BaseTask task) {
        if (task == null) {
            return;
        }
        cancelPool.remove(task.taskId);
        failedPool.remove(task.taskId);
        add(task);
        execute();
    }


    /**
     * 取消当前正在执行的task，其他任务不会继续执行
     * */

    public synchronized void pause() {
        if (currentTask == null) {
            return;
        }
//        cancel();
        isPaused = true;
        currentTask.cancel();

        consumeHandler.removeMessages(MSG_CONSUME);
    }

    /**
     * 恢复取消状态
     * */
    public synchronized void resume() {
        isPaused = false;
        if (currentTask.stateMachine.getCurrentState() == BaseTask.TaskState.RUNNING) {
            isPaused = false;
        } else {
            currentTask.stateMachine.setCurrentState(BaseTask.TaskState.IDLE);
            execute();
        }

    }

//    public synchronized void cancel() {
//        taskMap.forEach((s, honeyTask) -> honeyTask.cancel());
//        consumeHandler.removeMessages(MSG_CONSUME);
//    }

//    public synchronized void pause() {
////        cancel();
//        isPaused = true;
//        currentTask.cancel();
//        consumeHandler.removeMessages(MSG_CONSUME);
//    }
//
//    public synchronized void resume() {
//        isPaused = false;
//        if (currentTask.stateMachine.getCurrentState() == BaseTask.TaskState.RUNNING) {
//            isPaused = false;
//        } else if (currentTask.stateMachine.getCurrentState() == BaseTask.TaskState.FAIL
//                || currentTask.stateMachine.getCurrentState() == BaseTask.TaskState.CANCEL) {
//            execute();
//        }
//    }

    public synchronized void destroy() {
        for (BaseTask baseTask : taskMap.values()) {
            baseTask.destroy();
        }
        workingQueue.clear();
        consumedPool.clear();
        failedPool.clear();
        cancelPool.clear();
        taskMap.clear();
    }

    public synchronized BaseTask getCurrentTask() {
        return currentTask;
    }

    public synchronized List<BaseTask> listTasks() {
        List<BaseTask> list = new ArrayList<>();
        list.addAll(taskMap.values());
        return list;
    }

    public void setTaskChainStateCallBack(TaskChainStateCallBack taskChainStateCallBack) {
        this.taskChainStateCallBack = taskChainStateCallBack;
    }

    public interface TaskChainStateCallBack {
        void onCancel(BaseTask task, Throwable e);
        void onFail(BaseTask task, Throwable e);
        void onNextTask(BaseTask task);
        void onTaskSucceed(BaseTask task);
        void onSucceed();
    }
}
