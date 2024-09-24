package org.example.util;

import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Eric.Lee
 * Date: 2024/9/24
 */
public class SikuliAutomationTool {
    public void processFolder(String numericFolderPath) {
        ApplicationAutomationTool applicationAutomationTool = new ApplicationAutomationTool();
        try {
            // 在处理前检查是否请求停止
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("自动化操作已被中止。");
                return;
            }

            Screen screen = new Screen();
            // 打开数字文件夹（例如 "C:\1"）
            Runtime.getRuntime().exec("explorer.exe " + numericFolderPath);
            Thread.sleep(2000); // 等待文件夹窗口打开
            // 获取文件夹名称
            String folderName = new File(numericFolderPath).getName();
            applicationAutomationTool.moveWindowToRightHalf(folderName);
            Thread.sleep(3000); // 等待文件夹移動
            // 打開 EbSynth
            applicationAutomationTool.openEbSynth();

            // 使用 SikuliX 识别 EbSynth 初始化界面
            Pattern ebsynthInitial = createPatternFromResource("/ebsynth-init.png");

            // 等待 EbSynth 初始化界面出现
            screen.wait(ebsynthInitial, 10);

            // 获取 EbSynth 窗口的区域
            Match ebsynthWindow = screen.find(ebsynthInitial);

            // 在该窗口区域内执行操作
            Region ebSynthRegion = ebsynthWindow;

            // 定位 keys 文件夹图标并拖拽到 EbSynth 的 Keyframes 字段
            dragAndDropFolder("keys", "/keyframes.png", ebSynthRegion);

            // 定位 video 文件夹图标并拖拽到 EbSynth 的 Video 字段
            dragAndDropFolder("video", "/video.png", ebSynthRegion);

            // 点击 "Run All" 按钮
            Pattern runAllButton = createPatternFromResource("/run_all_button.png");

            // 取得當前區域的 x, y, 寬度和高度
            int x = ebsynthWindow.getX();
            int y = ebsynthWindow.getY();
            int width = ebsynthWindow.getW();
            int height = ebsynthWindow.getH();
            Region ebSynthRegion2 = new Region(x, y, width, height + 700);

            ebSynthRegion2.highlight(2);
            ebSynthRegion2.click(runAllButton);

            applicationAutomationTool.closeeWindow(folderName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dragAndDropFolder(String folderName, String targetImagePath, Region ebSynthRegion) throws Exception {
        Screen screen = new Screen();

        // 定位要拖拽的文件夹图标（在数字文件夹窗口中）
        Pattern folderIcon = createPatternFromResource("/" + folderName + "_icon.png");

        // 定位 EbSynth 中的目标字段
        Pattern targetField = createPatternFromResource(targetImagePath);

        // 等待元素出现
        screen.wait(folderIcon, 10);
        ebSynthRegion.wait(targetField, 10);

        // 执行拖拽操作，从文件夹图标到 EbSynth 的目标字段
        screen.dragDrop(folderIcon, targetField);
    }

    // 辅助方法：从资源文件创建 Pattern 对象
    private Pattern createPatternFromResource(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new FileNotFoundException("资源文件未找到：" + resourcePath);
        }
        BufferedImage image = ImageIO.read(is);
        return new Pattern(image);
    }

}