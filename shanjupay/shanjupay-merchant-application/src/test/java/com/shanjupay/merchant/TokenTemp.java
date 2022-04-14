package com.shanjupay.merchant;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import org.apache.dubbo.config.annotation.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Administrator
 * @version 1.0
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
public class TokenTemp {
    @Reference
    MerchantService merchantService;

    //生成token，指定商户id
    @Test
    public void createTestToken() {
        Long merchantId = 1511703687811895297L;//填写用于测试的商户id
        MerchantDTO merchantDTO = merchantService.queryMerchantById(merchantId);
        JSONObject token = new JSONObject();
        token.put("mobile", merchantDTO.getMobile());
        token.put("user_name", merchantDTO.getUsername());
        token.put("merchantId", merchantId);

        String jwt_token = "Bearer " + EncryptUtil.encodeBase64(JSON.toJSONString(token).getBytes());
        System.out.println(jwt_token);
    }
}
