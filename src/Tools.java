import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

class Tools {

    /**
     * 获取File的File内容
     */
    public static byte[] smartCat(int fileId){
        try {
            MyFile file = FileID.getFileById(fileId);
            int origin = file.fCursor.bytePos;
            file.move(0,MyFile.MOVE_HEAD);
            byte[] content = file.read(file.size);
            //恢复文件光标
            file.fCursor.update(origin);
            return content;
        }catch (ErrorCode e){
            System.out.println(ErrorCode.getErrorText(e.getErrorCode()));
            return new byte[0];
        }
    }

    /**
     * 读取block的data并用16进制的形式打印到控制台
     * @param blockStr 格式如BM1.1
     */
    public static void smartHex(String blockStr){
        try {
            Block block = findBlockFromName(blockStr);
            byte[] content = block.read();
            System.out.println(DatatypeConverter.printHexBinary(content));
        }catch (ErrorCode errorCode){
            System.out.println(ErrorCode.getErrorText(errorCode.getErrorCode()));
        }
    }

    /**
     * 将写入指针移动到指定位置后，开始读取用户数据，并且写入到文件中
     */
    public static void smartWrite(int offset, int where, int fileId){
        MyFile file = FileID.getFileById(fileId);
        file.move(offset,where);
        Scanner scan = new Scanner(System.in);
        System.out.println("请输入写入内容：");
        String input = "";
        if (scan.hasNextLine()) {
            input += scan.nextLine();
        }
        byte[] content = input.getBytes(StandardCharsets.UTF_8);
        file.write(content);
    }

    /**
     * 复制File到另一个File,直接复制File的FileMeta
     */
    public static MyFile smartCopy(int fileId){
        try {
            MyFile src = FileID.getFileById(fileId);
            //新的file加在同一个fm下，修改file.meta
            MyFile dest = new MyFile(src);
            return dest;
        }catch (ErrorCode errorCode){
            System.out.println(ErrorCode.getErrorText(errorCode.getErrorCode()));
            return null;
        }
    }

    /**
     * 查看文件系统结构，包括:
     * 每个FileManager下管理的文件
     * 每个BlockManager下管理的 block
     * 和每个file使用的logic block
     */
    public static void smartLs(){
        System.out.println("---------------------start---------------------");
        System.out.println("\u001B[33m" + "Block Managers:"+"\u001B[0m");
        FileSystem.blockManagerMap.forEach((bmId,bm)-> {
            System.out.print("BM" + bmId + ":");
            bm.blockMap.forEach((blkId,blk)-> System.out.print("b"+blkId+" "));
            System.out.println();
        });
        System.out.println();
        System.out.println("\u001B[33m" + "File Managers:"+"\u001B[0m");
        FileSystem.fileManagerMap.forEach((fmId,fm)-> {
            System.out.print("FM" + fmId + ":");
            fm.fileMap.forEach((fileId,file)-> System.out.print("f"+fileId+" "));
            System.out.println();
        });
        System.out.println();
        System.out.println("\u001B[33m"+ "Files:"+"\u001B[0m");
        FileSystem.fileManagerMap.forEach((fmId,fm)-> {
            fm.fileMap.forEach((fileId,file)-> {
                System.out.println("f"+ fileId+":");
                for(Block[] logicalBlk:file.logicalBlocks){
                    for(Block blk:logicalBlk){
                        if (blk==null){
                            System.out.print("BMbroken");
                        }else {
                            System.out.print("BM"+blk.getBlockManager().bmId+"."+blk.getIndex()+" ");
                        }
                    }
                    System.out.println();
                }
            });
        });
        System.out.println("----------------------end----------------------\n");
    }

    /**
     * 用CRC32算法得到hash值
     */
    public static long getHash(byte[] b){
        Checksum crc32 = new CRC32();
        crc32.update(b, 0, b.length);
        return crc32.getValue();
    }

    /**
     * 根据格式如BM1.1的字符串返回对应的块对象
     * @throws ErrorCode BLOCKMANAGER_NOT_EXIST ID_INVALID BLOCK_META_BROEKN
     */
    public static Block findBlockFromName(String blkInfo) throws ErrorCode {
        String[] info = blkInfo.split("BM|\\.");
        int bmId = Integer.parseInt(info[1]);
        int blkId = Integer.parseInt(info[2]);
        BlockManager bm = FileSystem.blockManagerMap.get(bmId);
        if (bm == null) {
            throw new ErrorCode(ErrorCode.BLOCK_MANAGER_NOT_EXIST);
        }
        return bm.getBlock(blkId);//可能会抛出异常
    }
}
