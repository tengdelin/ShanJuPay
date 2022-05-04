package com.shanjupay.transaction.controller;

import com.alibaba.fastjson.JSON;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.IPUtil;
import com.shanjupay.common.util.ParseURLPairUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.vo.OrderConfirmVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 支付相关接口
 *
 * @author Administrator
 * @version 1.0
 **/
@Slf4j
@Controller
public class PayController {

    @Autowired
    TransactionService transactionService;

    @Reference
    AppService appService;

    /**
     * 支付入口
     *
     * @param ticket  传入数据，对json数据进行的base64编码
     * @param request
     * @return
     */
    @RequestMapping("/pay-entry/{ticket}")
    public String payEntry(@PathVariable("ticket") String ticket, HttpServletRequest request) throws Exception {
        //1、准备确认页面所需要的数据
        String jsonString = EncryptUtil.decodeUTF8StringBase64(ticket);
        //将json串转成对象
        PayOrderDTO payOrderDTO = JSON.parseObject(jsonString, PayOrderDTO.class);
        //将对象的属性和值组成一个url的key/value串
        String params = ParseURLPairUtil.parseURLPair(payOrderDTO);
        //2、解析客户端的类型（微信、支付宝）
        //得到客户端类型
        BrowserType browserType = BrowserType.valueOfUserAgent(request.getHeader("user-agent"));
        switch (browserType) {
            case ALIPAY:
                //转发到确认页面
                return "forward:/pay-page?" + params;
            case WECHAT:
                //转发到确认页面
                return "forward:/pay-page?" + params;
            default:

        }
        //不支持客户端类型，转发到错误页面
        return "forward:/pay-page-error";
    }


    /**
     * 支付宝的下单接口,前端订单确认页面，点击确认支付，请求进来
     *
     * @param orderConfirmVO 订单信息
     * @param request
     * @param response
     */
    @ApiOperation("支付宝门店下单付款")
    @PostMapping("/createAliPayOrder")
    public void createAlipayOrderForStore(OrderConfirmVO orderConfirmVO, HttpServletRequest request, HttpServletResponse response) throws BusinessException, IOException {

        PayOrderDTO payOrderDTO = PayOrderConvert.INSTANCE.vo2dto(orderConfirmVO);
        //应用id
        String appId = payOrderDTO.getAppId();
        AppDTO app = appService.getAppById(appId);
        payOrderDTO.setMerchantId(app.getMerchantId());//商户id
        //将前端输入的元转成分
        payOrderDTO.setTotalAmount(Integer.parseInt(AmountUtil.changeY2F(orderConfirmVO.getTotalAmount().toString())));
        //客户端ip
        payOrderDTO.setClientIp(IPUtil.getIpAddr(request));
        //保存订单，调用支付渠道代理服务的支付宝下单
        PaymentResponseDTO<String> paymentResponseDTO = transactionService.submitOrderByAli(payOrderDTO);
        //支付宝下单接口响应
        String content = paymentResponseDTO.getContent();
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(content);//直接将完整的表单html输出到页面
        response.getWriter().flush();
        response.getWriter().close();
    }
}
