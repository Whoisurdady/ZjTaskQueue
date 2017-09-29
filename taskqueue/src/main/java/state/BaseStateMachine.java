package state;

import java.util.Stack;

/**
 * Created by zhaojian on 2017/8/11.
 */

public abstract class BaseStateMachine<T extends IState> {
    private T currentState;
    private Stack<T> stateStack = new Stack<>();

    public BaseStateMachine() {
        currentState = startState();
        stateStack.add(currentState);
    }

    /**
     * 设置初始化state
     * */
    public abstract T startState();

    /**
     * 当state变化时回调
     *
     * @param stateStack state 过去的栈;
     * @param currentState 当前的state;
     *
     * @return 是否允许这种state的改变
     * */
    public abstract boolean onStateChanged(Stack<T> stateStack, T currentState);

    /**
     * 当state 变化失败后回调
     *
     * @param stateStack state 过去的栈;
     * @param illegalState 当前的state;
     *
     * */

    public abstract void onStateChangeFailed(Stack<T> stateStack, T illegalState);

    /**
     * 设置当前state
     * */
    public synchronized void setCurrentState(T state) {
        if (state == currentState) {
            return;
        }
        T lastState = currentState;
        this.currentState = state;
        if (onStateChanged(stateStack, state)) {
            stateStack.push(state);
        } else {
            onStateChangeFailed(stateStack, state);
            currentState = lastState;
        }
    }

    public T getCurrentState() {
        return currentState;
    }


    /**
     * 回退到上个状态
     * */
    public BaseStateMachine stateBack() {
        if (stateStack.size() > 1) {
            T tempState = currentState;
            currentState = stateStack.pop();
            if (!onStateChanged(stateStack, currentState)) {
                onStateChangeFailed(stateStack, currentState);
                currentState = tempState;
                stateStack.push(currentState);
            }
        }
        return this;
    }


    /**
     * 在某个状态下执行
     * */

    public boolean doOnState(Runnable runnable, T state) {
        if (state == currentState) {
            runnable.run();
            return true;
        } else {
            return false;
        }
    }


    /**
     * 不在某个状态下执行
     * */

    public boolean doWithOutState(Runnable runnable, T state) {
        if (state != currentState) {
            runnable.run();
            return true;
        } else {
            return false;
        }
    }



    /**
     * 在指定规则下执行
     * */

    public boolean doOnRule(Runnable runnable, StateRuler stateRuler) {
        if (stateRuler == null) {
            runnable.run();
            return true;
        }
        if (stateRuler.isStateOk(stateStack)) {
            runnable.run();
            return true;
        } else {
            return false;
        }
    }


}
