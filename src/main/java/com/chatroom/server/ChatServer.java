package com.chatroom.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程聊天室服务器
 * 使用线程池管理客户端连接，支持并发处理多个客户端
 */
public class ChatServer {
    private static final int PORT = 8888;
    private static final int MAX_THREADS = 50;
    private static final int CORE_THREADS = 10;
    
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final ExecutorService threadPool;
    private final AtomicInteger clientIdCounter;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer() {
        this.clients = new ConcurrentHashMap<>();
        this.threadPool = new ThreadPoolExecutor(
            CORE_THREADS,
            MAX_THREADS,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "ChatServer-Worker-" + threadNumber.getAndIncrement());
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.clientIdCounter = new AtomicInteger(1);
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("聊天室服务器启动成功，端口: " + PORT);
            System.out.println("线程池配置 - 核心线程数: " + CORE_THREADS + ", 最大线程数: " + MAX_THREADS);
            System.out.println("等待客户端连接...\n");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = "Client-" + clientIdCounter.getAndIncrement();
                    System.out.println("新客户端连接: " + clientId + " [" + clientSocket.getInetAddress() + "]");
                    
                    ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
                    clients.put(clientId, handler);
                    threadPool.execute(handler);
                    
                    System.out.println("当前在线用户数: " + clients.size() + 
                                     ", 活动线程数: " + ((ThreadPoolExecutor)threadPool).getActiveCount());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("接受客户端连接时发生错误: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void broadcast(String message, String senderId) {
        String broadcastMsg = "[广播] " + message;
        clients.forEach((clientId, handler) -> {
            if (!clientId.equals(senderId)) {
                handler.sendMessage(broadcastMsg);
            }
        });
    }

    public void removeClient(String clientId) {
        ClientHandler handler = clients.remove(clientId);
        if (handler != null) {
            System.out.println("客户端断开: " + clientId);
            System.out.println("当前在线用户数: " + clients.size());
            broadcast(clientId + " 离开了聊天室", clientId);
        }
    }

    public void shutdown() {
        if (!running) {
            return;
        }
        
        running = false;
        System.out.println("\n正在关闭服务器...");
        
        // 关闭所有客户端连接
        clients.forEach((id, handler) -> handler.close());
        clients.clear();
        
        // 关闭线程池
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        
        // 关闭服务器套接字
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭服务器套接字时发生错误: " + e.getMessage());
        }
        
        System.out.println("服务器已关闭");
    }

    public static void main(String[] args) {
        final ChatServer server = new ChatServer();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
        
        server.start();
    }
}
