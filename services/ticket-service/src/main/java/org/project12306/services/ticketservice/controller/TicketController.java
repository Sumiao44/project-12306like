package org.project12306.services.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.project12306.commons.web.Results;
import org.project12306.convention.result.Result;
import org.project12306.services.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.project12306.services.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.project12306.services.ticketservice.dto.resp.TicketPageQueryRespDTO;
import org.project12306.services.ticketservice.dto.resp.TicketPurchaseRespDTO;
import org.project12306.services.ticketservice.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


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

}
