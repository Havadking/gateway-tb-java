package gataway.util;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔、掇月普通话机的编解码工具类
 * @author: Havad
 * @create: 2025-02-07 16:47
 **/
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class PDUUtil {

    /**
     * 解析PDU字符串(测试用)
     *
     * @param pdu 需要解析的PDU字符串
     * @throws IllegalArgumentException 当PDU字符串为空或长度小于15时抛出异常
     */
    @SuppressWarnings({"checkstyle:ReturnCount", "checkstyle:MagicNumber"})
    public static void parsePDU(String pdu) {
        if (pdu == null || pdu.length() < 15) {
            LogUtils.logBusiness("无效的 PDU 数据！");
            return;
        }

        // 1. 提取 pre（固定 4 字符）
        String pre = pdu.substring(0, 4);

        // 2. 提取 length（4 字符），转为整数
        String lengthStr = pdu.substring(4, 8);
        int length;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            LogUtils.logError("长度字段解析失败：{}", e, lengthStr);
            return;
        }

        int expectedTotalLength = length;
        if (pdu.length() != expectedTotalLength) {
            LogUtils.logBusiness("【警告】实际 PDU 长度({})与期望长度({})不符！", pdu.length(), expectedTotalLength);
        }

        // 3. 提取 comm_type（1 字符）
        int index = 8;  // 当前解析位置（已解析 pre 与 length）
        String commTypeStr = pdu.substring(index, index + 1);
        int commType;
        try {
            commType = Integer.parseInt(commTypeStr);
        } catch (NumberFormatException e) {
            LogUtils.logError("comm_type 字段解析失败：{}", e, commTypeStr);
            return;
        }
        index += 1;

        // 4. 提取 func_no（2 字符）
        String funcNo = pdu.substring(index, index + 2);
        index += 2;

        // 5. 提取 seq_no（4 字符），转为整数
        String seqNoStr = pdu.substring(index, index + 4);
        int seqNo;
        try {
            seqNo = Integer.parseInt(seqNoStr);
        } catch (NumberFormatException e) {
            LogUtils.logError("seq_no 字段解析失败：{}", e, seqNoStr);
            return;
        }
        index += 4;


        // 6. 按照说明，body 长度 = (length 字段值 – 14)
        int bodyLength = length - 14 - 1 - 4;
        if (bodyLength < 0 || index + bodyLength > pdu.length()) {
            LogUtils.logError("PDU 数据不完整，无法解析 body 字段！", new Throwable());
            return;
        }
        String body = pdu.substring(index, index + bodyLength);
        index += bodyLength;

        // 7. 提取校验码 check（最后 4 字符）
        if (index + 4 > pdu.length()) {
            LogUtils.logError("PDU 数据不完整，无法解析 check 字段！", new Throwable());
            return;
        }
        String check = pdu.substring(index, index + 4);

        // 输出各字段
        LogUtils.logBusiness("pre: {}", pre);
        LogUtils.logBusiness("length: {}", length);
        LogUtils.logBusiness("comm_type: {}", commType);
        LogUtils.logBusiness("func_no: {}", funcNo);
        LogUtils.logBusiness("seq_no: {}", seqNo);
        LogUtils.logBusiness("body: {}", body);
        LogUtils.logBusiness("check: {}", check);
    }

    /**
     * 获取 PDU 中的 length 字段值
     *
     * @param pdu 需要解析的 PDU 字符串
     * @return length 字段对应的整数值
     * @throws IllegalArgumentException 当 PDU 数据无效或长度不足以获取 length 字段时抛出异常
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static int getLength(String pdu) {
        if (pdu == null || pdu.length() < 8) {
            throw new IllegalArgumentException("PDU 数据无效或长度不足以获取 length 字段！");
        }
        String lengthStr = pdu.substring(4, 8);
        try {
            return Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析 length 字段：" + lengthStr, e);
        }
    }

    /**
     * 获取 PDU 中的 func_no 字段值 (暂时不用)
     *
     * @param pdu 需要解析的 PDU 字符串
     * @return func_no 字段对应的字符串（2 个字符）
     * @throws IllegalArgumentException 当 PDU 数据无效或长度不足以获取 func_no 字段时抛出异常
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static int getFuncNo(String pdu) {
        // func_no 固定位于索引 9 到 11（2 个字符）
        if (pdu == null || pdu.length() < 11) {
            throw new IllegalArgumentException("PDU 数据无效或长度不足以获取 func_no 字段！");
        }
        return Integer.parseInt(pdu.substring(9, 11));
    }

    /**
     * 获取 PDU 中的 body 字段值
     * <p>
     * 根据协议：
     * - pre：固定 4 字符
     * - length：4 字符
     * - comm_type：1 字符
     * - func_no：2 字符
     * - seq_no：4 字符
     * 因此前 15 个字符固定，
     * 协议中说明 body 长度 = (length 字段值 – 14)，
     * 但在你的代码中额外扣除了 comm_type（1 字符）和 check（4 字符），
     * 故此计算 body 长度为： length - 14 - 1 - 4 = length - 19。
     *
     * @param pdu 需要解析的 PDU 字符串
     * @return body 字段对应的字符串
     * @throws IllegalArgumentException 当 PDU 数据无效或无法获取完整 body 字段时抛出异常
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static String getBody(String pdu) {
        int length = getLength(pdu);
        if (pdu.length() != length) {
            LogUtils.logBusiness("【警告】实际 PDU 长度({})与 length 字段({})不符！", pdu.length(), length);
        }
        // 计算 body 长度：length - 14 - comm_type(1) - check(4) = length - 19
        int bodyLength = length - 19;
        if (bodyLength < 0) {
            throw new IllegalArgumentException("计算得到的 body 长度为负数：" + bodyLength);
        }
        // body 从索引 15 开始（pre(4) + length(4) + comm_type(1) + func_no(2) + seq_no(4) = 15）
        int startIndex = 15;
        // 检查 body 部分是否越界：剩下的部分中还需包含 4 字符的 check 字段
        if (startIndex + bodyLength > pdu.length() - 4) {
            throw new IllegalArgumentException("PDU 数据不完整，无法获取完整的 body 字段！");
        }
        return pdu.substring(startIndex, startIndex + bodyLength);
    }

    /**
     * 获取 PDU 中的 check 字段值
     * <p>
     * check 字段为 PDU 的最后 4 个字符
     *
     * @param pdu 需要解析的 PDU 字符串
     * @return check 字段对应的字符串（4 个字符）
     * @throws IllegalArgumentException 当 PDU 数据无效或长度不足以获取 check 字段时抛出异常
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static String getCheck(String pdu) {
        int length = getLength(pdu);
        if (pdu.length() < 4 || length < 4) {
            throw new IllegalArgumentException("PDU 数据长度不足，无法获取 check 字段！");
        }
        // 根据你的代码逻辑，check 字段从索引 (length - 4) 到 (length)
        return pdu.substring(length - 4, length);
    }


    /**
     * 校验 PDU
     *
     * @param pdu PDU 数据字符串
     * @return 校验成功返回 true，否则返回 false
     */
    @SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:ReturnCount"})
    public static boolean validateCheck(String pdu) {
        if (pdu == null || pdu.length() < 15 || !pdu.startsWith("*#F#")) {
            LogUtils.logError("无效的 PDU 数据！", new Throwable());
            return false;
        }


        // 1. 提取 length 字段（4 个字符，位于索引 4~8），并转换为整数
        int length = getLength(pdu);

        // 2. 提取 check 字段（最后 4 个字符）
        String checkHex = getCheck(pdu);

        // 3. 将 check 字段从 16 进制转换为 10 进制
        int checkVal;
        try {
            checkVal = Integer.parseInt(checkHex, 16);
        } catch (NumberFormatException e) {
            LogUtils.logError("check 字段解析失败，非有效16进制：{}", e, checkHex);
            return false;
        }

        // 4. 比较 length - 4 与转换后的 check 值是否相等
        int computedValue = length - 4;
        if (computedValue == checkVal) {
//            LogUtils.logBusiness("校验成功: length-4 = {} 与 check (10进制) = {} 相等", computedValue, checkVal);
            return true;
        } else {
            LogUtils.logError("校验失败: length-4 = {} 不等于 check (10进制) = {}", new Throwable(), computedValue, checkVal);
            return false;
        }
    }


    /**
     * 根据PDU获取设备编号(只用于发往设备发往终端的信息)
     *
     * @param pdu PDU字符串
     * @return 获取到的设备编号，若PDU长度小于18或处理失败，可能返回空字符串
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static String getDeviceNo(String pdu) {
        if (pdu.length() < 18) {
            LogUtils.logError("方法{}, 获取设备编号失败，解析失败的pdu为{}", new Throwable(), "getDeviceNo", pdu);
        }
        String body = getBody(pdu);
        return body.substring(0, 18).replaceAll("\\s+", "");
    }


    public static void main(String[] args) {
        // 示例 PDU 数据
        String pdu = "*#F#00551100011864603061185738   VER8.43 2024/05/280033";
        String callPUD = "*#F#00661070002864603061185738   17651979715    20250212113310003E";
//        parsePDU(pdu);
//        LogUtils.logBusiness(String.valueOf(getCheck(pdu)));
//        LogUtils.logBusiness(getBody(pdu));
//        LogUtils.logBusiness(String.valueOf(getFuncNo(pdu)));
//        LogUtils.logBusiness(String.valueOf(getLength(pdu)));
        boolean isValidate = validateCheck(pdu);
        LogUtils.logBusiness(String.valueOf(isValidate));
//        LogUtils.logBusiness(getDeviceNo(pdu));
    }
}
