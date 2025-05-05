package org.project12306.services.orderservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.project12306.services.orderservice.dao.entity.OrderItemDO;
import org.project12306.services.orderservice.dto.req.TicketOrderCreateReqDTO;

public interface OrderService {
    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);
}
