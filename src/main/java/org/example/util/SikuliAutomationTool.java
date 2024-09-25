package org.example.util;

import org.sikuli.script.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Eric.Lee
 * Date: 2024/9/24
 */
public class SikuliAutomationTool {
    private final DirectoryChooserTool chooserTool;

    public SikuliAutomationTool(DirectoryChooserTool chooserTool) {
        this.chooserTool = chooserTool;
    }

    public void processFolder(String numericFolderPath) {
        ApplicationAutomationTool applicationAutomationTool = new ApplicationAutomationTool();
        try {
            if (chooserTool.isStopRequested()) return;

            Screen screen = new Screen();
            // 打開數字資料夾（例如 "\1"）
            FolderOpenerTool.openFolder(numericFolderPath);
            Thread.sleep(2000); // 等待資料夾視窗打開
            // 取得資料夾名稱
            String folderName = new File(numericFolderPath).getName();
            applicationAutomationTool.moveWindowToRightHalf(folderName);
            Thread.sleep(3000); // 等待文件夹移動
            // 打開 EbSynth
            applicationAutomationTool.openEbSynth();
            // 使用 SikuliX 識別 EbSynth 初始化介面
            Pattern ebsynthInitial = createPatternFromResource("/ebsynth-init.png");
            // 等待 EbSynth 初始化界面出现
            screen.wait(ebsynthInitial, 10);
            // 取得 EbSynth 視窗的區域
            Match ebsynthWindow = screen.find(ebsynthInitial);
            // 在該視窗區域內執行操作
            Region ebSynthRegion = ebsynthWindow;
            // 定位 keys 資料夾圖示並拖曳到 EbSynth 的 Keyframes 字段
            dragAndDropFolder("keys", "/keyframes.png", ebSynthRegion);
            // 定位 video 資料夾圖示並拖曳到 EbSynth 的 Video 字段
            dragAndDropFolder("video", "/video.png", ebSynthRegion);
            // "Run All" 按鈕 Pattern
            Pattern runAllButton = createPatternFromResource("/run_all_button.png");

            // 拓寬run all按鈕判斷區域
            Region ebSynthRegion2 = new Region(
                    ebsynthWindow.getX(),
                    ebsynthWindow.getY(),
                    ebsynthWindow.getW(),
                    ebsynthWindow.getH() + 700
            );
            // 點亮選擇區域
            ebSynthRegion2.highlight(2);
            // 點擊 "Run All" 按鈕
            ebSynthRegion2.click(runAllButton);
            // 關閉數字資料夾
            applicationAutomationTool.closeWindow(folderName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dragAndDropFolder(String folderName, String targetImagePath, Region ebSynthRegion) throws Exception {
        Screen screen = new Screen();

        // 定位要拖曳的資料夾圖示（在數位資料夾視窗中）
        Pattern folderIcon = createPatternFromResource("/" + folderName + "_icon.png");

        // 定位 EbSynth 中的目標字段
        Pattern targetField = createPatternFromResource(targetImagePath);

        // 等待元素出現
        screen.wait(folderIcon, 10);
        ebSynthRegion.wait(targetField, 10);

        // 執行拖曳操作，從資料夾圖示到 EbSynth 的目標字段
        screen.dragDrop(folderIcon, targetField);
    }

    // 每 10 秒檢查所有 EbSynth 視窗狀態，直到完成後返回
    public void checkEbSynthWindows(int countLimitEachBatch) {
        try {
            //運行時間先等待
            Thread.sleep(60000L * countLimitEachBatch);

            Screen screen = new Screen();
            Pattern finishedPattern = createPatternFromResource("/ebsynth_finish_sign.png");
            int countClosedWindows = 0;

            // 持續檢查直到所有視窗關閉
            while (true) {
                if (chooserTool.isStopRequested()) break;
                boolean hasRunningWindow = false;

                // 查找 "完成" 的 EbSynth 視窗
                while (countClosedWindows != countLimitEachBatch && screen.findAll(finishedPattern).hasNext()) {
                    if (chooserTool.isStopRequested()) break;
                    // 在該視窗區域內執行操作
                    Region ebSynthRegion = screen.find(finishedPattern);
                    ebSynthRegion.highlight(2);
                    if (matchAnyInCollection(ebSynthRegion)) {
                        System.out.print("檢測到cancel文字 繼續等待完成");
                        Thread.sleep(2000);
                        continue;
                    }

                    // 自定義的關閉視窗方法
                    closeWindowUsingX(ebSynthRegion);
                    hasRunningWindow = true;
                    countClosedWindows++;
                    Thread.sleep(2000);
                }

                // 如果沒有找到任何正在執行的視窗，則退出循環
                if (!hasRunningWindow) {
                    System.out.println("所有 EbSynth 視窗均已完成並關閉。");
                    break;
                }
                // 每 10 秒檢查一次
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 輔助方法：從資源檔案建立 Pattern 對象
    private Pattern createPatternFromResource(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new FileNotFoundException("資源文件未找到：" + resourcePath);
        }
        BufferedImage image = ImageIO.read(is);
        return new Pattern(image);
    }

    // 自定義的關閉視窗方法
    private void closeWindowUsingX(Region match) {
        Screen screen = new Screen();
        try {
            if (match != null) {
                System.out.println("找到圖片，準備點擊右上角的 'X' 按鈕");

                // 計算 "X" 的位置：匹配區域的右上角
                int x = match.getX() + match.getW() - 10;  // 右邊 - 10 像素
                int y = match.getY() + 10;                 // 上方 + 10 像素

                // 點擊右上角的 "X" 按鈕
                screen.click(new Location(x, y));
                System.out.println("已點擊 'X' 按鈕，關閉視窗");
            } else {
                throw new Exception("找不到匹配的圖片：" + match);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在指定區域內檢查是否存在集合中的任意圖片
     */
    public boolean matchAnyInCollection(Region region) {
        // 拓寬判斷區域
        Region newRegion = new Region(
                region.getX(),
                region.getY(),
                region.getW(),
                region.getH() + 700
        );
        newRegion.highlight(2);

        File folder = new File("src/main/resources/cancel_collection");
        // 確認資料夾存在
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("指定的資料夾不存在或不是資料夾！");
            return false;
        }

        // 獲取資料夾中所有的圖像文件
        File[] imageFiles = folder.listFiles((dir, name) -> name.endsWith(".png"));

        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("資料夾中沒有圖片！");
            return false;
        }
        // 創建一個線程池
        int coreCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(coreCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        // 遍歷圖像文件，逐一提交比對任務
        for (File imageFile : imageFiles) {
            Callable<Boolean> task = () -> {
                try {
                    System.out.println("正在比對圖像: " + imageFile.getName());
                    // 使用 createPatternFromResource 來創建 Pattern
                    Pattern pattern = createPatternFromResource("/cancel_collection/" + imageFile.getName());

                    // 在指定的區域內檢查是否存在匹配
                    if (newRegion.exists(pattern) != null) {
                        System.out.println("匹配成功: " + imageFile.getName());
                        return true;  // 匹配成功
                    }
                } catch (Exception e) {
                    System.out.println("未找到匹配: " + imageFile.getName());
                }
                // 匹配失敗
                return false;
            };
            // 提交任務
            futures.add(executorService.submit(task));
        }

        // 檢查任務結果
        try {
            for (Future<Boolean> future : futures) {
                if (Boolean.TRUE.equals(future.get())) {
                    System.out.println("找到匹配圖像，結束搜索！");
                    executorService.shutdownNow();  // 匹配成功後停止所有任務
                    return true;  // 一旦找到匹配，返回 true
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 確保線程池關閉
            executorService.shutdown();
        }
        System.out.println("圖像集合中無匹配項！");
        // 沒有匹配的情況下返回 false
        return false;
    }

}