package org.project12306.services.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.ticketservice.dto.resp.StationQueryRespDTO;
import org.project12306.services.ticketservice.service.RegionStationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地区以及车站查询控制层
 */
@RestController
@RequiredArgsConstructor
public class RegionStationController {

    private final RegionStationService regionStationService;

    @GetMapping("/api/ticket-service/station/all")
    public Result<List<StationQueryRespDTO>> listAllStation() {
        return Results.success(regionStationService.listAllStation());
    }
}
