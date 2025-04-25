package org.project12306.services.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.commons.common.toolkit.BeanUtil;
import org.project12306.services.ticketservice.dao.mapper.StationMapper;
import org.project12306.services.ticketservice.dto.resp.StationQueryRespDTO;
import org.project12306.services.ticketservice.service.RegionStationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.project12306.services.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.project12306.services.ticketservice.common.constant.RedisKeyConstant.STATION_ALL;

@Service
@RequiredArgsConstructor
public class RegionStationServiceImpl implements RegionStationService {
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return distributedCache.safeGet(
                STATION_ALL,
                List.class,
                () -> BeanUtil.convert(stationMapper.selectList(Wrappers.emptyWrapper()), StationQueryRespDTO.class),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
    }
}
