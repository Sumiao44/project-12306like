package org.project12306.services.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.user.core.UserContext;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.userservice.dto.resp.PassengerRespDTO;
import org.project12306.services.userservice.service.PassengerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 乘车人控制层
 */
@RestController
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    /**
     * 根据用户名查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername() {
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }
}
