public class Buffer {
    /*
    0->2 读一次磁盘 0->1 写一次数据
    1->2 同步一次
    2->1 写一次数据
     */
    //buffer内容无效，需要从磁盘读写；初始化时是invalid,之后只会是后两种状态
    public static final int INVALID = 0;
    //buffer的修改待写回磁盘，close时需要同步，正常读写
    public static final int DELAY_WRITE = 1;
    //与磁盘上内容一致，正常读写，不需同步
    public static final int VALID = 2;
    public int status;
    public byte[] content;

    Buffer(){
        status = INVALID;
    }
}
