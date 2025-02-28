package gataway.util;

/**
 * @program: gateway-netty
 * @description: 用于处理多包消息的辅助类
 * @author: Havad
 * @create: 2025-02-15 16:31
 **/

public class PackageAssembler {

    /**
     * 数据包数组，用于存储各个数据包的内容。
     */
    private final byte[][] packages;
    /**
     * 已接收数量
     */
    private int receivedCount = 0;

    public PackageAssembler(int totalPackages) {
        this.packages = new byte[totalPackages][];
    }

    /**
     * 向指定索引位置添加数据包
     *
     * @param packageIndex 数据包索引位置
     * @param data         要添加的数据包内容
     */
    public void addPackage(int packageIndex, byte[] data) {
        if (packages[packageIndex] == null) {
            packages[packageIndex] = data;
            receivedCount++;
        }
    }

    public boolean isComplete() {
        return receivedCount == packages.length;
    }

    /**
     * 获取完整的消息内容。
     * <p>
     * 该方法通过遍历字节包数组，计算总长度，并复制所有字节包到一个新的字节数组中。
     *
     * @return 合并后的完整消息字节数组
     */
    public byte[] getCompleteMessage() {
        int totalLength = 0;
        for (byte[] pkg : packages) {
            totalLength += pkg.length;
        }

        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] pkg : packages) {
            System.arraycopy(pkg, 0, result, offset, pkg.length);
            offset += pkg.length;
        }
        return result;
    }
}
