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

package org.project12306.services.ticketservice.service.cache;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.commons.cache.toolkit.CacheUtil;
import org.project12306.services.ticketservice.common.enums.*;
import org.project12306.services.ticketservice.dao.entity.SeatDO;
import org.project12306.services.ticketservice.dao.entity.TrainDO;
import org.project12306.services.ticketservice.dao.mapper.SeatMapper;
import org.project12306.services.ticketservice.dao.mapper.TrainMapper;
import org.project12306.services.ticketservice.dto.domain.RouteDTO;
import org.project12306.services.ticketservice.service.TrainStationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.project12306.services.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.project12306.services.ticketservice.common.constant.RedisKeyConstant.*;


/**
 * 座位余量缓存加载
 */
@Component
@RequiredArgsConstructor
public class SeatMarginCacheLoader {

    private final TrainMapper trainMapper;
    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final TrainStationService trainStationService;

    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();
        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);
        //上分布式锁获取余票量
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            //如果缓存中没有这个余票就重建缓存
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                //先获取对应的列车信息
                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );

                //枚举并返回所有的路线，
                //A->D途径B,C
                //这个方法将会返回A->B,A->C,B->C,B->D,C->D,A->D的路线
                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(trainId, trainDO.getStartStation(), trainDO.getEndStation());
                if (CollUtil.isNotEmpty(routeDTOList)) {
                    switch (trainDO.getTrainType()) {
                        // TODO 通过已有列车类型座位枚举重构
                        //高速列车
                        case 0 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put("0", selectSeatMargin(trainId, 0, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("1", selectSeatMargin(trainId, 1, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("2", selectSeatMargin(trainId, 2, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        //普速列车
                        case 1 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put("3", selectSeatMargin(trainId, 3, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("4", selectSeatMargin(trainId, 4, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("5", selectSeatMargin(trainId, 5, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        //快车
                        case 2 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put("6", selectSeatMargin(trainId, 6, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("7", selectSeatMargin(trainId, 7, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("8", selectSeatMargin(trainId, 8, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                    }
                } else {
                    //什么情况下才会出现没有路线啊…怎么说都至少有可能是出发点到终点啊
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    //获取座位类型之后给每个座位类型都赋值0
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    //余票也赋值0
                    trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }
                // TODO LUA 脚本执行
                //将所有的列车余票放入缓存中
                trainStationRemainingTicketMaps.forEach((cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap));
            }
        } finally {
            lock.unlock();
        }
        //返回map表示列车的每种座位的余票分别多少
        return Optional.ofNullable(trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix))
                .orElse(new LinkedHashMap<>());
    }

    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}
