package org.project12306.services.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.ticketservice.dto.req.CancelTicketOrderReqDTO;
import org.project12306.services.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.project12306.services.ticketservice.dto.req.RefundTicketReqDTO;
import org.project12306.services.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.project12306.services.ticketservice.dto.resp.RefundTicketRespDTO;
import org.project12306.services.ticketservice.dto.resp.TicketPageQueryRespDTO;
import org.project12306.services.ticketservice.dto.resp.TicketPurchaseRespDTO;
import org.project12306.services.ticketservice.remote.dto.PayInfoRespDTO;
import org.project12306.services.ticketservice.service.TicketService;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
public class TicketController {

    private final TicketService ticketService;
    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQueryV1(requestParam));
    }

    /**
     * 购买车票
     */
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV1(requestParam));
    }

    /**
     * 购买车票v2
     */
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV2(requestParam));
    }

    /**
     * 取消车票订单
     */
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {

//        ticketService.cancelTicketOrder(requestParam);
        return Results.success();
    }

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/ticket-service/ticket/pay/query")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn) {
//        return Results.success(ticketService.getPayInfo(orderSn));
        return null;
    }

    /**
     * 公共退款接口
     */
    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<RefundTicketRespDTO> commonTicketRefund(@RequestBody RefundTicketReqDTO requestParam) {
//        return Results.success(ticketService.commonTicketRefund(requestParam));
        return null;
    }
}
