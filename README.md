# chatRoom
支持线程池和锁的网络聊天室

## 功能特性

- ✅ 多线程并发处理：使用线程池管理客户端连接
- ✅ 线程安全：使用 ConcurrentHashMap 和同步机制保证线程安全
- ✅ 可配置的线程池：核心线程数 10，最大线程数 50
- ✅ 消息广播：实时向所有在线用户广播消息
- ✅ 用户管理：支持用户加入、离开通知
- ✅ 优雅关闭：支持资源清理和线程池关闭

## 技术实现

### 服务器端 (ChatServer)
- 使用 `ThreadPoolExecutor` 管理客户端连接线程
- 使用 `ConcurrentHashMap` 存储客户端连接，保证线程安全
- 使用 `AtomicInteger` 生成客户端 ID，保证原子性
- 每个客户端连接由独立的 `ClientHandler` 线程处理

### 客户端 (ChatClient)
- 使用两个线程：主线程处理用户输入，后台线程接收服务器消息
- 支持实时双向通信

## 编译和运行

### 前置要求
- JDK 1.8 或更高版本
- Maven 3.x

### 编译项目
```bash
mvn clean compile
```

### 启动服务器
```bash
mvn exec:java -Dexec.mainClass="com.chatroom.server.ChatServer"
```

### 启动客户端
在新的终端窗口中运行：
```bash
mvn exec:java -Dexec.mainClass="com.chatroom.client.ChatClient"
```

可以启动多个客户端进行测试。

## 使用说明

1. 启动服务器后，它会监听 8888 端口
2. 启动客户端后，会自动连接到服务器
3. 按提示输入用户名
4. 输入消息并回车发送，消息会广播给所有其他在线用户
5. 输入 `/quit` 或 `/exit` 退出聊天室
6. 输入 `/users` 查看当前在线用户数

## 线程池配置

服务器线程池配置：
- 核心线程数：10
- 最大线程数：50
- 空闲线程存活时间：60 秒
- 任务队列容量：100
- 拒绝策略：CallerRunsPolicy（调用者运行策略）

## 项目结构
```
chatRoom/
├── src/main/java/com/chatroom/
│   ├── server/
│   │   ├── ChatServer.java      # 服务器主类
│   │   └── ClientHandler.java   # 客户端处理器
│   └── client/
│       └── ChatClient.java       # 客户端主类
├── pom.xml                       # Maven 配置文件
└── README.md                     # 项目说明
```
