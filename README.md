# ZjTaskQueue
新项目，可以用，不过只能clone下来自己导入
先把之前写的东西拿过来share一下，之后完善文档


导入
	clone 项目，引入module taskque

使用方法:

	1.添加任务
	TaskChain.getBuilder().add(BaseTask task).build().add().build()....build().execute();

	可以在任意时刻，给builder add task并execute，如果没有任务，task会被立即执行，如果有任务，会将task放在任务队列的最后执行
	2.针对每个人的完成状态，做回调
	TaskQueue.getBuilder().add(BaseTask task).onSucceed(Runnable).onFail(Runnable).onCancel(Runnable).build().execute();

	注：这个builder，如果中间有任务被cancel或者失败了，会跳过失败的任务继续执行
	如果希望在中间任务失败后，整个任务链不继续进行，使用 TaskChain.getLinkedBuilder();
	其他功能，参考代码中注释

	3.BaseTask
	最主要是复写execute方法
	每个BaseTask的实例，都会利用RxJava异步执行execute方法。所以主要由两种情况:
	case 1：
	  execute中执行阻塞（非异步）的耗时操作，则可以在execute中运行耗时方法即可。如果中途任务失败，扔异常出来。如果中途要取消任务，扔TaskCancelException即可。
	case 2：
	  execute中执行异步操作，利用CallBack返回执行状态。
	  在这种情况下，需要在启动异步任务后，调用blockWait（）方法，阻塞住execute，再根据异步操作的执行状态，在其对应的回调里调用stateMachine.setCurrentState(TaskState);
	  TaskState.SUCCEED, TaskState.CANCEL, TaskState.FAIL，会取消掉blockWait中的锁，使execute方法继续执行下去。
