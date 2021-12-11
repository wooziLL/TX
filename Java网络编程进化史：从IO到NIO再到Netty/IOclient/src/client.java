import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class client {
    public static void main(String[] args) throws IOException{
        //创建客户端的Socket对象
        Socket s=new Socket("192.168.0.178", 50000);//一个可用的地址（server端的）

        //获取输出流，写数据
        OutputStream os=s.getOutputStream();
        os.write("hello,物联网2019级WOOZI".getBytes());

        //释放资源
        s.close();
    }
}
