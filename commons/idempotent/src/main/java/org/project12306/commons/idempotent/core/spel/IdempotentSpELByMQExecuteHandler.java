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
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.commons.idempotent.annotation.Idempotent;
import org.project12306.commons.idempotent.core.*;
import org.project12306.commons.idempotent.enums.IdempotentMQConsumeStatusEnum;
import org.project12306.commons.idempotent.toolkit.LogUtil;
import org.project12306.commons.idempotent.toolkit.SpELUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于 SpEL 方法验证请求幂等性，适用于 MQ 场景
 */
@Slf4j
@RequiredArgsConstructor
public final class IdempotentSpELByMQExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {
    /**
     * 固定执行0.6秒后被删除键
     * 避免redis的键占用
     */
    private final static int TIMEOUT = 600;

    private final static String WRAPPER = "wrapper:spEL:MQ";

    private final static String LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH = "lua/set_if_absent_and_get.lua";

    private final DistributedCache distributedCache;

    /**
     * 获取注解参数
     * @param joinPoint AOP 方法处理
     * @return
     */
    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    /**
     * 防止重复处理消息队列的幂等处理器
     * @param wrapper 幂等参数包装器
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        log.info("唯一键："+uniqueKey);
        //向redis中插入数据表示mq中的该键正在被执行，消费中
        String absentAndGet = this.setIfAbsentAndGet(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMING.getCode(), TIMEOUT, TimeUnit.SECONDS);

        //执行失败返回非空
        if (Objects.nonNull(absentAndGet)) {
            //返回结果为0则锁被占用正在消费中，即发生错误
            boolean error = IdempotentMQConsumeStatusEnum.isError(absentAndGet);
            LogUtil.getLog(wrapper.getJoinPoint()).warn("[{}] MQ repeated consumption, {}.", uniqueKey, error ? "Wait for the client to delay consumption" : "Status is completed");
            throw new RepeatConsumptionException(error);
        }
        IdempotentContext.put(WRAPPER, wrapper);
    }

    /**
     * 该方法是向去重表中插入唯一键并获得返回值
     * 使用lua脚本保证了元素插入的原子性
     * @param key 去重表的唯一键
     * @param value 插入的值
     * @param timeout 时间
     * @param timeUnit 时间单位
     * @return 返回插入的结果
     */
    public String setIfAbsentAndGet(String key, String value, long timeout, TimeUnit timeUnit) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        ClassPathResource resource = new ClassPathResource(LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH);
        redisScript.setScriptSource(new ResourceScriptSource(resource));
        redisScript.setResultType(String.class);

        long millis = timeUnit.toMillis(timeout);
        return ((StringRedisTemplate) distributedCache.getInstance())//获取redis操作实例
                    .execute(redisScript, List.of(key), value, String.valueOf(millis));//执行脚本并填入参数
    }

    @Override
    public void exceptionProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                distributedCache.delete(uniqueKey);
            } catch (Throwable ex) {
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.", uniqueKey);
            }
        }
    }

    @Override
    public void postProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                //消费完成后标记键值为消费完成
                distributedCache.put(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMED.getCode(), idempotent.keyTimeout(), TimeUnit.SECONDS);
            } catch (Throwable ex) {
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to set MQ anti-heavy token.", uniqueKey);
            }
        }
    }
}
