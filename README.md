文件结构：
Android-OCR-Homework
├─ manifests/
├─ AndroidManifest.xml
├─ java/
│ └─ com/example/myapplication
│ ├─ MainActivity1.java
│ ├─ OcrApiService.java
│ ├─ OcrSdkHelper.java
│ ├─ Base64Util.java
│ ├─ IdentifyResult.java
│ └─ SignUtil.java
├─ res/
│ ├─ layout/
│ │ ├─ activity_main1.xml
│ │ └─ activity_sign_util.xml
│ └─ xml/
│ └─ file_paths.xml
├─ gradle scripts/
│ └─ build.gradle.kts

本仓库为课程作业展示，仅包含与身份证 OCR 功能直接相关的核心代码。  
所有涉及密钥的类均已脱敏处理，替换为占位符后可在本地运行。

功能模块及代码说明：
1. MainActivity1.java
- 应用入口 Activity，负责主界面和核心流程控制  
- 核心功能：
  1. 界面初始化，绑定控件（ImageView、按钮等）  
  2. 拍摄身份证照片：
     - 检查相机权限
     - 调用系统相机拍照
     - 保存照片到本地并展示预览
  3. OCR 识别：
     - 将拍摄的 Bitmap 转为 byte[]
     - 使用 `Base64Util` 编码为 Base64
     - 调用 `OcrSdkHelper` 识别身份证信息
     - 将 SDK 返回结果封装为 `IdentifyResult` 对象
     - 跳转到结果展示界面（`ResultActivity`）
  4. 提供按钮跳转到签名生成演示界面（`SignUtil`）
- 注意：
  - 图片分辨率过低会提示重新拍照  
  - 线程异步处理 OCR 调用，避免阻塞 UI

2. OcrApiService.java
- OCR HTTP 请求示例类（未最终使用）  
- 展示了基于 OkHttp 调用腾讯云 OCR REST API 的请求结构与参数  
- 核心方法：
  1. `sendOcrRequest(String base64Img)`：发送 HTTP POST 请求，返回 OCR 原始 JSON 响应  
  2. `convertJsonToResult(String json)`：将 JSON 响应解析为 `IdentifyResult` 对象  
- 主要用于说明 REST API 请求流程和 JSON 解析逻辑  
- 注意：实际作业运行使用 `OcrSdkHelper` 和官方 Java SDK，`OcrApiService` 仅作演示参考

3. OcrSdkHelper.java
- OCR SDK 辅助类，用于调用腾讯云身份证识别接口  
- 提供静态方法 `idCardOcr(String base64Img)`，接收 Base64 编码图片，返回识别结果对象 `IDCardOCRResponse`  
- 核心逻辑：
  1. 使用 `Credential` 初始化 SDK（密钥已脱敏，占位符替代 `SECRET_ID` / `SECRET_KEY`）  
  2. 配置网络请求参数 (`HttpProfile` / `ClientProfile`)  
  3. 调用 `OcrClient.IDCardOCR` 获取身份证识别结果  
- 密钥敏感，仓库中不提供真实密钥，替换占位符后可在本地运行  

4. Base64Util.java
- 自定义 Base64 编码工具类  
- 提供静态方法 `encode(byte[] from)`，将字节数组转换为 Base64 字符串  
- 主要用于将图片数据转换成 Base64，以便发送到 OCR 接口  

5. IdentifyResult.java
- OCR 返回结果封装类，实现 `Serializable` 接口  
- 用于存储身份证识别结果，包括：
  - `name`：姓名
  - `sex`：性别
  - `nation`：民族
  - `birth`：出生日期
  - `address`：住址
  - `id`：身份证号码
- 同时包含错误码 `errorcode` 和错误信息 `errormsg`  
- 使用 `@SerializedName` 注解，方便 JSON 与对象字段映射  

6. SignUtil.java
- 签名生成演示类，继承 `AppCompatActivity`  
- 提供界面用于生成 Tencent Cloud API 请求的 TC3-HMAC-SHA256 签名  
- 核心功能：
  1. `createAuthorizationHeader(String requestBody)`：根据请求体生成 Authorization Header  
  2. SHA256 哈希与 HMAC-SHA256 签名计算  
  3. 显示签名、时间戳及状态信息  
  4. 支持将生成的签名复制到剪贴板  

布局与资源文件说明：
- `res/layout/activity_main1.xml`：主界面布局，包含拍照、OCR 按钮和图片预览控件  
- `res/layout/activity_sign_util.xml`：签名演示界面布局  
- `res/xml/file_paths.xml`：文件路径配置，用于图片选择和存储  
- `AndroidManifest.xml`：权限声明（网络、相机）与 Activity 注册  
- `build.gradle.kts`：项目构建文件  




