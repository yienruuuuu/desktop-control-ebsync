package org.example;

import org.example.beans.DirectoryData;
import org.example.util.DirectoryChooserTool;
import org.example.util.SikuliAutomationTool;

import java.io.File;
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
        // 获取数字文件夹列表
        List<String> numericFolders = directoryData.numericFolders();

        // 获取根目录路径
        String rootPath = directoryData.rootPath();

        // 创建 SikuliAutomationTool 实例
        SikuliAutomationTool automationTool = new SikuliAutomationTool();

        // 处理每个数字文件夹
        for (String folderName : numericFolders) {
            if (chooserTool.isStopRequested()) {
                System.out.println("程序已停止。");
                break;
            }

            String numericFolderPath = rootPath + File.separator + folderName;

            // 检查 keys 和 video 文件夹是否存在
            String keysPath = numericFolderPath + File.separator + "keys";
            String videoPath = numericFolderPath + File.separator + "video";
            if (!new File(keysPath).exists() || !new File(videoPath).exists()) {
                System.out.println("keys 或 video 文件夹不存在于目录: " + folderName);
                continue;
            }

            // 使用 SikuliAutomationTool 进行自动化操作
            automationTool.processFolder(numericFolderPath);

            // 可以在这里添加对停止请求的检查
            if (chooserTool.isStopRequested()) {
                System.out.println("程序已停止。");
                break;
            }
        }

        // 处理完成，更新窗口状态
        chooserTool.updateTitle("处理完成");
        chooserTool.showCompletionMessage("处理已完成。");
        chooserTool.closeWindow(); // 关闭窗口
    }
}
