package org.example.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;

/**
 * @author Eric.Lee
 * Date: 2024/9/23
 */
public class ApplicationAutomationTool {
    // EbSynth 應用程式路徑
    private static final String EBSYNC_APP_PATH = "C:\\Users\\a23034\\Downloads\\EbSynth-Beta-Win\\EbSynth.exe";

    public void openEbSynth() {
        try {
            // 打開 EbSynth 應用程式
            Process process = openApplication(EBSYNC_APP_PATH);
            long processId = getProcessId(process);  // 获取 Process ID

            // 等待應用程式啟動完成
            Thread.sleep(3000);

            // 使用 Process ID 將應用程式窗口置頂
            bringWindowToFrontByProcessId((int) processId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 打開應用程式，並返回 Process 物件
    public static Process openApplication(String appPath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(appPath);
        return processBuilder.start();
    }

    // 通过 Process 物件获取其 PID (Process ID)
    public static long getProcessId(Process process) {
        return process.pid();
    }

    // 通过 Process ID 尋找並將視窗置頂
    public static void bringWindowToFrontByProcessId(int processId) {
        User32 user32 = User32.INSTANCE;
        WinDef.HWND hwnd = getWindowHandleByProcessId(processId);
        if (hwnd != null) {
            user32.ShowWindow(hwnd, User32.SW_RESTORE);
            user32.SetForegroundWindow(hwnd);
        } else {
            System.out.println("未找到對應的視窗");
        }
    }

    // 根据 Process ID 获取窗口句柄
    public static WinDef.HWND getWindowHandleByProcessId(int processId) {
        User32 user32 = User32.INSTANCE;
        // 取得第一個視窗
        WinDef.HWND hwnd = user32.FindWindow(null, null);
        while (hwnd != null) {
            IntByReference lpdwProcessId = new IntByReference();
            user32.GetWindowThreadProcessId(hwnd, lpdwProcessId);
            if (lpdwProcessId.getValue() == processId) {
                return hwnd;
            }
            // 取得下一個視窗
            hwnd = user32.FindWindowEx(null, hwnd, null, null);
        }
        // 未找到窗口
        return null;
    }

    // 获取指定进程的窗口句柄（Explorer.exe）
    public static WinDef.HWND findWindowByTitleContains(String titlePart) {
        final WinDef.HWND[] result = new WinDef.HWND[1];

        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hWnd, Pointer data) {
                // 取得視窗標題
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
                String wText = Native.toString(windowText);

                if (wText.contains(titlePart)) {
                    // 檢查視窗是否可見
                    if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                        result[0] = hWnd;
                        return false; // 找到窗口，停止枚舉
                    }
                }
                return true; // 繼續枚舉
            }
        }, null);

        return result[0];
    }

    // 移動視窗到螢幕右半邊
    public static void moveWindowToRightHalf(WinDef.HWND hWnd) {
        User32 user32 = User32.INSTANCE;

        // 取得螢幕尺寸
        int screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN);
        int screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN);

        // 計算右半邊的位置和尺寸
        int x = screenWidth / 2;
        int y = 0;
        int width = screenWidth / 2;
        int height = screenHeight;

        // 調整視窗位置和大小
        user32.SetWindowPos(hWnd, null, x, y, width, height, WinUser.SWP_NOZORDER);
    }

    public void moveWindowToRightHalf(String folderName) {
        // 根據資料夾名稱查找視窗
        WinDef.HWND hWnd = findWindowByTitleContains(folderName);
        if (hWnd != null) {
            // 移動視窗到螢幕右半邊
            moveWindowToRightHalf(hWnd);
        } else {
            System.out.println("未找到資料夾視窗的句柄");
        }
    }

    public void closeWindow(String folderName) {
        try {
            // 根據資料夾名稱查找視窗
            WinDef.HWND hWnd = findWindowByTitleContains(folderName);
            if (hWnd != null) {
                // 發送 WM_CLOSE 訊息，關閉窗口
                User32.INSTANCE.PostMessage(hWnd, WinUser.WM_CLOSE, null, null);
                System.out.println("窗口已關閉: " + folderName);

                // 等待一段時間確認窗口關閉
                Thread.sleep(1000);

                // 再次檢查窗口是否還在，若還在則強制銷毀
                if (User32.INSTANCE.IsWindow(hWnd)) {
                    User32.INSTANCE.DestroyWindow(hWnd);
                    System.out.println("窗口已銷毀: " + folderName);
                } else {
                    System.out.println("窗口已關閉: " + folderName);
                }
            } else {
                System.out.println("未找到文件夹窗口的句柄");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
