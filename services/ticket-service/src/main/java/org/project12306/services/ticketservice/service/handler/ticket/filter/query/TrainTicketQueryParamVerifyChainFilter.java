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

package org.project12306.services.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.convention.exception.ClientException;
import org.project12306.services.ticketservice.dao.entity.RegionDO;
import org.project12306.services.ticketservice.dao.entity.StationDO;
import org.project12306.services.ticketservice.dao.mapper.RegionMapper;
import org.project12306.services.ticketservice.dao.mapper.StationMapper;
import org.project12306.services.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.project12306.services.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 查询列车车票流程过滤器之验证数据是否正确
 * 缓存是否是否初始化了？初始化了的话缓存中是否有站点
 * 缓存没有初始化的话就对地区站点缓存进行初始化，并再次查询是否有这两个站点。
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 缓存数据为空并且已经加载过标识
     */
    private static boolean CACHE_DATA_ISNULL_AND_LOAD_FLAG = false;

    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        //从缓存的站点哈希表中获取出发地和起始地
        List<Object> actualExistList = hashOperations.multiGet(
                QUERY_ALL_REGION_LIST,
                ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
        );
        //计算获取到的空地区，也就是没能正常获取到的地区个数
        long emptyCount = actualExistList.stream().filter(Objects::isNull).count();
        //如果是0就是正常的，说明两个地点都不为null，都正常获取到了
        if (emptyCount == 0L) {
            return;
        }
        //如果CacheDataIsnullAndLoadFlag是false就说明读取到两个都是null是因为缓存还没初始化过
        //如果是true就查看缓存中是否正常存入了这个地区列表key
        //使用标志可以避免去缓存中查询，提高运行效率
        if (emptyCount == 1L || (emptyCount == 2L && CACHE_DATA_ISNULL_AND_LOAD_FLAG && distributedCache.hasKey(QUERY_ALL_REGION_LIST))) {
            throw new ClientException("出发地或目的地不存在");
        }
        //走到这里就说明缓存没被初始化，或者初始化了但是缓存中没有被存入地区列表
        //使用分布式锁初始化站点列表
        RLock lock = redissonClient.getLock(LOCK_QUERY_ALL_REGION_LIST);
        lock.lock();
        try {
            if (distributedCache.hasKey(QUERY_ALL_REGION_LIST)) {
                //进入这个if说明缓存中存在地点列表，有可能别的线程先获得了锁并且已经完成了初始化，避免了缓存击穿的问题
                actualExistList = hashOperations.multiGet(
                        QUERY_ALL_REGION_LIST,
                        ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
                );
                emptyCount = actualExistList.stream().filter(Objects::nonNull).count();
                if (emptyCount != 2L) {
                    //到这里就说明缓存是正常的，但是缓存列表中没有这两个站点，抛出异常
                    throw new ClientException("出发地或目的地不存在");
                }
                return;
            }
            //缓存中不存在地点列表，需要进行初始化
            //获取地区列表
            List<RegionDO> regionDOList = regionMapper.selectList(Wrappers.emptyWrapper());
            //获取站点列表
            List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
            HashMap<Object, Object> regionValueMap = Maps.newHashMap();

            //分别存入各自的地区码和地区名，站点码和站点名
            //因为地区码和站点码是分开算的，这牵涉到数据库的表设计
            //地区：北京的code是BJP
            //站点：北京的code也是BJP 但是站点：北京南的code是VNP，但他们俩的region都是BJP
            for (RegionDO each : regionDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            for (StationDO each : stationDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            //把数据进行初始化，也就是向redis中存入站点和地区的哈希表
            hashOperations.putAll(QUERY_ALL_REGION_LIST, regionValueMap);
            //标志为true
            CACHE_DATA_ISNULL_AND_LOAD_FLAG = true;
            emptyCount = regionValueMap.keySet().stream()
                    .filter(each -> StrUtil.equalsAny(each.toString(), requestParam.getFromStation(), requestParam.getToStation()))
                    .count();
            //最后再次查询目标地区，依然没有就是没有这俩地点
            if (emptyCount != 2L) {
                throw new ClientException("出发地或目的地不存在");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
