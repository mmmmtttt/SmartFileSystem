import java.io.File;
import java.util.HashMap;

public class FileManager {
    int fmId;
    HashMap<Integer, MyFile> fileMap; //(fileid,File)

    /**
    用户在系统启动后实例化fm
     */
    public FileManager() {
        fmId = FileSystem.fileManagerMap.size();
        fileMap = new HashMap<>();
        FileSystem.fileManagerMap.put(fmId, this);
        createDirectory();
    }

    /**
    系统启动时从fm-fmId文件夹中恢复fm
     */
    public FileManager(File fmDir) {
        fileMap = new HashMap<>();
        String path = fmDir.getName();
        fmId = Integer.parseInt(path.substring(path.lastIndexOf('-') + 1));
        File[] fileMetas = fmDir.listFiles(pathname -> (pathname.getName().contains(".meta")) ? true : false);
        for (File fileMeta : fileMetas) {//用fileMeta里的id,size,block 恢复成MyFile对象
            MyFile myFile = new MyFile(fileMeta, this);
            fileMap.put(myFile.fileId.fildId, myFile);
        }
    }

    /**
     * 根据fileid找到当前fm下的myfile对象
     * @throws ErrorCode ID_INVALID FILE_NOT_EXISTED
     */
    public MyFile getFile(int fileId) {
        if (fileId < 0) {
            return getRandomFile(ErrorCode.ID_INVALID);
        }
        if (fileMap.get(fileId) == null) {
            System.out.println(ErrorCode.getErrorText(ErrorCode.FILE_NOT_EXIST));
        }
        return fileMap.get(fileId);
    }

    /**
     * 在当前fm下创建一个新的myfile文件，指定id
     * @throws ErrorCode ID_INVALID FILE_NOT_EXISTED
     */
    public MyFile newFile(int fileId) {
        if (fileId < 0) {
            return getRandomFile(ErrorCode.ID_INVALID);
        }
        try {
            MyFile file = new MyFile(this, fileId);
            fileMap.put(fileId, file);
            return file;
        } catch (ErrorCode e) {
            if (e.getErrorCode() == ErrorCode.CREATE_DUPLICATED_FILE) {//MyFile构造函数可能抛出id重复异常,返回已有的file
                System.out.println(e.getErrorText(e.getErrorCode()) + ",return existing file");
                return FileID.idMap.get(fileId);
            } else
                return getRandomFile(e.getErrorCode());
        }
    }

    private void createDirectory() {
        String path = FileSystem.fmPath + "/fm-" + fmId;
        File fmDir = new File(path);
        if (!fmDir.exists()) {
            fmDir.mkdir();
        }
    }

    private MyFile getRandomFile(int errorCode) {
        MyFile myFile = new MyFile(this, FileID.allocateFileId());
        System.out.println(ErrorCode.getErrorText(errorCode) + ",return new file with id:" + myFile.fileId);
        return myFile;
    }
}
