package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.entity.PayOrder;
import com.shanjupay.transaction.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    //从配置文件读取支付入口地址
    @Value("${shanjupay.payurl}")
    String payurl;

    @Reference
    AppService appService;

    @Reference
    MerchantService merchantService;

    @Autowired
    PayOrderMapper payOrderMapper;

    @Reference
    PayChannelAgentService payChannelAgentService;

    @Autowired
    PayChannelService payChannelService;


    /**
     * 生成门店二维码的url
     *
     * @param qrCodeDto@return 支付入口（url），要携带参数（将传入的参数转成json，用base64编码）
     * @throws BusinessException
     */
    @Override
    public String createStoreQRCode(QRCodeDto qrCodeDto) throws BusinessException {

        //校验商户id和应用id和门店id的合法性
        verifyAppAndStore(qrCodeDto.getMerchantId(), qrCodeDto.getAppId(), qrCodeDto.getStoreId());

        //组装url所需要的数据
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setMerchantId(qrCodeDto.getMerchantId());
        payOrderDTO.setAppId(qrCodeDto.getAppId());
        payOrderDTO.setStoreId(qrCodeDto.getStoreId());
        payOrderDTO.setSubject(qrCodeDto.getSubject());//显示订单标题
        payOrderDTO.setChannel("shanju_c2b");//服务类型，要写为c扫b的服务类型
        payOrderDTO.setBody(qrCodeDto.getBody());//订单内容
        //转成json
        String jsonString = JSON.toJSONString(payOrderDTO);
        //base64编码
        String ticket = EncryptUtil.encodeUTF8StringBase64(jsonString);

        //目标是生成一个支付入口 的url，需要携带参数将传入的参数转成json，用base64编码
        String url = payurl + ticket;
        return url;
    }

    /**
     * 保存支付宝订单，1、保存订单到闪聚平台，2、调用支付渠道代理服务调用支付宝的接口
     *
     * @param payOrderDTO
     * @return
     * @throws BusinessException
     */
    @Override
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException {

        payOrderDTO.setChannel("ALIPAY_WAP");//支付渠道
        //保存订单到闪聚平台数据库
        PayOrderDTO save = save(payOrderDTO);

        //调用支付渠道代理服务支付宝下单接口
        PaymentResponseDTO paymentResponseDTO = alipayH5(save.getTradeNo());
        return paymentResponseDTO;
    }

    //调用支付渠道代理服务的支付宝下单接口
    private PaymentResponseDTO alipayH5(String tradeNo) {
        //订单信息，从数据库查询订单
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);
        //组装alipayBean
        AlipayBean alipayBean = new AlipayBean();
        alipayBean.setOutTradeNo(payOrderDTO.getTradeNo());//订单号
        try {
            alipayBean.setTotalAmount(AmountUtil.changeF2Y(payOrderDTO.getTotalAmount().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_300006);
        }
        alipayBean.setSubject(payOrderDTO.getSubject());
        alipayBean.setBody(payOrderDTO.getBody());
        alipayBean.setExpireTime("30m");

        //支付渠道配置参数，从数据库查询
        //String appId,String platformChannel,String payChannel
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(), "shanju_c2b", "ALIPAY_WAP");
        String paramJson = payChannelParamDTO.getParam();
        //支付渠道参数
        AliConfigParam aliConfigParam = JSON.parseObject(paramJson, AliConfigParam.class);
        //字符编码
        aliConfigParam.setCharest("utf-8");
        //AliConfigParam aliConfigParam, AlipayBean alipayBean
        PaymentResponseDTO payOrderByAliWAP = payChannelAgentService.createPayOrderByAliWAP(aliConfigParam, alipayBean);
        return payOrderByAliWAP;
    }

    /**
     * 根据订单号查询订单信息
     *
     * @param tradeNo
     * @return
     */
    public PayOrderDTO queryPayOrder(String tradeNo) {
        PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getTradeNo, tradeNo));
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    //保存订单（公用）
    private PayOrderDTO save(PayOrderDTO payOrderDTO) throws BusinessException {
        PayOrder payOrder = PayOrderConvert.INSTANCE.dto2entity(payOrderDTO);
        //订单号
        payOrder.setTradeNo(PaymentUtil.genUniquePayOrderNo());//采用雪花片算法
        payOrder.setCreateTime(LocalDateTime.now());//创建时间
        payOrder.setExpireTime(LocalDateTime.now().plus(30, ChronoUnit.MINUTES));//过期时间是30分钟后
        payOrder.setCurrency("CNY");//人民币
        payOrder.setTradeState("0");//订单状态，0：订单生成
        payOrderMapper.insert(payOrder);//插入订单
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    //私有，校验商户id和应用id和门店id的合法性
    private void verifyAppAndStore(Long merchantId, String appId, Long storeId) {
        //根据 应用id和商户id查询
        Boolean aBoolean = appService.queryAppInMerchant(appId, merchantId);
        if (!aBoolean) {
            throw new BusinessException(CommonErrorCode.E_200005);
        }
        //根据 门店id和商户id查询
        Boolean aBoolean1 = merchantService.queryStoreInMerchant(storeId, merchantId);
        if (!aBoolean1) {
            throw new BusinessException(CommonErrorCode.E_200006);
        }
    }


}
