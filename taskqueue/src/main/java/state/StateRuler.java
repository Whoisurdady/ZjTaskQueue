package state;

import java.util.Stack;

/**
 * Created by zhaojian on 2017/8/11.
 */

public abstract class StateRuler {

    abstract <T extends IState> boolean isStateOk(Stack<T> stateStack);
}
