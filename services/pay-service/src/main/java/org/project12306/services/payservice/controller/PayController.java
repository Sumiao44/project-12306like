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

package org.project12306.services.payservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.payservice.convert.PayRequestConvert;
import org.project12306.services.payservice.dto.base.PayRequest;
import org.project12306.services.payservice.dto.command.PayCommand;
import org.project12306.services.payservice.dto.req.RefundReqDTO;
import org.project12306.services.payservice.dto.resp.PayInfoRespDTO;
import org.project12306.services.payservice.dto.resp.PayRespDTO;
import org.project12306.services.payservice.dto.resp.RefundRespDTO;
import org.project12306.services.payservice.service.PayService;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制层
 */
@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 公共支付接口
     * 对接常用支付方式，比如：支付宝、微信以及银行卡等
     */
    @PostMapping("/api/pay-service/pay/create")
    public Result<PayRespDTO> pay(@RequestBody PayCommand requestParam) {
        PayRequest payRequest = PayRequestConvert.command2PayRequest(requestParam);
        PayRespDTO result = payService.commonPay(payRequest);
        return Results.success(result);
    }

    /**
     * 跟据订单号查询支付单详情
     */
    @GetMapping("/api/pay-service/pay/query/order-sn")
    public Result<PayInfoRespDTO> getPayInfoByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(payService.getPayInfoByOrderSn(orderSn));
    }

    /**
     * 跟据支付流水号查询支付单详情
     */
    @GetMapping("/api/pay-service/pay/query/pay-sn")
    public Result<PayInfoRespDTO> getPayInfoByPaySn(@RequestParam(value = "paySn") String paySn) {
        return Results.success(payService.getPayInfoByPaySn(paySn));
    }

    /**
     * 公共退款接口
     * 后续为了方便开发系列退款相关接口，已迁移 {@link RefundController #commonRefund(RefundReqDTO)}
     */
    @Deprecated
    @PostMapping("/api/pay-service/refund")
    public Result<RefundRespDTO> refund(@RequestBody RefundReqDTO requestParam) {
        return Results.success(payService.commonRefund(requestParam));
    }
}
