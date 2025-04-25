package org.project12306.services.ticketservice.service;

import org.project12306.services.ticketservice.dto.resp.StationQueryRespDTO;

import java.util.List;

public interface RegionStationService {
    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    List<StationQueryRespDTO> listAllStation();
}
