package org.project12306.services.orderservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.project12306.convention.page.PageResponse;
import org.project12306.services.orderservice.dao.entity.OrderItemDO;
import org.project12306.services.orderservice.dto.domain.OrderStatusReversalDTO;
import org.project12306.services.orderservice.dto.req.*;
import org.project12306.services.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.project12306.services.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.project12306.services.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.project12306.services.orderservice.mq.event.PayResultCallbackOrderEvent;

import java.util.List;

public interface OrderService {
    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 订单状态反转
     *
     * @param requestParam 请求参数
     */
    void statusReversal(OrderStatusReversalDTO requestParam);

    /**
     * 支付结果回调订单
     *
     * @param requestParam 请求参数
     */
    void payCallbackOrder(PayResultCallbackOrderEvent requestParam);

    /**
     * 关闭火车票订单
     *
     * @param requestParam 关闭火车票订单入参
     */
    boolean closeTickOrder(CancelTicketOrderReqDTO requestParam);


    /**
     * 取消火车票订单
     *
     * @param requestParam 取消火车票订单入参
     */
    boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn);

    /**
     * 跟据用户名分页查询车票订单
     *
     * @param requestParam 跟据用户 ID 分页查询对象
     * @return 订单分页详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

    /**
     * 查询本人车票订单
     *
     * @param requestParam 请求参数
     * @return 本人车票订单集合
     */
    PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam);
}
