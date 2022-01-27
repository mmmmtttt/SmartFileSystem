import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MyFile {
    //光标的三个枚举值，用来标识move移动的相对起点，具体数值无实际意义
    public static final int MOVE_CURR = 0;
    public static final int MOVE_HEAD = 1;
    public static final int MOVE_TAIL = 2;
    FileID fileId;
    int size;
    Cursor fCursor;
    FileManager fileManager;
    ArrayList<Block[]> logicalBlocks;

    /**
     * 创建新的file对象并且在物理层创建 fileId.meta
     *
     * @throws ErrorCode FILE_DUPLICATED SYSTEM_IO_FAIL
     */
    MyFile(FileManager fileManager, int fileId) throws ErrorCode {
        size = 0;
        logicalBlocks = new ArrayList<>();
        this.fileManager = fileManager;
        this.fileId = new FileID(fileId, this);
        fCursor = new Cursor();
        writeFileMeta();
    }

    /**
     * 在系统启动时从fmid.meta中恢复MyFile的id,size,logicalBlock
     * 格式举例：
     * fileId:2
     * size:32
     * logicalBlocks:
     * BM1.1 BM2.1
     * BM2.2 BM1.3
     */
    MyFile(File fileMeta, FileManager fm) {
        fileManager = fm;
        //从file.meta中读内容
        String content = ";";
        //默认fileMeta不会损坏，因此异常在内部处理
        try (BufferedReader br = new BufferedReader(new FileReader(fileMeta))) {
            String line;
            while ((line = br.readLine()) != null) {
                content += line + ";";
            }
            //分别读取id,size,block
            String[] contents = content.split(";\\w+?:");
            fileId = new FileID(Integer.parseInt(contents[1]), this);
            size = Integer.parseInt(contents[2]);
            fCursor = new Cursor();
            logicalBlocks = new ArrayList<>();
            loadLogicalBlocksFromString(contents[3]);
        } catch (IOException e) {
            System.out.println(ErrorCode.getErrorText(ErrorCode.IO_EXCEPTION));
        }
    }

    /**
     * 从origin的myfile对象中拷贝除了fileid之外的所有信息
     */
    MyFile(MyFile origin) {
        this.fileId = new FileID(this);//分配id
        this.size = origin.size;
        this.fileManager = origin.fileManager;
        this.fileManager.fileMap.put(fileId.fildId, this);
        this.logicalBlocks = new ArrayList<>(origin.logicalBlocks);
        fCursor = new Cursor();
        writeFileMeta();
    }

    /**
     * 从文件光标处开始读取length字节长度的file内容
     * 超过size读取到size
     * 文件损坏，所有备份块不可用，返回空数组
     */
    byte[] read(int length) {
        if (length <= 0) {
            return new byte[0];
        }
        int endByte = Math.min(fCursor.bytePos + length, size);//如果length超过file长度就返回file长度的内容
        int realLength = Math.max(endByte - fCursor.bytePos, 0);//真正要返回的字节数
        try {
            Cursor end = new Cursor(endByte);
            byte[] bytes = readBlocks(Math.max(fCursor.blkIndex, 0), end.blkIndex);
            //截取要读的字节处的数组
            byte[] content = Arrays.copyOfRange(bytes, fCursor.offset, realLength + fCursor.offset);
            fCursor.update(fCursor.bytePos + realLength);
            return content;
        } catch (ErrorCode errorCode) {//文件损坏 FILE_BROKEN
            System.out.println(errorCode.getErrorText(errorCode.getErrorCode()));
            return new byte[0];
        }
    }

    /**
     * 从文件光标处插入式的写入字节
     * 对每个要写入的逻辑块用hash函数从可用的bm中分配3个bm，写入buffer
     * 成功写入后更改file.meta，在逻辑块列表中加入块信息
     */
    void write(byte[] b) {
        if (b.length == 0) {
            return;
        }
        //光标在块中间，把光标移动到块开始处，插入重组的内容（转换成光标在块边界）
        if (!fCursor.atBlockBoundary) {
            byte[] cursorBlk = readBlocks(fCursor.blkIndex, fCursor.blkIndex);//得到光标处的块的blk.data
            byte[] newInsert = new byte[b.length + cursorBlk.length];
            System.arraycopy(cursorBlk, 0, newInsert, 0, fCursor.offset);//拷贝前一半
            System.arraycopy(b, 0, newInsert, fCursor.offset, b.length);//加上中间
            System.arraycopy(cursorBlk, fCursor.offset, newInsert, fCursor.offset + b.length, cursorBlk.length - fCursor.offset);//拷贝后一半
            deleteBlksFromList(fCursor.blkIndex, fCursor.blkIndex);//在逻辑块列表中删除后面的块
            insertBytesToBlk(newInsert, fCursor.blkIndex);//把新组装的bytes插入光标所在blk前
        } else//光标在块边界
            insertBytesToBlk(b, fCursor.blkIndex + 1);//把新组装的bytes插入光标所在blk前
        //成功修改BM和block信息后，修改file对象信息
        fCursor.update(fCursor.bytePos + b.length);
        size += b.length;
        writeFileMeta();//重写file.meta
    }

    /**
     * 把文件光标移到距离where offset个byte的位置，并返回文件光标所在位置
     * where是三种模式之一，offset可正可负，移动后在[0,size]之内
     */
    int move(int offset, int where) {
        int afterMove;
        switch (where) {
            case MOVE_HEAD:
                afterMove = offset;
                break;
            case MOVE_CURR:
                afterMove = fCursor.bytePos + offset;
                break;
            case MOVE_TAIL:
                afterMove = size + offset;
                break;
            default://输入参数非法
                System.out.println(ErrorCode.getErrorText(ErrorCode.UNSUPPORTED_OPERATION));
                return fCursor.bytePos;
        }
        if (afterMove >= 0 && afterMove <= size) {
            fCursor.update(afterMove);
        } else {//光标移动到合理范围外
            System.out.println(ErrorCode.getErrorText(ErrorCode.CURSOR_OUT_OF_BOUND));
        }
        return fCursor.bytePos;
    }

    /**
     * 如果size大于file size，那么新增的字节应该全为0x00
     * 如果size小于原来的file size，修改file meta中对应的logic block,文件光标如果大于新的size就移动到结尾
     */
    void setSize(int newSize) {
        if (newSize == size) {
            return;
        }
        if (newSize > size) {
            byte[] append = new byte[newSize - size];
            Arrays.fill(append, (byte) 0x00);
            Cursor fileEnd = new Cursor(size);
            insertBytesToBlk(append, fileEnd.blkIndex + 1);
            size += append.length;
            writeFileMeta();
            return;
        }
        if (newSize < size) {
            Cursor newEnd = new Cursor(newSize);
            if (!newEnd.atBlockBoundary) {//新的大小不是整块数
                //读出来最后一块的前一半
                byte[] endBlock = readBlocks(newEnd.blkIndex, newEnd.blkIndex);
                byte[] halfEndBlk = Arrays.copyOfRange(endBlock, 0, newEnd.offset);
                deleteBlksFromList(newEnd.blkIndex, logicalBlocks.size() - 1);//在逻辑块列表中删除之后的所有块
                //用前一半创建新的块作为更改后的最后一块
                insertBytesToBlk(halfEndBlk, newEnd.blkIndex);
            } else {//新的大小是整块数
                deleteBlksFromList(newEnd.blkIndex + 1, logicalBlocks.size() - 1);
            }
            size = newSize;
            fCursor.update(Math.min(newSize, fCursor.bytePos));
            writeFileMeta();
        }
    }

    /**
     * 检查所有块，强制同步buffer到磁盘上
     */
    void flush() {
        for (Block[] blks : logicalBlocks) {
            for (Block blk : blks) {
                if (blk != null)
                    blk.bufferSync();
            }
        }
    }

    /**
     * 读取文件的逻辑块列表中序号在[startBlk,endBlk]内的块内容
     *
     * @throws ErrorCode FILE_BROKEN
     */
    private byte[] readBlocks(int startBlk, int endBlk)
            throws ErrorCode {
        byte[] content = new byte[(endBlk - startBlk + 1) * FileSystem.blockSize];//不一定读满
        int pos = 0;//当前写到content数组的位置
        for (int i = startBlk; i <= endBlk; i++) {
            Block[] blocks = logicalBlocks.get(i);//得到第i+1个逻辑块的备份块列表
            int backupNumber = blocks.length;
            int tryTime = 0;
            //依次读取备份块，找到可用的块
            for (Block block : blocks) {
                try {
                    //从block对象去读buffer的内容,可能抛出DATA_BLOCK_NOT_FOUND SYSTEM_IO_FAIL DATA_BLOCK_BROKEN
                    byte[] result = block.read();
                    System.arraycopy(result, 0, content, pos, result.length);
                    pos += result.length;
                    break;//成功就去读下一个逻辑块
                } catch (Exception e) {//有任何异常就尝试读其他备份块(备份块有可能在写的时候出错，=null)
                    tryTime++;
                    if (tryTime == backupNumber) {//某块逻辑块的块全部不可用，抛出文件损坏的异常
                        throw new ErrorCode(ErrorCode.FILE_BROKEN);
                    }
                }
            }
        }
        //pos后的是空
        return Arrays.copyOfRange(content, 0, pos);
    }

    /**
     * 把bytes数组内容转换成多个块对象，插入序号为blkIndex的逻辑块列表之前
     */
    private void insertBytesToBlk(byte[] b, int blkIndex) {
        byte[][] blocks = splitToBlocks(b);
        ArrayList<Block[]> insertBlocks = new ArrayList<>();//插入的块列表
        for (int j = 0; j < blocks.length; j++) {
            byte[] block = blocks[j]; //一个逻辑块的字符列表
            Block[] oneLogicalBlock = new Block[FileSystem.duplicatedBlockNum];//要转换成的一个逻辑块的块列表

            int createdBackUp = 0;//记录已经创建的备份块数量
            int i = 0;//第几次尝试创建备份块
            while (createdBackUp < FileSystem.duplicatedBlockNum) {
                //为每一个块分配BM并且创建block对象
                BlockManager bm = allocateBMbyHash(i, blkIndex + j);
                try {
                    oneLogicalBlock[createdBackUp] = bm.newBlock(block);
                    createdBackUp++;
                } catch (ErrorCode errorCode) {//CREATE_DUPLICATED_BLOCK，SYSTEM_IO_FAIL
                    //do nothing: 假定不会bm全都当掉
                } finally {
                    i++;
                }
            }
            insertBlocks.add(oneLogicalBlock);
        }
        logicalBlocks.addAll(blkIndex, insertBlocks);
    }

    /**
     * 用hash函数给blk分配bm，用文件id和块在逻辑块列表中的序号和备份块在逻辑块中的序号来作为参数，有助于查找
     */
    private BlockManager allocateBMbyHash(int indexInDup, int indexInLogicBlock) {
        String para = "" + fileId.fildId + indexInLogicBlock + indexInDup;
        int bmId = para.hashCode() % FileSystem.blockManagerMap.size();
        return FileSystem.blockManagerMap.get(bmId);
    }

    /**
     * 文件的逻辑块列表中移除参数闭区间内的块。
     * bm的Blockmap和物理上blk.meta保留（因为有可能有copy的文件仍然使用）
     */
    private void deleteBlksFromList(int fromBlock, int toBlock) {
        for (int i = fromBlock; i <= toBlock; i++) {
            logicalBlocks.remove(fromBlock);
        }
    }

    /**
     * 把每个byte数组按块分割成二维数组
     */
    private byte[][] splitToBlocks(byte[] b) {
        int blkNum = (int) Math.ceil((double) b.length / FileSystem.blockSize);
        byte[][] blocks = new byte[blkNum][];
        for (int i = 0; i < blkNum - 1; i++) {//到倒数第二块
            blocks[i] = Arrays.copyOfRange(b, i * FileSystem.blockSize, (i + 1) * FileSystem.blockSize);
        }
        blocks[blkNum - 1] = Arrays.copyOfRange(b, (blkNum - 1) * FileSystem.blockSize, b.length);//最后一块
        return blocks;
    }

    /**
     * 从file.meta的逻辑块字符串中找到相应的块加入myfile对象的逻辑块列表
     */
    private void loadLogicalBlocksFromString(String content) {
        if (content.length() > 1) { //有逻辑块信息
            String[] logicBlocks = content.split(";");
            for (String block : logicBlocks) {//元素是一个逻辑块，格式类似 BM1.1 BM2.1
                String[] backupBlks = block.split(" ");
                Block[] backup = new Block[backupBlks.length];
                for (int i = 0; i < backupBlks.length; i++) {
                    try {
                        backup[i] = Tools.findBlockFromName(backupBlks[i]);
                    } catch (ErrorCode e) {//BLOCKMANAGER_NOT_EXIST ID_INVALID BLOCK_META_BROEKN
                        backup[i] = null;//异常的块，用null代替，之后读到它会选择其他备份块
                    }
                }
                logicalBlocks.add(backup);
            }
        }
    }

    /**
     * 物理上创建file.meta并且写入内容/重写覆盖file.meta内容（重复检测在创建file对象分配id时做过）
     *
     * @throws ErrorCode SYSTEM_IO_FAIL
     */
    private void writeFileMeta() throws ErrorCode {
        String content = formatFileMeta();
        String path = FileSystem.fmPath + "/fm-" + fileManager.fmId + "/" + fileId.fildId + ".meta";
        java.io.File fileMeta = new java.io.File(path);
        try {
            if (fileMeta.exists())//已经存在则重写
                fileMeta.delete();
            fileMeta.createNewFile();
            FileWriter fw = new FileWriter(fileMeta);
            fw.write(content);
            fw.flush();
            fw.close();
        } catch (IOException e) {//io异常
            throw new ErrorCode(ErrorCode.IO_EXCEPTION);
        }
    }

    /**
     * 从对象字段得到可以写入file.meta文件的格式化的字符串
     */
    private String formatFileMeta() {
        String content = "fileId:" + fileId.fildId + "\n" + "size:" + size + "\n" + "logicalBlocks:";
        for (Block[] logicalBlock : logicalBlocks) {
            for (Block physicalBlock : logicalBlock) {
                if (physicalBlock == null) {//logicalBlock中可能有null（当物理上有异常时）,直接跳过这个块
                    continue;
                }
                content += "BM" + physicalBlock.getBlockManager().bmId + "." + physicalBlock.getIndex() + " ";
            }
            content += "\n";
        }
        return content;
    }

    int getFileId() {
        return fileId.fildId;
    }

    FileManager getFileManager() {
        return fileManager;
    }

    int getSize() {
        return size;
    }

    int pos() {
        return move(0, MOVE_CURR);
    }

    /**
     * 光标类，保存了块层面光标和块之间的位置关系
     * 和文件层面光标指向的字节数
     */
    class Cursor {
        int bytePos;//光标在第几个字节后
        int blkIndex;//光标所在的块序号，从0开始
        int offset;//光标距离它所在的块开始处的偏移值
        int prevSumBytes;//在它所在的块之前的所有字节
        boolean atBlockBoundary;//光标是否在块边界

        /**
         * 在file对象被初始化时初始化，光标在文件0字节处
         */
        Cursor() {
            bytePos = 0;
            blkIndex = -1;
            offset = 0;
            prevSumBytes = 0;
            atBlockBoundary = true;
        }

        /**
         * 在文件读写过程被创建，根据文件字节数得到一个光标对象
         */
        Cursor(int bytePos) {
            update(bytePos);
        }

        /**
         * 得到每个逻辑块中实际储存的文件字节数
         * 假定光标在0处属于第-1块（方便计算）
         * 第i个块包括右边界不包括左边界
         */
        private int[] getBlockSizeList() {
            int[] size = new int[logicalBlocks.size() + 1];//假定有第-1块
            size[0] = 0;
            for (int i = 0; i < logicalBlocks.size(); i++) {
                for (Block blk : logicalBlocks.get(i)) {
                    if (blk != null) {
                        size[i + 1] = blk.getSize();
                        break;
                    }
                }
            }
            return size;
        }

        /**
         * 根据bytePos更新光标和块之间的关系（读写会让sizelist变动）
         */
        public void update(int pos) {
            bytePos = pos;
            int[] sizeList = getBlockSizeList();
            int sum = 0;
            for (int i = 0; i < sizeList.length; i++) {
                sum += sizeList[i];
                if (sum >= bytePos) {
                    blkIndex = i - 1;
                    prevSumBytes = sum - sizeList[i];
                    offset = bytePos - prevSumBytes;
                    atBlockBoundary = (sum == bytePos);
                    break;
                }
            }
        }
    }
}
