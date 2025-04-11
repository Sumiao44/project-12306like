package org.project12306.commons.desingnpattern.chain;

import org.springframework.core.Ordered;

public interface AbstractChainHandler<T> extends Ordered {
    /**
     * 实际的责任链执行方法
     * @param handler 责任链判断参数
     */
    void handler(T handler);

    /**
     * 返回属于哪一个组件的组件标识
     * @return 责任链组件标识
     */
    String mark();
}
