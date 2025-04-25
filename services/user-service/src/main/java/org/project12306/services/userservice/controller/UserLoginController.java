package org.project12306.services.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.userservice.dto.req.UserLoginReqDTO;
import org.project12306.services.userservice.dto.resp.UserLoginRespDTO;
import org.project12306.services.userservice.service.UserLoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
/**
 * 用户登录控制层
 */
@RequiredArgsConstructor
@RestController
public class UserLoginController {
    private final UserLoginService userLoginService;

    /**
     * 用户登录
     */
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userLoginService.login(requestParam));
    }



}
