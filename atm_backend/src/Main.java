/**
 * DateTime: 2025/5/26 19:20
 * Package: src
 * Author: Fulling
 */

import config.ConfigManager;
import server.Server;

public class Main {
    public static void main(String[] args) {
        ConfigManager configManager = ConfigManager.getInstance();
        
        // 从配置文件获取端口
        int mainPort = configManager.getIntProperty("server.main.port", 8888);

        // 启动主服务器
        Server server = new Server(mainPort);
        new Thread(server::start).start();
        
        System.out.println("银行系统服务器已启动...");
    }
}