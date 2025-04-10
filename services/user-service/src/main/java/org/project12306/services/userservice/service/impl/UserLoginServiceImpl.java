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

package org.project12306.services.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project12306.cache.DistributedCache;
import org.project12306.desingnpattern.chain.AbstractChainContext;
import org.project12306.convention.exception.ServiceException;
import org.project12306.services.userservice.common.enums.UserChainMarkEnum;
import org.project12306.services.userservice.dao.entity.UserDO;
import org.project12306.services.userservice.dao.entity.UserMailDO;
import org.project12306.services.userservice.dao.entity.UserPhoneDO;
import org.project12306.services.userservice.dao.entity.UserReuseDO;
import org.project12306.services.userservice.dao.mapper.UserMailMapper;
import org.project12306.services.userservice.dao.mapper.UserMapper;
import org.project12306.services.userservice.dao.mapper.UserPhoneMapper;
import org.project12306.services.userservice.dao.mapper.UserReuseMapper;
import org.project12306.services.userservice.dto.req.UserRegisterReqDTO;
import org.project12306.services.userservice.dto.resp.UserRegisterRespDTO;
import org.project12306.services.userservice.service.UserLoginService;
import org.project12306.services.userservice.service.UserService;
import org.project12306.general.toolkit.BeanUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import static org.project12306.services.userservice.common.constant.RedisKeyConstant.LOCK_USER_REGISTER;
import static org.project12306.services.userservice.common.constant.RedisKeyConstant.USER_REGISTER_REUSE_SHARDING;
import static org.project12306.services.userservice.common.enums.UserRegisterErrorCodeEnum.*;
import static org.project12306.general.toolkit.UserReuseUtil.hashShardingIdx;

/**
 * 用户登录接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserReuseMapper userReuseMapper;
//    private final UserDeletionMapper userDeletionMapper;
    private final UserPhoneMapper userPhoneMapper;
    private final UserMailMapper userMailMapper;
    private final RedissonClient redissonClient;
    private final DistributedCache distributedCache;
    private final AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(), requestParam);
        //通过用户名获取一把锁
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            //失败说明正好有其它用户也在注册，并且用户名与当前事务用户名一致，其他用户的事务持有锁
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }
        try {
            try {
                int inserted = userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
                //修改行数有误，抛出插入用户异常
                if (inserted < 1) {
                    throw new ServiceException(USER_REGISTER_FAIL);
                }
            } catch (DuplicateKeyException dke) {
                //用户名重复异常
                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
                throw new ServiceException(HAS_USERNAME_NOTNULL);
            }
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(requestParam.getPhone())
                    .username(requestParam.getUsername())
                    .build();
            try {
                userPhoneMapper.insert(userPhoneDO);
            } catch (DuplicateKeyException dke) {
                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
                throw new ServiceException(PHONE_REGISTERED);
            }
            if (StrUtil.isNotBlank(requestParam.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(requestParam.getMail())
                        .username(requestParam.getUsername())
                        .build();
                try {
                    userMailMapper.insert(userMailDO);
                } catch (DuplicateKeyException dke) {
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(MAIL_REGISTERED);
                }
            }
            String username = requestParam.getUsername();
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);

            userRegisterCachePenetrationBloomFilter.add(username);
        } finally {
            lock.unlock();
        }
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }

    @Override
    public Boolean hasUsername(String username) {
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if (hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            return instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        }
        return true;
    }


}
