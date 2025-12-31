package com.example.myapplication;

import android.util.Log;

import org.json.JSONObject;

import okhttp3.*;

import java.util.concurrent.TimeUnit;

/**
 * 基于 HTTP 的腾讯云 OCR 调用示例
 * 本项目最终版本采用腾讯云官方 Java SDK（V3 签名）
 * 本类用于说明 OCR REST API 的请求结构与参数组成,未最终使用
 */

public class OcrApiService {

    private static final String TAG = "TencentOCR";
    private static final String API_ENDPOINT = "https://ocr.tencentcloudapi.com/";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public static String sendOcrRequest(String base64Img) throws Exception {

        String authToken = "TC3-HMAC-SHA256 Credential=AKIDxxxx/2024-01-01/ocr/tc3_request, SignedHeaders=content-type;host, Signature=xxxx";

        JSONObject body = new JSONObject();
        body.put("ImageBase64", base64Img);
        body.put("CardSide", "FRONT");

        RequestBody reqBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_ENDPOINT)
                .post(reqBody)
                .addHeader("Authorization", authToken)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Host", "ocr.tencentcloudapi.com")
                .addHeader("X-TC-Action", "IDCardOCR")
                .addHeader("X-TC-Version", "2018-11-19")
                .addHeader("X-TC-Region", "ap-beijing")
                .build();

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new Exception("HTTP 错误：" + response.code());
        }

        String result = response.body().string();
        Log.e(TAG, "OCR_RAW = " + result);
        return result;
    }

    public static IdentifyResult convertJsonToResult(String json) throws Exception {

        JSONObject root = new JSONObject(json);
        JSONObject response = root.getJSONObject("Response");

        // 1. 错误处理（腾讯云规范）
        if (response.has("Error")) {
            JSONObject err = response.getJSONObject("Error");
            throw new Exception(err.optString("Message", "OCR识别失败"));
        }

        // 2. 正确字段映射（严格按 IdentifyResult）
        IdentifyResult result = new IdentifyResult();

        result.setName(response.optString("Name"));
        result.setSex(response.optString("Sex"));
        result.setNation(response.optString("Nation"));
        result.setBirth(response.optString("Birth"));
        result.setAddress(response.optString("Address"));
        result.setId(response.optString("IdNum"));

        return result;
    }
}
