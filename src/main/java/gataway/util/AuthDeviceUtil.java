package gataway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


/**
 * @program: gateway-netty
 * @description: 用于进行设备的认证
 * @author: Havad
 * @create: 2025-02-07 11:11
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class AuthDeviceUtil {

    /**
     * 调用设备认证接口，返回 content 字段的值
     *
     * @param deviceNo 设备编号
     * @return 接口返回的 content 字段值
     * @throws Exception 请求或解析过程中出现异常时抛出异常
     */
    public static boolean getDeviceAuth(String deviceNo) throws Exception {
        // 构造请求 URL
        InputStream is = getStream(deviceNo);
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        String response = responseBuilder.toString();

        // 使用 Jackson 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);
        return rootNode.path("content").asBoolean();
    }

    /**
     * 获取设备授权测试结果
     *
     * @param deviceNo 设备编号
     * @return 设备授权测试是否通过，真表示通过，假表示不通过
     * @throws Exception 方法执行过程中遇到异常时抛出
     */
    public static boolean getDeviceAuthTest(String deviceNo) throws Exception {
        return true;
    }

    /**
     * 获取指定设备编号的数据流
     *
     * @param deviceNo 设备编号
     * @return 对应设备编号的数据流
     * @throws IOException 当创建连接或获取输入流失败时抛出
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static InputStream getStream(String deviceNo) throws IOException {
        String urlString = "https://rest.xxt.cn/hardware-business/receive/device-auth?deviceNo=" + deviceNo;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // 读取响应
        int responseCode = connection.getResponseCode();
        return (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream() : connection.getErrorStream();
    }


    /**
     * 调用认证接口，返回 token 的值
     *
     * @return token 字符串
     * @throws Exception 请求或解析过程中出现异常时抛出异常
     */
    private static String getToken() throws Exception {
        // 认证接口地址
        String urlString = "https://rest.xxt.cn/hardware-business/manager/identity-auth";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方式和请求头
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        // 固定地请求参数
        String payload = "{\"clientId\": \"xxt\", \"clientSecret\": \"123456\"}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // 读取响应
        String response = getResponse(connection);

        // 使用 Jackson 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode outerJson = mapper.readTree(response);
        // 外层返回的 message 字段是一个 JSON 格式的字符串，需要再次解析
        String messageStr = outerJson.path("message").asText();
        JsonNode innerJson = mapper.readTree(messageStr);
        // 返回 token 的值
        return innerJson.path("token").asText();
    }

    /**
     * 获取HTTP连接响应内容的方法
     *
     * @param connection HTTP连接对象
     * @return 连接的响应内容字符串
     * @throws IOException 当读取响应内容时发生I/O错误
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static String getResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        return responseBuilder.toString();
    }
}
