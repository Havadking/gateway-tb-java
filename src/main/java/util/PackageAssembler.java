package util;

/**
 * @program: gateway-netty
 * @description: 用于处理多包消息的辅助类
 * @author: Havad
 * @create: 2025-02-15 16:31
 **/

public class PackageAssembler {

    private final byte[][] packages;
    private int receivedCount = 0;

    public PackageAssembler(int totalPackages) {
        this.packages = new byte[totalPackages][];
    }

    public void addPackage(int packageIndex, byte[] data) {
        if (packages[packageIndex] == null) {
            packages[packageIndex] = data;
            receivedCount++;
        }
    }

    public boolean isComplete() {
        return receivedCount == packages.length;
    }

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
