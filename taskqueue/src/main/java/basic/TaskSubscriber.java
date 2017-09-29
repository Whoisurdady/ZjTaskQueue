package basic;

/**
 * Created by zhaojian on 2017/9/12.
 */

public class TaskSubscriber {

    TaskQueueBuilder builder;

    Runnable succeed = null;
    Runnable fail = null;
    Runnable cancel = null;


    public TaskSubscriber(TaskQueueBuilder builder) {
        this.builder = builder;
    }

    public TaskSubscriber onSucceed(Runnable runnable) {
        succeed = runnable;
        return this;
    }
    public TaskSubscriber onFail(Runnable runnable) {
        fail = runnable;
        return this;
    }
    public TaskSubscriber onCancel(Runnable runnable) {
        cancel = runnable;
        return this;
    }

    public TaskQueueBuilder build() {
        return builder;
    }
}
