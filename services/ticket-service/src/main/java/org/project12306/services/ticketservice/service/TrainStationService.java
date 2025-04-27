package org.project12306.services.ticketservice.service;

import org.project12306.services.ticketservice.dto.domain.RouteDTO;

import java.util.List;

/**
 * 列车站点接口层
 */
public interface TrainStationService {
    /**
     * 计算列车站点路线关系
     * 获取开始站点和目的站点及中间站点信息
     *
     * @param trainId   列车 ID
     * @param departure 出发站
     * @param arrival   到达站
     * @return 列车站点路线关系信息
     */
    List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival);
}
