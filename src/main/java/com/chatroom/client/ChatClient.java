package com.chatroom.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * 聊天室客户端
 * 使用两个线程：一个用于接收服务器消息，一个用于发送用户输入
 */
public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running;
    private Thread receiveThread;

    public ChatClient() {
        this.running = false;
    }

    public void start() {
        try {
            // 连接到服务器
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            running = true;

            System.out.println("已连接到聊天室服务器");
            System.out.println("----------------------------------------");

            // 启动接收消息的线程
            receiveThread = new Thread(new ReceiveHandler(), "ReceiveThread");
            receiveThread.start();

            // 主线程处理用户输入
            handleUserInput();

        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            System.err.println("请确保服务器正在运行在 " + SERVER_HOST + ":" + SERVER_PORT);
        } finally {
            close();
        }
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            try {
                if (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    if (message != null && !message.trim().isEmpty()) {
                        writer.println(message);
                        
                        if (message.equalsIgnoreCase("/quit") || message.equalsIgnoreCase("/exit")) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("发送消息时发生错误: " + e.getMessage());
                }
                break;
            }
        }
        scanner.close();
    }

    private class ReceiveHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while (running && (message = reader.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("接收消息时发生错误: " + e.getMessage());
                }
            } finally {
                running = false;
            }
        }
    }

    public void close() {
        running = false;
        
        try {
            if (receiveThread != null && receiveThread.isAlive()) {
                receiveThread.interrupt();
                receiveThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
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
            System.err.println("关闭连接时发生错误: " + e.getMessage());
        }
        
        System.out.println("已断开与服务器的连接");
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
        }));
        
        client.start();
    }
}
