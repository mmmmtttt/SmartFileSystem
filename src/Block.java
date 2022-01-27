import java.io.*;
import java.nio.charset.StandardCharsets;

/*
封装了块的元信息，块不可重入
写blk.meta只在构造方法中，写blk.data推迟到buffer同步时
读从buffer中读
 */
public class Block {
    private int blockId;//顺序生成
    private int size;//block中有意义的文件的字节，块不可重入，一旦固定就不能修改大小
    private String dataPath;
    private BlockManager blockManager;
    private long checkSum;
    Buffer buffer = new Buffer();

    /**
    系统启动时从blkid.meta文件中恢复blk的id,dataPath,checkSum
    * 格式举例：
     * blockId:2
     * size:20
     * physicalPath:./xxx.data
     * checkSum:xxxx
     * @throws ErrorCode SYSTEM_IO_FAIL
     */
    Block(File blkMeta,BlockManager blockManager){
        this.blockManager = blockManager;
        //从blk.meta中读内容
        String content = ";";
        try(BufferedReader br = new BufferedReader(new FileReader(blkMeta))){
            String line;
            while((line = br.readLine())!=null){
                content+=line+";";
            }
            content=content.substring(0,content.length()-1);//去掉最后一个；
            //分别读取信息
            String[] contents = content.split(";\\w+?:");
            blockId = Integer.parseInt(contents[1]);
            size = Integer.parseInt(contents[2]);
            dataPath = contents[3];
            checkSum = Long.parseLong(contents[4]);
        } catch (IOException e) {
            System.out.println(ErrorCode.getErrorText(ErrorCode.IO_EXCEPTION));
        }
    }

    /**
     * 创建一个有内容的block新对象，物理上创造blk.meta
     * blk.data的创建会延迟到buffer同步
     * @throws ErrorCode CREATE_DUPLICATED_BLOCK，SYSTEM_IO_FAIL
     */
    Block(BlockManager blockManager,int blkId,byte[] b) throws ErrorCode{
        dataPath = FileSystem.bmPath+"/bm-"+blockManager.bmId+"/"+blkId+".data";
        //将内容写入buffer中
        buffer.content = b;
        buffer.status = Buffer.DELAY_WRITE;
        //修改block对象字段
        blockId = blkId;
        this.blockManager = blockManager;
        checkSum = Tools.getHash(b);
        size = b.length;
        //将内容写入blkid.meta中
        String metaPath = FileSystem.bmPath+"/bm-"+blockManager.bmId+"/"+blkId+".meta";
        writeToBlock(metaPath,formatBlockMeta().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 提供给上层的接口，从buffer读取内容
     * @throws ErrorCode DATA_BLOCK_NOT_FOUND SYSTEM_IO_FAIL DATA_BLOCK_BROKEN
     */
    byte[] read() throws ErrorCode {
        if (buffer.status!=Buffer.INVALID){//可读
            return buffer.content;
        }else{//不可读，先从磁盘读取进buffer,再返回buffer
            buffer.content = readPhysicalBlock();
            buffer.status = Buffer.VALID;
            return buffer.content;
        }
    }

    /**
     * 在file.close中被调用，强制同步buffer
     * 对于本次操作中新创建的block，需要重新创建block.data写入
     */
    void bufferSync(){
        if(buffer.status==Buffer.DELAY_WRITE){//需要同步
            writeToBlock(dataPath, buffer.content);
            buffer.status=Buffer.VALID;
        }
    }


    int getSize() {
        return size;
    }

    int getIndex() {
        return blockId;
    }

    BlockManager getBlockManager() {
        return blockManager;
    }

    /**
     从物理地址dataPath读取block data
     @throws ErrorCode DATA_BLOCK_NOT_FOUND SYSTEM_IO_FAIL DATA_BLOCK_BROKEN
     */
    private byte[] readPhysicalBlock() throws ErrorCode {
        byte[] content = new byte[size];
        try(FileInputStream in =new FileInputStream(dataPath)){
            int readCount = 0;
            while(readCount<size){
                readCount += in.read(content,readCount,size-readCount);
            }
        } catch (FileNotFoundException e) {//物理块损坏
            throw new ErrorCode(ErrorCode.DATA_BLOCK_NOT_FOUND);
        } catch (IOException e) {
            throw new ErrorCode(ErrorCode.IO_EXCEPTION);
        }
        //检查校验和
        if (Tools.getHash(content)!=checkSum){
            System.out.println("(提示)校验和失败");
            throw new ErrorCode(ErrorCode.DATA_BLOCK_BROKEN);
        }
        return content;
    }

    /**
     * 物理上写入文件
     * @throws ErrorCode CREATE_DUPLICATED_BLOCK，SYSTEM_IO_FAIL
     */
    private void writeToBlock(String dataPath,byte[] b) throws ErrorCode{
        File dataBlk = new File(dataPath);
        if (!dataBlk.exists()){
            try (FileOutputStream fout = new FileOutputStream(dataBlk)){
                dataBlk.createNewFile();
                fout.write(b);
                fout.flush();
            } catch (IOException e){
                throw new ErrorCode(ErrorCode.IO_EXCEPTION);
            }
        }else{
            throw new ErrorCode(ErrorCode.CREATE_DUPLICATED_BLOCK);
        }
    }

    /**
     * 格式化写入block.meta的字符串
     */
    private String formatBlockMeta(){
        String content = "blockId:"+blockId+"\n"+"size:"+size+"\n"+"physicalPath:"+dataPath+"\n"+"checkSum:"+checkSum;
        return content;
    }
}

