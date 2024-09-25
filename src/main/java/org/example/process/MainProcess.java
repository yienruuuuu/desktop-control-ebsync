package org.example.process;

import org.example.beans.DirectoryData;
import org.example.util.DirectoryChooserTool;
import org.example.util.SikuliAutomationTool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric.Lee
 * Date: 2024/9/24
 */
public class MainProcess extends Thread {
    private final DirectoryData directoryData;
    private final DirectoryChooserTool chooserTool;

    public MainProcess(DirectoryData directoryData, DirectoryChooserTool chooserTool) {
        this.directoryData = directoryData;
        this.chooserTool = chooserTool;
    }


    @Override
    public void run() {
        // 每批處理的數量
        int countLimitEachBatch = 5;

        // 取得數字資料夾列表
        List<String> numericFolders = directoryData.numericFolders();

        while (!numericFolders.isEmpty()) {
            if (chooserTool.isStopRequested()) break;

            // 取得根目錄路徑
            String rootPath = directoryData.rootPath();
            // 用於存儲已處理過的資料夾
            List<String> processedFolders = new ArrayList<>();

            // 處理每個數位資料夾
            for (int i = 0; i < Math.min(countLimitEachBatch, numericFolders.size()); i++) {
                if (chooserTool.isStopRequested()) break;
                // 路徑組裝
                String folderName = numericFolders.get(i);
                String numericFolderPath = rootPath + File.separator + folderName;
                // 確認 keys 和 video 資料夾是否不存在
                if (checkIfKeyAndVideoDirNotExist(numericFolderPath, folderName)) return;
                // 使用 SikuliAutomationTool 進行自動化操作
                new SikuliAutomationTool(chooserTool).processFolder(numericFolderPath);
                // 加入已處理列表
                processedFolders.add(folderName);
            }

            // 將處理過的資料夾從 numericFolders 列表中移除
            numericFolders.removeAll(processedFolders);
            //檢查所有運行中的 EbSynth狀態，直到完成
            new SikuliAutomationTool(chooserTool).checkEbSynthWindows(countLimitEachBatch);
        }

        // 關閉工具視窗
        chooserTool.closeWindow();
    }

    /**
     * 確認 keys 和 video 資料夾是否存在
     */
    private static boolean checkIfKeyAndVideoDirNotExist(String numericFolderPath, String folderName) {
        String keysPath = numericFolderPath + File.separator + "keys";
        String videoPath = numericFolderPath + File.separator + "frames";
        if (!new File(keysPath).exists() || !new File(videoPath).exists()) {
            System.out.println("keys 或 video 資料夾不存在於目錄: " + folderName);
            return true;
        }
        return false;
    }

}
