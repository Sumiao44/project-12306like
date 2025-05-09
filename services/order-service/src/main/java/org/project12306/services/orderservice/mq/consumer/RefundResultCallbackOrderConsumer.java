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

package org.project12306.services.orderservice.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.project12306.commons.common.toolkit.BeanUtil;
import org.project12306.commons.idempotent.annotation.Idempotent;
import org.project12306.commons.idempotent.enums.IdempotentSceneEnum;
import org.project12306.commons.idempotent.enums.IdempotentTypeEnum;
import org.project12306.services.orderservice.common.constant.OrderRocketMQConstant;
import org.project12306.services.orderservice.common.enums.OrderItemStatusEnum;
import org.project12306.services.orderservice.common.enums.OrderStatusEnum;
import org.project12306.services.orderservice.dao.entity.OrderItemDO;
import org.project12306.services.orderservice.dto.domain.OrderItemStatusReversalDTO;
import org.project12306.services.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.project12306.services.orderservice.mq.domain.MessageWrapper;
import org.project12306.services.orderservice.mq.event.RefundResultCallbackOrderEvent;
import org.project12306.services.orderservice.service.OrderItemService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 退款结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_ORDER_CG_KEY
)
public class RefundResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<RefundResultCallbackOrderEvent>> {

    private final OrderItemService orderItemService;

    @Idempotent(
            uniqueKeyPrefix = "index12306-order:refund_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<RefundResultCallbackOrderEvent> message) {
        RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = message.getMessage();
        Integer status = refundResultCallbackOrderEvent.getRefundTypeEnum().getCode();
        String orderSn = refundResultCallbackOrderEvent.getOrderSn();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList = refundResultCallbackOrderEvent.getPartialRefundTicketDetailList();
        partialRefundTicketDetailList.forEach(partial -> {
            OrderItemDO orderItemDO = new OrderItemDO();
            BeanUtil.convert(partial, orderItemDO);
            orderItemDOList.add(orderItemDO);
        });
        if (status.equals(OrderStatusEnum.PARTIAL_REFUND.getStatus())) {
            OrderItemStatusReversalDTO partialRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    .orderSn(orderSn)
                    .orderStatus(OrderStatusEnum.PARTIAL_REFUND.getStatus())
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                    .orderItemDOList(orderItemDOList)
                    .build();
            orderItemService.orderItemStatusReversal(partialRefundOrderItemStatusReversalDTO);
        } else if (status.equals(OrderStatusEnum.FULL_REFUND.getStatus())) {
            OrderItemStatusReversalDTO fullRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    .orderSn(orderSn)
                    .orderStatus(OrderStatusEnum.FULL_REFUND.getStatus())
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                    .orderItemDOList(orderItemDOList)
                    .build();
            orderItemService.orderItemStatusReversal(fullRefundOrderItemStatusReversalDTO);
        }
    }
}
