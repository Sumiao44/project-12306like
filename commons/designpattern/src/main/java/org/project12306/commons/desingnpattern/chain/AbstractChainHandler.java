/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
