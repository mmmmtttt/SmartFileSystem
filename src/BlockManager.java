import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/*
分组地管理block对象
 */
public class BlockManager {
    int bmId;
    int blkNum;//分配过的总块数，用来给新块分配id
    HashMap<Integer,Block> blockMap = new HashMap<>(); //(blkId，Block)

    /**
     * 系统第一次初始化时创建bm
     */
    public BlockManager(){
        bmId = FileSystem.blockManagerMap.size();
        blkNum = 0;
        createDirectory();
    }

    /**
    系统启动时从bm-bmId文件夹中恢复bm
     @throws ErrorCode SYSTEM_IO_FAIL
     */
    public BlockManager(File bmDir){
        String path = bmDir.getName();
        bmId = Integer.parseInt(path.substring(path.lastIndexOf('-')+1));
        File[] blkMetas = bmDir.listFiles(pathname -> (pathname.getName().contains(".meta"))?true:false);
        blkNum = blkMetas.length;
        for(File blkMeta:blkMetas){//用fileMeta里的id,size,block 恢复成MyFile对象
            Block blk = new Block(blkMeta,this);
            blockMap.put(blk.getIndex(),blk);
        }
    }

    /**
     * 根据索引得到block对象
     * @throws ErrorCode ID_INVALID BLOCK_META_BROEKN
     */
    Block getBlock(int index) throws ErrorCode{
        if(index <0){
            throw new ErrorCode(ErrorCode.ID_INVALID);
        }
        if(blockMap.get(index)==null){//找不到物理块，向上抛出找备份块
            throw new ErrorCode(ErrorCode.BLOCK_META_NOT_FOUND);
        }
        return blockMap.get(index);
    }

    /**
     * 在当前bm下创建一个新的有内容的blk对象
     * 内容写入buffer，创建blockid.meta
     * @throws ErrorCode CREATE_DUPLICATED_BLOCK，SYSTEM_IO_FAIL
     */
    Block newBlock(byte[] b) throws ErrorCode{
        int blkId = blkNum;
        Block block = new Block(this, blkId, b);
        blockMap.put(blkId,block);
        blkNum++;
        return block;
    }

    /**
     * 在当前bm下创建一个新的空blk对象
     * 只创建blockid.meta
     */
    Block newEmptyBlock(int blockSize) {
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);
        blkNum++;
        return newBlock(bytes);
    }

    /**
     * 创建bm/bmid文件夹
     */
    private void createDirectory(){
        String path = FileSystem.bmPath+"/bm-"+bmId;
        File bmDir = new File(path);
        if (!bmDir.exists()){
            bmDir.mkdir();
        }
    }
}
