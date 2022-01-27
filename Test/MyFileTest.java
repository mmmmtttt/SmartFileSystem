import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

class MyFileTest {
    @org.junit.jupiter.api.Test
    public void testSingleStep(){
        FileSystem fileSystem = FileSystem.getInstance();

        FileManager fileManager0 = new FileManager();
//        MyFile file = fileManager0.newFile(1); // id为1的一个file
//        file.write("abcd".getBytes(StandardCharsets.UTF_8));
//        watchStatus(file);
//        file.flush();

//        MyFile file1 = FileID.getFileById(1);
//        file1.move(1,MyFile.MOVE_HEAD);
//        file1.write("ef".getBytes(StandardCharsets.UTF_8));
//        watchStatus(file1);
//        file1.flush();

//        MyFile file1 = FileID.getFileById(1);
//        Tools.smartLs();
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("block:");
//        String para = scanner.nextLine();
//        Tools.smartHex(para);

//        MyFile file1 = FileID.getFileById(1);
//        file1.setSize(4);
//        watchStatus(file1);
//        file1.setSize(8);
//        watchStatus(file1);
//        file1.flush();

//        MyFile file1 = FileID.getFileById(1);
//        Tools.smartLs();
//        watchStatus(file1);
//
        Tools.smartLs();
        MyFile file1 = fileManager0.newFile(1);
        MyFile file2 = fileManager0.newFile(2);
        watchStatus(file1);

        file2.write("ab".getBytes(StandardCharsets.UTF_8));
        file2.flush();
        MyFile file3 = fileManager0.getFile(3);
        System.out.println(111);


//        MyFile file2 = Tools.smartCopy(1);
//        watchStatus(file2);
//        Tools.smartWrite(0,MyFile.MOVE_HEAD,file2.getFileId());
//        watchStatus(file);
//        watchStatus(file2);

    }


    /**
     * 给定测试
     */
    @org.junit.jupiter.api.Test
    public void test(){
        //initialize file system
        FileSystem fileSystem = FileSystem.getInstance();

        FileManager fileManager0 = new FileManager();
        MyFile file = fileManager0.newFile(1); // id为1的一个file
        file.write("FileSystem".getBytes(StandardCharsets.UTF_8));
        print((file.read(file.getSize())));//光标在文件末尾，应该是空
        watchStatus(file);

        file.move(0, MyFile.MOVE_HEAD);
        file.write("Smart".getBytes(StandardCharsets.UTF_8));
        print(file.read(file.getSize())); //光标在smart之后，打印处f...m
        watchStatus(file);

        file.setSize(100);
        print(file.read(file.getSize()));//光标在m后，打印出很多0
        watchStatus(file);

        file.setSize(16);
        print((file.read(file.getSize())));//光标在16个字节后，打印出空
        watchStatus(file);

        file.flush();
        watchStatus(file);

        /*
        here we will destroy a block, and you should handler this exception
         */

        MyFile file1 = fileManager0.getFile(1);
        print(file1.read(file1.getSize()));//光标在尾部，和file一样，应该读出空
        watchStatus(file1);

        MyFile file2 = Tools.smartCopy(1);
        print(file2.read(file2.getSize()));//光标在头部，应该读出全部16个字节
        watchStatus(file2);

        Tools.smartHex(getBlockStr());

        Tools.smartWrite(0, MyFile.MOVE_HEAD, file2.getFileId());
        watchStatus(file2);

        file2.flush();
        watchStatus(file2);

        //确保所有文件同步到磁盘上
        fileSystem.close();
    }

    @org.junit.jupiter.api.Test
    public void recoverFs(){
        FileSystem fileSystem = FileSystem.getInstance();
        print(Tools.smartCat(220));
        print(Tools.smartCat(1));
    }

    @org.junit.jupiter.api.Test
    public void checkSumTest1(){
        FileSystem fileSystem = FileSystem.getInstance();
        FileManager fileManager0 = new FileManager();
        MyFile file = fileManager0.newFile(1); // id为1的一个file
        file.write("FileSystem".getBytes(StandardCharsets.UTF_8));
        watchStatus(file);
        file.flush();
        /*
        手动去修改一个block的内容
         */
        MyFile file2 = Tools.smartCopy(1);
        watchStatus(file2);//读的是buffer 不会出错，接下去运行test2
    }

    @org.junit.jupiter.api.Test
    public void checkSumTest2(){
        /*
        接着test1,重新启动系统
         */
        FileSystem fileSystem = FileSystem.getInstance();
        print(Tools.smartCat(376));//这里填入在test1看到的file2的ID
    }

