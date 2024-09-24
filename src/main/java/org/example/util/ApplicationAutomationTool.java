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

    public static void main(String[] args) throws Exception {
        // 打開 EbSynth 應用程式
        Process process = openApplication(EBSYNC_APP_PATH);
        long processId = getProcessId(process);  // 获取 Process ID

        // 等待應用程式啟動完成
        Thread.sleep(3000);

        // 使用 Process ID 將應用程式窗口置頂
        bringWindowToFrontByProcessId((int) processId);

    }

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
        WinDef.HWND hwnd = user32.FindWindow(null, null);  // 取得第一個視窗
        while (hwnd != null) {
            IntByReference lpdwProcessId = new IntByReference();
            user32.GetWindowThreadProcessId(hwnd, lpdwProcessId);
            if (lpdwProcessId.getValue() == processId) {
                return hwnd;
            }
            hwnd = user32.FindWindowEx(null, hwnd, null, null);  // 取得下一個視窗
        }
        return null;  // 未找到窗口
    }

    // 获取指定进程的窗口句柄（Explorer.exe）
    public static WinDef.HWND findWindowByTitleContains(String titlePart) {
        final WinDef.HWND[] result = new WinDef.HWND[1];

        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hWnd, Pointer data) {
                // 获取窗口标题
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
                String wText = Native.toString(windowText);

                if (wText.contains(titlePart)) {
                    // 检查窗口是否可见
                    if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                        result[0] = hWnd;
                        return false; // 找到窗口，停止枚举
                    }
                }
                return true; // 继续枚举
            }
        }, null);

        return result[0];
    }

    // 移动窗口到屏幕右半边
    public static void moveWindowToRightHalf(WinDef.HWND hWnd) {
        User32 user32 = User32.INSTANCE;

        // 获取屏幕尺寸
        int screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN);
        int screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN);

        // 计算右半边的位置和尺寸
        int x = screenWidth / 2;
        int y = 0;
        int width = screenWidth / 2;
        int height = screenHeight;

        // 调整窗口位置和大小
        user32.SetWindowPos(hWnd, null, x, y, width, height, WinUser.SWP_NOZORDER);
    }

    public void moveWindowToRightHalf(String folderName) {
        // 根据文件夹名称查找窗口
        WinDef.HWND hWnd = findWindowByTitleContains(folderName);
        if (hWnd != null) {
            // 移动窗口到屏幕右半边
            moveWindowToRightHalf(hWnd);
        } else {
            System.out.println("未找到文件夹窗口的句柄");
        }
    }

    public void closeeWindow(String folderName) {
        // 根据文件夹名称查找窗口
        WinDef.HWND hWnd = findWindowByTitleContains(folderName);
        if (hWnd != null) {
            // 發送 WM_CLOSE 訊息，關閉窗口
            User32.INSTANCE.PostMessage(hWnd, WinUser.WM_CLOSE, null, null);
            System.out.println("窗口已關閉: " + folderName);
        } else {
            System.out.println("未找到文件夹窗口的句柄");
        }
    }
}
