package org.example.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * 工具類，用於打開資料夾
 * @author Eric.Lee
 * Date: 2024/9/25
 */
public class FolderOpenerTool {

    // 私有構造函數，防止實例化
    private FolderOpenerTool() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void openFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!Desktop.isDesktopSupported()) {
            System.out.println("Desktop API 不支持該操作系統");
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (folder.exists() && folder.isDirectory()) {
            desktop.open(folder);  // 打開資料夾
        } else {
            System.out.println("資料夾不存在或路徑不正確: " + folderPath);
        }
    }
}