    @org.junit.jupiter.api.Test
    public void deleteBlockTest1(){
        FileSystem fileSystem = FileSystem.getInstance();
        FileManager fileManager0 = new FileManager();
        MyFile file = fileManager0.newFile(1); // id为1的一个file
        file.write("FileSystem".getBytes(StandardCharsets.UTF_8));
        watchStatus(file);
        file.flush();
        MyFile file2 = Tools.smartCopy(1);
        watchStatus(file2);//读的是buffer 不会出错，接下去运行test2
        /*
        手动去删除block.data或者meta
         */
    }

    @org.junit.jupiter.api.Test
    public void delectBlockTest2(){
        /*
        接着test1,重新启动系统
         */
        FileSystem fileSystem = FileSystem.getInstance();
        print(Tools.smartCat(344));//这里填入在test1看到的file2的ID
    }

    @org.junit.jupiter.api.Test
    public void singleFileIOTest() {
        //blksize = 2
        FileSystem fileSystem = FileSystem.getInstance();//初始化
        Tools.smartLs();
        FileManager fileManager0 = new FileManager();
        MyFile file = fileManager0.newFile(1);
        Tools.smartLs();

        System.out.println(Arrays.toString(file.read(file.getSize())));
        System.out.println(Arrays.toString(file.read(3)));

        byte[] bytes = "cse is fun!".getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
        file.write(bytes);
        Tools.smartLs();
        System.out.println(Arrays.toString(Tools.smartCat(file.getFileId())));

        file.setSize(5);//cse i
        System.out.println(Arrays.toString(Tools.smartCat(file.getFileId())));
        Tools.smartLs();

        file.setSize(10);//cse i00000
        System.out.println(Arrays.toString(Tools.smartCat(file.getFileId())));
        Tools.smartLs();

        //光标应该在i后 cse isecond write00000
        file.write("second write".getBytes(StandardCharsets.UTF_8));
        System.out.println(Arrays.toString(Tools.smartCat(file.getFileId())));
        Tools.smartLs();

        file.move(3,MyFile.MOVE_HEAD);
        //光标应该在e处 isecond w
        System.out.println(Arrays.toString(file.read(10)));

        file.move(2,MyFile.MOVE_CURR);//在i后
        file.write("third write".getBytes(StandardCharsets.UTF_8));// cse isecond writhirdwritete00000
        System.out.println(Arrays.toString(Tools.smartCat(file.getFileId())));
        Tools.smartLs();

        fileSystem.close();
    }

    @org.junit.jupiter.api.Test
    public void MultipleFilesTest() {
        //blksize = 2
        FileSystem fileSystem = FileSystem.getInstance();//初始化
        Tools.smartLs();
        FileManager fm0 = new FileManager();

        MyFile f0 = fm0.newFile(1);
        Tools.smartWrite(0,MyFile.MOVE_HEAD,f0.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f0.getFileId())));

        System.out.println(Arrays.toString(f0.read(1)));
        f0.move(0,MyFile.MOVE_HEAD);
        System.out.println(Arrays.toString(f0.read(f0.getSize())));
        Tools.smartLs();

        FileManager fm1 = new FileManager();
        MyFile f1 = fm1.newFile(1);
        MyFile f2 = fm1.newFile(2);
        Tools.smartWrite(0,MyFile.MOVE_HEAD,f2.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f2.getFileId())));
        Tools.smartLs();

        MyFile f3 = Tools.smartCopy(f0.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f3.getFileId())));
        Tools.smartLs();

        Tools.smartWrite(0,MyFile.MOVE_HEAD,f0.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f0.getFileId())));
        Tools.smartLs();

        Tools.smartWrite(-2,MyFile.MOVE_TAIL,f3.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f3.getFileId())));
        System.out.println(Arrays.toString(f2.read(2)));

        System.out.println(Arrays.toString(Tools.smartCat(f0.getFileId())));
        f0.move(3,MyFile.MOVE_HEAD);
        System.out.println(Arrays.toString(f0.read(0)));
        System.out.println(Arrays.toString(f0.read(2)));
        Tools.smartWrite(-3,MyFile.MOVE_CURR,f0.getFileId());
        System.out.println(Arrays.toString(Tools.smartCat(f0.getFileId())));

        Tools.smartLs();
        Scanner scanner = new Scanner(System.in);
        String para = scanner.nextLine();
        Tools.smartHex(para);
    }

    void watchStatus(MyFile f){
        Tools.smartLs();
        print(Tools.smartCat(f.getFileId()));
    }

    String getBlockStr(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("指定块编号,如：BM1.1");
        String para = scanner.nextLine();
        return para;
    }

    void print(byte[] b){
        System.out.print("byte：");
        System.out.println(Arrays.toString(b));
        try {
            System.out.print("String：");
            System.out.println(new String(b,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
