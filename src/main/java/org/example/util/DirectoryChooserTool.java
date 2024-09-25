package org.example.util;

import org.example.beans.DirectoryData;
import org.example.process.MainProcess;

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

    private final JFrame frame;
    // 選擇目錄
    private File selectedDirectory;
    // 標記是否請求停止運行
    private volatile boolean stopRequested = false;

    public DirectoryChooserTool() {
        // 建立主視窗
        frame = new JFrame("選擇目錄");
        frame.setSize(400, 150);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // 建立面板來放置組件
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        // 文字欄位用於顯示選取的目錄路徑
        JTextField directoryField = new JTextField(20);
        directoryField.setEditable(false);

        // "選擇目錄"按钮
        JButton chooseButton = new JButton("選擇目錄");
        chooseButton.addActionListener(e -> {
            // 開啟目錄選擇器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = fileChooser.getSelectedFile();
                directoryField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // "確認"按鈕
        JButton confirmButton = new JButton("執行");
        confirmButton.addActionListener(e -> {
            if (selectedDirectory != null) {
                // 啟動處理邏輯
                startProcessing();
            } else {
                JOptionPane.showMessageDialog(frame, "請選擇目錄！");
            }
        });

        // "停止"按钮
        JButton stopButton = new JButton("停止");
        stopButton.addActionListener(e -> {
            stopRequested = true;
            System.out.println("服務已請求停止。");
        });

        // 新增組件到面板
        panel.add(directoryField);
        panel.add(chooseButton);
        panel.add(confirmButton);

        // 將面板新增至視窗
        frame.add(panel, BorderLayout.CENTER);

        // 建立底部面板，放置 "停止" 按鈕
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT)); // 右對齊
        bottomPanel.add(stopButton);

        // 將底部面板新增至視窗
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // 設定視窗位置在螢幕右下角
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - frame.getWidth() - 200;
        int y = screenSize.height - frame.getHeight() - 200;
        frame.setLocation(x, y);

        // 顯示視窗
        frame.setVisible(true);
    }

    // 啟動處理邏輯
    private void startProcessing() {
        // 停用按鈕，避免重複點擊
        frame.setTitle("服務運作中...");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        DirectoryData directoryData = processDirectory(selectedDirectory);
        if (directoryData.rootPath().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "目錄處理失敗。");
            return;
        }
        // 呼叫主程式邏輯，傳入 directoryData
        MainProcess mainProcess = new MainProcess(directoryData, this);
        mainProcess.start();
    }


    // 取得停止請求狀態
    public boolean isStopRequested() {
        return stopRequested;
    }

    // 關閉視窗
    public void closeWindow() {
        SwingUtilities.invokeLater(frame::dispose);
    }

    // 處理選擇目錄，返回 DirectoryData
    private DirectoryData processDirectory(File directory) {
        // 取得根目錄路徑
        String rootPath = directory.getAbsolutePath();

        // 遍歷目錄，找到所有純數字名稱的資料夾
        File[] subDirs = directory.listFiles(File::isDirectory);
        List<String> numericFolders = new ArrayList<>();

        if (subDirs != null) {
            for (File subDir : subDirs) {
                // 检查文件夹名称是否为纯数字
                if (subDir.getName().matches("\\d+")) {
                    numericFolders.add(subDir.getName());
                }
            }

            // 依照數字順序排序
            numericFolders.sort(Comparator.comparingInt(Integer::parseInt));
        }
        // 將資料打包為 DirectoryData 物件並傳回
        return new DirectoryData(rootPath, numericFolders);
    }
}

