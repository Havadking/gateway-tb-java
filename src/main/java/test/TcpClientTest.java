package test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClientTest {

    public static void main(String[] args) {
        try {
            // 1. 创建连接到 Netty 服务器的 Socket
            String host = "127.0.0.1"; // 服务器地址
            int port = 8080;           // 服务器端口
            Socket socket = new Socket(host, port);
            System.out.println("成功连接到服务器: " + host + ":" + port);

            // 2. 获取输出流，用于发送数据
            OutputStream out = socket.getOutputStream();

            // 构造测试消息：4字节长度 + JSON内容
            String jsonMessage = "{\"type\":\"HEARTBEAT\",\"content\":\"测试数据\"}";
            byte[] payload = jsonMessage.getBytes(StandardCharsets.UTF_8);
            int length = payload.length;

            // 将长度字段（4 字节）和消息体写入输出流
            out.write(new byte[]{
                    (byte) (length >> 24),
                    (byte) (length >> 16),
                    (byte) (length >> 8),
                    (byte) (length)
            });
            out.write(payload);
            out.flush();
            System.out.println("发送数据到服务器: " + jsonMessage);

            // 3. 获取输入流，用于接收数据
            InputStream in = socket.getInputStream();

            // 接收服务器的响应
            byte[] lengthBytes = new byte[4];
            if (in.read(lengthBytes) != 4) {
                System.err.println("未能正确读取到长度字段");
                return;
            }

            // 将长度字段转换为 int
            int responseLength = ((lengthBytes[0] & 0xFF) << 24) |
                    ((lengthBytes[1] & 0xFF) << 16) |
                    ((lengthBytes[2] & 0xFF) << 8) |
                    (lengthBytes[3] & 0xFF);

            // 读取响应的消息体
            byte[] responsePayload = new byte[responseLength];
            int bytesRead = in.read(responsePayload);
            if (bytesRead != responseLength) {
                System.err.println("未能正确读取完整的响应消息");
                return;
            }

            String responseMessage = new String(responsePayload, StandardCharsets.UTF_8);
            System.out.println("收到服务器响应: " + responseMessage);

            // 4. 关闭连接
            socket.close();
            System.out.println("连接已关闭");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


