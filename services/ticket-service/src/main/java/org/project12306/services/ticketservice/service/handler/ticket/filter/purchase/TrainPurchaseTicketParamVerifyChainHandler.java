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

package org.project12306.services.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.commons.common.toolkit.EnvironmentUtil;
import org.project12306.convention.exception.ClientException;
import org.project12306.services.ticketservice.common.constant.Index12306Constant;
import org.project12306.services.ticketservice.dao.entity.TrainDO;
import org.project12306.services.ticketservice.dao.entity.TrainStationDO;
import org.project12306.services.ticketservice.dao.mapper.TrainMapper;
import org.project12306.services.ticketservice.dao.mapper.TrainStationMapper;
import org.project12306.services.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.project12306.services.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.project12306.services.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;
import static org.project12306.services.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * 购票流程过滤器之验证参数是否有效
 * 验证参数的合法性，例如车次是否存在，站点顺序是否正常
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamVerifyChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final TrainMapper trainMapper;
    private final TrainStationMapper trainStationMapper;
    private final DistributedCache distributedCache;

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 查询会员购票车次是否存在，通过封装后安全的 Get 方法
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        if (Objects.isNull(trainDO)) {
            // 如果按照严谨逻辑，类似异常应该记录当前用户的 userid 并发送到风控中心
            // 如果一段时间有过几次的异常，直接封号处理。下述异常同理
            throw new ClientException("请检查车次是否存在");
        }
        // TODO，当前列车数据并没有通过定时任务每天生成最新的，所以需要隔离这个拦截。后期定时生成数据后删除该判断
        if (!EnvironmentUtil.isDevEnvironment()) {
            // 查询车次是否已经发售
            if (new Date().before(trainDO.getSaleTime())) {
                throw new ClientException("列车车次暂未发售");
            }
            // 查询车次是否在有效期内
            if (new Date().after(trainDO.getDepartureTime())) {
                throw new ClientException("列车车次已出发禁止购票");
            }
        }
        // 车站是否存在车次中，以及车站的顺序是否正确
        String trainStationStopoverDetailStr = distributedCache.safeGet(
                TRAIN_STATION_STOPOVER_DETAIL + requestParam.getTrainId(),
                String.class,
                () -> {
                    LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                            .eq(TrainStationDO::getTrainId, requestParam.getTrainId())
                            .select(TrainStationDO::getDeparture);
                    List<TrainStationDO> actualTrainStationList = trainStationMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(actualTrainStationList) ? JSON.toJSONString(actualTrainStationList) : null;
                },
                Index12306Constant.ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        List<TrainStationDO> trainDOList = JSON.parseArray(trainStationStopoverDetailStr, TrainStationDO.class);
        boolean validateStation = validateStation(
                trainDOList.stream().map(TrainStationDO::getDeparture).toList(),
                requestParam.getDeparture(),
                requestParam.getArrival()
        );
        if (!validateStation) {
            throw new ClientException("列车车站数据错误");
        }
    }


    public boolean validateStation(List<String> stationList, String startStation, String endStation) {
        int index1 = stationList.indexOf(startStation);
        int index2 = stationList.indexOf(endStation);
        if (index1 == -1 || index2 == -1) {
            return false;
        }
        return index2 >= index1;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
