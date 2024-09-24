package org.example.util;

import org.example.MainProcess;
import org.example.beans.DirectoryData;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 選擇目錄
 *
 * @author Eric.Lee
 * Date: 2024/9/23
 */
public class DirectoryChooserTool {

    private final JFrame frame;  // 使用 JFrame
    private File selectedDirectory;
    private volatile boolean stopRequested = false; // 标记是否请求停止

    public DirectoryChooserTool() {
        // 创建主窗口
        frame = new JFrame("选择目录");
        frame.setSize(400, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // 创建面板来放置组件
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        // 文本字段用于显示选择的目录路径
        JTextField directoryField = new JTextField(20);
        directoryField.setEditable(false); // 让文本字段不可编辑

        // "选择目录"按钮
        JButton chooseButton = new JButton("选择目录");
        chooseButton.addActionListener(e -> {
            // 打开目录选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = fileChooser.getSelectedFile();
                directoryField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // "确认"按钮
        JButton confirmButton = new JButton("确认");
        confirmButton.addActionListener(e -> {
            if (selectedDirectory != null) {
                // 启动处理逻辑
                startProcessing();
            } else {
                JOptionPane.showMessageDialog(frame, "请选择一个目录！");
            }
        });

        // "停止"按钮
        JButton stopButton = new JButton("停止");
        stopButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "确定要停止服务吗？", "确认停止", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                stopRequested = true;
                System.out.println("服务已请求停止。");
            }
        });

        // 添加组件到面板
        panel.add(directoryField);
        panel.add(chooseButton);
        panel.add(confirmButton);

        // 将面板添加到窗口
        frame.add(panel, BorderLayout.CENTER);

        // 创建底部面板，放置 "停止" 按钮
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT)); // 右对齐
        bottomPanel.add(stopButton);

        // 将底部面板添加到窗口
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // 设置窗口位置在屏幕右下角
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - frame.getWidth() - 200;
        int y = screenSize.height - frame.getHeight() - 200;
        frame.setLocation(x, y);

        // 显示窗口
        frame.setVisible(true);
    }

    // 启动处理逻辑
    private void startProcessing() {
        // 禁用按钮，避免重复点击
        frame.setTitle("服务运行中...");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // 禁用关闭按钮


        DirectoryData directoryData = processDirectory(selectedDirectory);
        if (directoryData.rootPath().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "目录处理失败。");
            return;
        }
        // 调用主程序逻辑，传入 directoryData
        MainProcess mainProcess = new MainProcess(directoryData, this);
        mainProcess.start();
    }


    // 获取停止请求状态
    public boolean isStopRequested() {
        return stopRequested;
    }

    // 提供公共方法更新窗口标题
    public void updateTitle(String title) {
        SwingUtilities.invokeLater(() -> frame.setTitle(title));
    }

    // 提供公共方法显示完成消息
    public void showCompletionMessage(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, message));
    }

    // 提供公共方法关闭窗口
    public void closeWindow() {
        SwingUtilities.invokeLater(() -> frame.dispose());
    }


    // 处理目录，返回 DirectoryData
    private DirectoryData processDirectory(File directory) {
        // 获取根目录路径
        String rootPath = directory.getAbsolutePath();

        // 遍历目录，找到所有纯数字名称的文件夹
        File[] subDirs = directory.listFiles(File::isDirectory);
        List<String> numericFolders = new ArrayList<>();

        if (subDirs != null) {
            for (File subDir : subDirs) {
                // 检查文件夹名称是否为纯数字
                if (subDir.getName().matches("\\d+")) {
                    numericFolders.add(subDir.getName());
                }
            }

            // 按照数字顺序排序
            numericFolders.sort(Comparator.comparingInt(Integer::parseInt));
        }

        // 将数据打包为 DirectoryData 对象并返回
        return new DirectoryData(rootPath, numericFolders);
    }
}

