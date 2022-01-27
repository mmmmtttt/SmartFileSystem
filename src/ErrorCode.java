import java.util.HashMap;
import java.util.Map;

public class ErrorCode extends RuntimeException{
    public static final int IO_EXCEPTION = 1;
    public static final int CHECKSUM_CHECK_FAIL = 2;
    public static final int ID_INVALID = 3;
    public static final int CREATE_DUPLICATED_FILE = 4;
    public static final int FILE_NOT_EXIST = 5;
    public static final int DATA_BLOCK_NOT_FOUND = 7;
    public static final int BLOCK_META_NOT_FOUND = 8;
    public static final int CREATE_DUPLICATED_BLOCK = 9;
    public static final int BLOCK_MANAGER_NOT_EXIST = 10;
    public static final int FILE_BROKEN = 11;//所有备份块不可用
    public static final int DATA_BLOCK_BROKEN = 12;//校验和检验失败
    public static final int CURSOR_OUT_OF_BOUND = 13;//文件指针越界
    public static final int UNSUPPORTED_OPERATION = 14;
    public static final int UNKNOWN = 1000;
    private static final Map<Integer, String> ErrorCodeMap = new HashMap<>();

    static {
        ErrorCodeMap.put(IO_EXCEPTION, "IO exception");
        ErrorCodeMap.put(CHECKSUM_CHECK_FAIL, "block checksum check failed");
        ErrorCodeMap.put(UNKNOWN, "unknown");
        ErrorCodeMap.put(ID_INVALID, "Id invalid");
        ErrorCodeMap.put(CREATE_DUPLICATED_FILE, "file already existed");
        ErrorCodeMap.put(FILE_NOT_EXIST, "file not existed");
        ErrorCodeMap.put(DATA_BLOCK_NOT_FOUND, "physical block broken");
        ErrorCodeMap.put(BLOCK_META_NOT_FOUND, "block not available");
        ErrorCodeMap.put(CREATE_DUPLICATED_BLOCK, "try to create duplicated block");
        ErrorCodeMap.put(BLOCK_MANAGER_NOT_EXIST, "blockManager not available");
        ErrorCodeMap.put(FILE_BROKEN, "file broken");
        ErrorCodeMap.put(DATA_BLOCK_BROKEN, "data block content broken");
        ErrorCodeMap.put(CURSOR_OUT_OF_BOUND, "file cursor out of bound");
        ErrorCodeMap.put(UNSUPPORTED_OPERATION, "unsupported operation");
    }

    public static String getErrorText(int errorCode) {
        return ErrorCodeMap.getOrDefault(errorCode, "invalid");
    }

    private int errorCode;

    public ErrorCode(int errorCode) {
        super(String.format("error code '%d' \"%s\"", errorCode, getErrorText(errorCode)));
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
