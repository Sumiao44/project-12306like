package org.project12306.services.ticketservice.service;

import org.project12306.services.ticketservice.dto.req.RegionStationQueryReqDTO;
import org.project12306.services.ticketservice.dto.resp.RegionStationQueryRespDTO;
import org.project12306.services.ticketservice.dto.resp.StationQueryRespDTO;

import java.util.List;

public interface RegionStationService {
    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    List<StationQueryRespDTO> listAllStation();

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&站点查询参数
     * @return 车站&站点返回数据集合
     */
    List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam);

}
