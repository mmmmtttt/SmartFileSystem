/*
 * 封装一些保证fileid全局唯一的操作
 * 和从fileid定位myfile对象的操作
 */
import java.util.HashMap;
public class FileID {
    int fildId;
    public static HashMap<Integer, MyFile> idMap = new HashMap<>();

    /**
     * 给用户指定fileid的Myfile对象创建字段fileid对象
     * @throws ErrorCode FILE_DUPLICATED
     */
    FileID(int fileId,MyFile file) throws ErrorCode{
        this.fildId =fileId;
        //保证能创造的file对象不会重复id
        if (idMap.get(fileId)!=null){
            throw new ErrorCode(ErrorCode.CREATE_DUPLICATED_FILE);
        }
        idMap.put(fileId,file);
    }

    /**
     * 给不指定fileid的Myfile创建字段fileid对象（文件层的copy操作）
     */
    FileID(MyFile file){
        fildId = allocateFileId();
        idMap.put(fildId,file);
    }

    /**
     * 从全局id索引已存在的MyFile对象（用于smart操作）
     * @throws ErrorCode FILE_NOT_EXISTED
     */
    public static MyFile getFileById(int id) throws ErrorCode{
        MyFile file = idMap.get(id);
        if (file == null) {
            throw new ErrorCode(ErrorCode.FILE_NOT_EXIST);
        } else {
            return file;
        }
    }

    /**
     * 分配一个不重复的fileid
     */
    public static int allocateFileId(){
        int id;
        while(true){
            id = (int)(Math.random()*FileSystem.maxFileNum);
            if (idMap.get(id)==null){
                return id;
            }
        }
    }
}
