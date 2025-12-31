package com.example.myapplication;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.IDCardOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.IDCardOCRResponse;

public class OcrSdkHelper {

/**
 * 本类为 OCR SDK 调用示例。
 * 实际使用时请在本地填写 SecretId 与 SecretKey，
 * 出于安全原因仓库中不提供真实密钥。
 */
    private static final String SECRET_ID = "YOUR_SECRET_ID";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";

    private static final String REGION = "ap-beijing";

    /**
     * 身份证正面识别
     */
    public static IDCardOCRResponse idCardOcr(String base64Img)
            throws TencentCloudSDKException {

        Credential cred = new Credential(SECRET_ID, SECRET_KEY);

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ocr.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        OcrClient client = new OcrClient(cred, REGION, clientProfile);

        IDCardOCRRequest req = new IDCardOCRRequest();
        req.setImageBase64(base64Img);
        req.setCardSide("FRONT");

        return client.IDCardOCR(req);
    }
}
