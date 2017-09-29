package basic;

/**
 * Created by zhaojian on 2017/9/19.
 */

public class LinkedTaskQueueBuilder extends TaskQueueBuilder {

    /**
     * 失败后，剩余task不继续执行
     * */
    @Override
    protected void doOnError(Throwable e) {
//        super.doOnError(e);
//        currentTask.stateMachine.setCurrentState(BaseTask.TaskState.FAIL);
        if (taskChainStateCallBack != null) {
            taskChainStateCallBack.onFail(currentTask, e);
        }
        currentTask.stateMachine.setCurrentState(BaseTask.TaskState.FAIL);

    }


    @Override
    public synchronized void pause() {
        super.pause();
    }

    @Override
    public synchronized void resumeTask(BaseTask task) {
        super.resumeTask(task);
    }
}
