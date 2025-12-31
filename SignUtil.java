package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SignUtil extends AppCompatActivity {

    private TextView tvAuthHeader;
    private TextView tvTimestamp;
    private TextView tvSignature;
    private TextView tvStatus;
    private Button btnGenerateSignature;
    private Button btnCopyResult;

    private static final String SECRET_ID = "AKIDz8krbsJ5**********EXAMPLE";
    private static final String SECRET_KEY = "Gu5t9xGARNpq86cd9**********EXAMPLE";
    private static final String SERVICE_TYPE = "ocr";
    private static final String API_HOST = "ocr.tencentcloudapi.com";
    private static final String ALGO_TYPE = "TC3-HMAC-SHA256";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_util);

        initViews();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtonListeners();
    }

    private void initViews() {
        tvAuthHeader = findViewById(R.id.tv_auth_header);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        tvSignature = findViewById(R.id.tv_signature);
        tvStatus = findViewById(R.id.tv_status);
        btnGenerateSignature = findViewById(R.id.btn_generate);
        btnCopyResult = findViewById(R.id.btn_copy);
    }

    private void setupButtonListeners() {
        btnGenerateSignature.setOnClickListener(v -> {
            try {
                String requestBody = "{\"ImageBase64\":\"test_base64_data_here\"}";
                String authHeader = createAuthorizationHeader(requestBody);

                tvAuthHeader.setText("Authorization: " + authHeader);
                tvTimestamp.setText("时间戳: " + (System.currentTimeMillis() / 1000));
                tvSignature.setText("签名值: " + extractSignature(authHeader));
                tvStatus.setText("签名生成成功");

            } catch (Exception e) {
                e.printStackTrace();
                tvStatus.setText("签名生成失败: " + e.getMessage());
                Toast.makeText(this, "生成签名时出错", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyResult.setOnClickListener(v -> {
            String authHeader = tvAuthHeader.getText().toString();
            if (!authHeader.isEmpty() && !authHeader.equals("Authorization: ")) {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(
                        "Authorization Header", authHeader.replace("Authorization: ", ""));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "签名已复制到剪贴板", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "请先生成签名", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String createAuthorizationHeader(String requestBody) throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = dateFormatter.format(new Date(currentTimestamp * 1000));

        String httpMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryStr = "";
        String canonicalHeaderStr = "content-type:application/json; charset=utf-8\n" +
                "host:" + API_HOST + "\n";
        String signedHeaderList = "content-type;host";
        String hashedPayload = computeSHA256Hash(requestBody);
        String canonicalReq = httpMethod + "\n" +
                canonicalUri + "\n" +
                canonicalQueryStr + "\n" +
                canonicalHeaderStr + "\n" +
                signedHeaderList + "\n" +
                hashedPayload;

        String credScope = dateStr + "/" + SERVICE_TYPE + "/tc3_request";
        String hashedCanonicalReq = computeSHA256Hash(canonicalReq);
        String stringToSign = ALGO_TYPE + "\n" +
                currentTimestamp + "\n" +
                credScope + "\n" +
                hashedCanonicalReq;

        byte[] secretDateBytes = computeHMACSHA256(("TC3" + SECRET_KEY).getBytes("UTF-8"), dateStr);
        byte[] secretServiceBytes = computeHMACSHA256(secretDateBytes, SERVICE_TYPE);
        byte[] secretSignBytes = computeHMACSHA256(secretServiceBytes, "tc3_request");
        String signatureVal = convertBytesToHex(
                computeHMACSHA256(secretSignBytes, stringToSign));

        return ALGO_TYPE + " " +
                "Credential=" + SECRET_ID + "/" + credScope + ", " +
                "SignedHeaders=" + signedHeaderList + ", " +
                "Signature=" + signatureVal;
    }

    private String computeSHA256Hash(String inputStr) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(inputStr.getBytes("UTF-8"));
        return convertBytesToHex(hashBytes);
    }

    private byte[] computeHMACSHA256(byte[] keyBytes, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, mac.getAlgorithm());
        mac.init(keySpec);
        return mac.doFinal(message.getBytes("UTF-8"));
    }

    private String convertBytesToHex(byte[] byteArray) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : byteArray) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexBuilder.append('0');
            hexBuilder.append(hex);
        }
        return hexBuilder.toString();
    }

    private String extractSignature(String authHeader) {
        if (authHeader.contains("Signature=")) {
            return authHeader.substring(authHeader.indexOf("Signature=") + 10);
        }
        return "";
    }
}