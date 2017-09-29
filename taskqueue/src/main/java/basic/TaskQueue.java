package basic;

/**
 * Created by zhaojian on 2017/9/12.
 */

public class TaskQueue {


    public static TaskQueueBuilder getBuilder() {
        return new TaskQueueBuilder();
    }

    public static TaskQueueBuilder getLinkedBuilder() { return new LinkedTaskQueueBuilder();}

}
