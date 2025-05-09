package org.project12306.services.payservice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project12306.commons.cache.DistributedCache;
import org.project12306.commons.common.toolkit.BeanUtil;
import org.project12306.commons.desingnpattern.strategy.AbstractStrategyChoose;
import org.project12306.commons.idempotent.annotation.Idempotent;
import org.project12306.commons.idempotent.enums.IdempotentTypeEnum;
import org.project12306.convention.exception.ServiceException;
import org.project12306.services.payservice.common.enums.TradeStatusEnum;
import org.project12306.services.payservice.convert.RefundRequestConvert;
import org.project12306.services.payservice.dao.entity.PayDO;
import org.project12306.services.payservice.dao.mapper.PayMapper;
import org.project12306.services.payservice.dto.base.PayRequest;
import org.project12306.services.payservice.dto.base.PayResponse;
import org.project12306.services.payservice.dto.base.RefundRequest;
import org.project12306.services.payservice.dto.command.RefundCommand;
import org.project12306.services.payservice.dto.req.PayCallbackReqDTO;
import org.project12306.services.payservice.dto.req.RefundReqDTO;
import org.project12306.services.payservice.dto.resp.PayInfoRespDTO;
import org.project12306.services.payservice.dto.resp.PayRespDTO;
import org.project12306.services.payservice.dto.resp.RefundRespDTO;
import org.project12306.services.payservice.dto.resp.RefundResponse;
import org.project12306.services.payservice.mq.event.PayResultCallbackOrderEvent;
import org.project12306.services.payservice.mq.produce.PayResultCallbackOrderSendProduce;
import org.project12306.services.payservice.service.PayService;
import org.project12306.services.payservice.service.payid.PayIdGeneratorManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.project12306.services.payservice.common.constant.RedisKeyConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {
    private final PayMapper payMapper;
    private final PayResultCallbackOrderSendProduce payResultCallbackOrderSendProduce;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final DistributedCache distributedCache;

    @Idempotent(
            type = IdempotentTypeEnum.SPEL,
            uniqueKeyPrefix = "project12306-pay:lock_create_pay:",
            key = "#requestParam.getOutOrderSn()"
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public PayRespDTO commonPay(PayRequest requestParam) {
        PayRespDTO cacheResult = distributedCache.get(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), PayRespDTO.class);
        if (cacheResult != null) {
            //不为空则已经创建过支付缓存
            return cacheResult;
        }
        /**
         * {@link AliPayNativeHandler}
         */
        // 策略模式：通过策略模式封装支付渠道和支付场景，用户支付时动态选择对应的支付组件
        // 在传参时就把支付方式对应的mark传入
        PayResponse result = abstractStrategyChoose.chooseAndExecuteResp(requestParam.buildMark(), requestParam);
        PayDO insertPay = BeanUtil.convert(requestParam, PayDO.class);
        //生成全局唯一id
        String paySn = PayIdGeneratorManager.generateId(requestParam.getOrderSn());
        //手动添加流水号
        insertPay.setPaySn(paySn);
        insertPay.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        insertPay.setTotalAmount(requestParam.getTotalAmount().multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        int insert = payMapper.insert(insertPay);

        if (insert <= 0) {
            log.error("支付单创建失败，支付聚合根：{}", JSON.toJSONString(requestParam));
            throw new ServiceException("支付单创建失败");
        }
        distributedCache.put(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), JSON.toJSONString(result), 10, TimeUnit.MINUTES);
        return BeanUtil.convert(result, PayRespDTO.class);
    }

    @Override
    public PayInfoRespDTO getPayInfoByOrderSn(String orderSn) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, orderSn);
        PayDO payDO = payMapper.selectOne(queryWrapper);
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    @Override
    public PayInfoRespDTO getPayInfoByPaySn(String paySn) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getPaySn, paySn);
        PayDO payDO = payMapper.selectOne(queryWrapper);
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    @Override
    public RefundRespDTO commonRefund(RefundReqDTO requestParam) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        PayDO payDO = payMapper.selectOne(queryWrapper);
        //查询要退款的订单
        if (Objects.isNull(payDO)) {
            log.error("支付单不存在，orderSn：{}", requestParam.getOrderSn());
            throw new ServiceException("支付单不存在");
        }


        /**
         * {@link AliRefundNativeHandler}
         */
        // 策略模式：通过策略模式封装退款渠道和退款场景，用户退款时动态选择对应的退款组件
        RefundCommand refundCommand = BeanUtil.convert(payDO, RefundCommand.class);
        RefundRequest refundRequest = RefundRequestConvert.command2RefundRequest(refundCommand);
        RefundResponse result = abstractStrategyChoose.chooseAndExecuteResp(refundRequest.buildMark(), refundRequest);
        payDO.setStatus(result.getStatus());
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        int updateResult = payMapper.update(payDO, updateWrapper);
        if (updateResult <= 0) {
            log.error("修改支付单退款结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            throw new ServiceException("修改支付单退款结果失败");
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void callbackPay(PayCallbackReqDTO requestParam) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        PayDO payDO = payMapper.selectOne(queryWrapper);
        if (Objects.isNull(payDO)) {
            log.error("支付单不存在，orderRequestId：{}", requestParam.getOrderRequestId());
            throw new ServiceException("支付单不存在");
        }

        payDO.setTradeNo(requestParam.getTradeNo());
        payDO.setStatus(requestParam.getStatus());
        payDO.setPayAmount(requestParam.getPayAmount());
        payDO.setGmtPayment(requestParam.getGmtPayment());
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        int result = payMapper.update(payDO, updateWrapper);
        if (result <= 0) {
            log.error("修改支付单支付结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            throw new ServiceException("修改支付单支付结果失败");
        }
        // 交易成功，回调订单服务告知支付结果，修改订单流转状态
        if (Objects.equals(requestParam.getStatus(), TradeStatusEnum.TRADE_SUCCESS.tradeCode())) {
            payResultCallbackOrderSendProduce.sendMessage(BeanUtil.convert(payDO, PayResultCallbackOrderEvent.class));
        }
    }
}
