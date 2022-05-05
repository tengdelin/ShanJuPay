package com.shanjupay.transaction.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;

/**
 * 交易相关的服务接口
 * Created by Administrator.
 */
public interface TransactionService {

    /**
     * 生成门店二维码的url
     * @param qrCodeDto,传入merchantId,appId、storeid、channel、subject、body
     * @return 支付入口（url），要携带参数（将传入的参数转成json，用base64编码）
     * @throws BusinessException
     */
    String createStoreQRCode(QRCodeDto qrCodeDto)throws BusinessException;

    /**
     * 保存支付宝订单，1、保存订单到闪聚平台，2、调用支付渠道代理服务调用支付宝的接口
     * @param payOrderDTO
     * @return
     * @throws BusinessException
     */
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException;

    /**
     * 根据订单号查询订单号
     * @param tradeNo
     * @return
     */
    public PayOrderDTO queryPayOrder(String tradeNo);
}
