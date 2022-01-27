import java.io.File;
import java.util.HashMap;

public class FileSystem {
    //一些参数
    public static final String fmPath = "./FileSystem/fm";
    public static final String bmPath = "./FileSystem/bm";
    public static final int duplicatedBlockNum = 3;
    public static final int blockSize = 2;
    public static final int maxFileNum = 1000;//一个bm下支持的最多块数,和分配fileId相关
    public static final int defaultBmNum = 6;//默认生成6个bm

    public static HashMap<Integer,FileManager> fileManagerMap = new HashMap<>();
    public static HashMap<Integer,BlockManager> blockManagerMap = new HashMap<>();

    /**
    初始化：
    * 在path下读取fm和bm，并且实例化对象
     * 先初始化bm保证所有有blk.meta的块对象被创建
     * 再初始化fm，保证file.meta中记录的块都能被加入逻辑块列表
     * 不同在于bm默认会创建6个，而fm默认用户创建
     */
    private FileSystem(){
        initializeBm();
        initializeFm();
    }


    //单例模式
    private static FileSystem fileSystem = new FileSystem();
    public static FileSystem getInstance(){
        return fileSystem;
    }

    /**
     * 在关闭文件系统之前保证所有文件同步
     */
    public void close(){
        FileID.idMap.forEach(((fileId, myFile) -> myFile.flush()));
    }

    private void initializeBm(){
        File bmDir = new File(bmPath);
        if (!bmDir.exists()){//不存在物理文件夹就按默认数量初始化bm列表
            bmDir.mkdirs();
            for (int i =0;i<defaultBmNum;i++){
                BlockManager bm = new BlockManager();
                blockManagerMap.put(bm.bmId,bm);
            }
        }else{//有备份就恢复bm对象
            File[] bms = bmDir.listFiles(pathname -> (pathname.getName().contains("bm-"))?true:false);
            for(File bm:bms){
                BlockManager blockManager = new BlockManager(bm);
                blockManagerMap.put(blockManager.bmId,blockManager);
            }
        }
    }

    private void initializeFm(){
        File fmDir = new File(fmPath);
        if (!fmDir.exists()){
            fmDir.mkdirs();
        }else{
            File[] fms = fmDir.listFiles(pathname -> (pathname.getName().contains("fm-"))?true:false);
            for(File fm:fms){
                FileManager fileManager = new FileManager(fm);
                fileManagerMap.put(fileManager.fmId,fileManager);
            }
        }
    }
}
