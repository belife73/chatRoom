package com.chatroom.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * 客户端处理器
 * 每个客户端连接由一个独立的线程处理
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String clientId;
    private final ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running;
    private String username;

    public ClientHandler(Socket socket, String clientId, ChatServer server) {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;
        this.running = true;
        this.username = clientId;
    }

    @Override
    public void run() {
        try {
            // 初始化输入输出流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // 发送欢迎消息
            sendMessage("欢迎来到聊天室！你的ID是: " + clientId);
            sendMessage("请输入你的用户名:");

            // 读取用户名
            String name = reader.readLine();
            if (name != null && !name.trim().isEmpty()) {
                username = name.trim();
                sendMessage("欢迎, " + username + "! 开始聊天吧...");
                server.broadcast(username + " 加入了聊天室", clientId);
            }

            // 主消息循环
            String message;
            while (running && (message = reader.readLine()) != null) {
                message = message.trim();
                if (!message.isEmpty()) {
                    if (message.equalsIgnoreCase("/quit") || message.equalsIgnoreCase("/exit")) {
                        sendMessage("再见!");
                        break;
                    } else if (message.equalsIgnoreCase("/users")) {
                        sendMessage("当前在线用户数: " + getOnlineUserCount());
                    } else {
                        // 广播消息给其他用户
                        System.out.println("[" + username + "]: " + message);
                        server.broadcast(username + ": " + message, clientId);
                    }
                }
            }
        } catch (SocketException e) {
            // 客户端断开连接
            System.out.println("客户端连接中断: " + username);
        } catch (IOException e) {
            System.err.println("处理客户端消息时发生错误 [" + username + "]: " + e.getMessage());
        } finally {
            close();
            server.removeClient(clientId);
        }
    }

    public void sendMessage(String message) {
        if (writer != null && running) {
            synchronized (writer) {
                writer.println(message);
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭客户端连接时发生错误: " + e.getMessage());
        }
    }

    private int getOnlineUserCount() {
        // 这是一个辅助方法，实际计数由服务器维护
        return 0; // 可以通过回调服务器获取
    }

    public String getUsername() {
        return username;
    }

    public String getClientId() {
        return clientId;
    }
}
