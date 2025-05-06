package org.project12306.services.orderservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.project12306.services.orderservice.dao.entity.OrderItemDO;
import org.project12306.services.orderservice.dto.domain.OrderStatusReversalDTO;
import org.project12306.services.orderservice.dto.req.CancelTicketOrderReqDTO;
import org.project12306.services.orderservice.dto.req.TicketOrderCreateReqDTO;
import org.project12306.services.orderservice.mq.event.PayResultCallbackOrderEvent;

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

}
