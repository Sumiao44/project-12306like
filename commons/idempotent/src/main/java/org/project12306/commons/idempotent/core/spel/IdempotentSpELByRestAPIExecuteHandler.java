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

package org.project12306.commons.idempotent.core.spel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.project12306.commons.idempotent.annotation.Idempotent;
import org.project12306.commons.idempotent.core.AbstractIdempotentExecuteHandler;
import org.project12306.commons.idempotent.core.IdempotentAspect;
import org.project12306.commons.idempotent.core.IdempotentContext;
import org.project12306.commons.idempotent.core.IdempotentParamWrapper;
import org.project12306.commons.idempotent.toolkit.SpELUtil;
import org.project12306.convention.exception.ClientException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 基于 SpEL 方法验证请求幂等性，适用于 RestAPI 场景
 */
@RequiredArgsConstructor
public final class IdempotentSpELByRestAPIExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:spEL:restAPI";

    /**
     * 获取注解参数，构建参数包装类
     * @param joinPoint AOP 方法处理
     * @return
     */
    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        //key生成策略，填入的key是要执行的spEl表达式，这里通过工具类获取真正的key
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        //赋值给注解的key和切点
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    /**
     * 处理器
     * @param wrapper 幂等参数包装器
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        //拼接唯一键
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        //使用redis作为去重表的实现
        RLock lock = redissonClient.getLock(uniqueKey);
        if (!lock.tryLock()) {
            //上锁失败就说明已经有锁了
            throw new ClientException(wrapper.getIdempotent().message());
        }
        //将锁存入上下文中
        IdempotentContext.put(LOCK, lock);
    }

    /**
     * 解锁，也就是从去重表中删除键
     */
    @Override
    public void postProcessing() {
        //成功后解锁
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void exceptionProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
