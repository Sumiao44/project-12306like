package org.project12306.services.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.project12306.services.orderservice.dao.entity.OrderItemDO;
import org.project12306.services.orderservice.dto.domain.OrderItemStatusReversalDTO;
import org.project12306.services.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import org.project12306.services.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 * 订单明细接口层
 */
public interface OrderItemService extends IService<OrderItemDO> {

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     */
    List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam);
    /**
     * 子订单状态反转
     *
     * @param requestParam 请求参数
     */
    void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam);
}