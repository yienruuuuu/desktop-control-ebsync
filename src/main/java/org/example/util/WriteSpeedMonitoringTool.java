package org.example.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eric.Lee
 * Date:2024/9/28
 */
@Component
public class WriteSpeedMonitoringTool {
    // 為每個進程存儲寫入數據量和時間戳
    private static final Map<Integer, Long> previousWriteBytesMap = new HashMap<>();
    private static final Map<Integer, Long> previousTimestampMap = new HashMap<>();

    // 用來記錄每個進程低於門檻的次數
    private static final Map<Integer, Integer> lowWriteCountMap = new HashMap<>();
    private static final int LOW_WRITE_THRESHOLD = 5;  // 需要低於門檻的次數，才視為進程完成

    public void monitorEbsynthAndCloseWindowsAfterWork() {
        // 定期檢查每個 EbSynth 進程的磁碟寫入速度
        List<Integer> pids = findEbSynthPIDs();
        if (pids.isEmpty()) {
            System.out.println("未找到任何 EbSynth 進程。");
            return;
        }

        boolean allProcessesCompleted = false; // 標記所有進程是否完成
        while (!allProcessesCompleted) {
            allProcessesCompleted = monitorEbSynthProcesses(pids);
            if (allProcessesCompleted) {
                // 當所有進程完成後，統一關閉所有 EbSynth 進程
                closeAllEbSynthProcesses(pids);
                System.out.println("所有 EbSynth 進程已關閉。");
            }

            try {
                // 每 10 秒檢查一次
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 查找所有 EbSynth.exe 進程的 PID
    public static List<Integer> findEbSynthPIDs() {
        List<Integer> pids = new ArrayList<>();
        Tlhelp32.PROCESSENTRY32 processEntry = new Tlhelp32.PROCESSENTRY32();
        HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));  // 修正為 WinDef.DWORD(0)

        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                String exeFile = Native.toString(processEntry.szExeFile);
                if ("EbSynth.exe".equalsIgnoreCase(exeFile)) {
                    pids.add(processEntry.th32ProcessID.intValue());
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        return pids;
    }

    // 檢查磁碟寫入速度
    public static long checkDiskWriteSpeed(int pid) {
        HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, pid);
        if (hProcess != null) {
            WinNT.IO_COUNTERS ioCounters = new WinNT.IO_COUNTERS();
            if (Kernel32.INSTANCE.GetProcessIoCounters(hProcess, ioCounters)) {
                long currentWriteBytes = ioCounters.WriteTransferCount;
                long currentTime = System.currentTimeMillis();  // 當前時間戳

                long previousWriteBytes = previousWriteBytesMap.getOrDefault(pid, 0L);
                long previousTimestamp = previousTimestampMap.getOrDefault(pid, currentTime);

                if (previousWriteBytes == 0) {
                    // 第一次檢查，直接記錄初始值
                    previousWriteBytesMap.put(pid, currentWriteBytes);
                    previousTimestampMap.put(pid, currentTime);
                    Kernel32.INSTANCE.CloseHandle(hProcess);
                    return 0;  // 初次無法計算
                }

                // 計算時間差（毫秒）
                long timeDiff = currentTime - previousTimestamp;
                if (timeDiff == 0) {
                    timeDiff = 1;  // 防止除以零錯誤
                }

                // 計算寫入速度（字節/秒）
                long bytesWritten = currentWriteBytes - previousWriteBytes;
                long writeSpeed = bytesWritten * 1000 / timeDiff;  // 轉換為字節/秒

                // 更新上次檢查數據
                previousWriteBytesMap.put(pid, currentWriteBytes);
                previousTimestampMap.put(pid, currentTime);

                Kernel32.INSTANCE.CloseHandle(hProcess);
                return writeSpeed;
            }
        }
        return -1;
    }

    // 檢查所有 EbSynth.exe 進程，當寫入速度降到一定門檻時返回 true，表示所有進程已完成
    public static boolean monitorEbSynthProcesses(List<Integer> pids) {
        // 設定門檻（例如：1 MB = 1,048,576 字節）
        long threshold = 1_048_576;  // 1 MB
        boolean allBelowThreshold = true; // 假設所有進程都已完成

        for (int pid : pids) {
            long writeSpeed = checkDiskWriteSpeed(pid);

            if (writeSpeed != -1) {
                System.out.println("進程 " + pid + " 的當前寫入速度: " + writeSpeed + " 字節/秒");

                if (writeSpeed >= threshold) {
                    // 如果寫入速度大於門檻，將低於門檻的計數歸零
                    lowWriteCountMap.put(pid, 0);
                    allBelowThreshold = false;
                } else {
                    // 如果寫入速度小於門檻，累加低寫入計數
                    int lowCount = lowWriteCountMap.getOrDefault(pid, 0) + 1;
                    lowWriteCountMap.put(pid, lowCount);

                    // 只有當進程的寫入速度持續低於門檻超過一定次數後，才認為該進程完成
                    if (lowCount < LOW_WRITE_THRESHOLD) {
                        allBelowThreshold = false;
                    }
                }
            } else {
                System.out.println("無法檢查進程 " + pid + " 的磁碟寫入速度。");
                allBelowThreshold = false;
            }
        }

        // 如果所有進程的寫入速度都低於門檻且超過設定的次數，返回 true，表示可以進行後續操作
        return allBelowThreshold;
    }

    // 關閉所有 EbSynth.exe 進程
    public static void closeAllEbSynthProcesses(List<Integer> pids) {
        for (int pid : pids) {
            HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_TERMINATE, false, pid);
            if (hProcess != null) {
                if (Kernel32.INSTANCE.TerminateProcess(hProcess, 0)) {
                    System.out.println("已關閉進程: " + pid);
                } else {
                    System.out.println("無法關閉進程: " + pid);
                }
                Kernel32.INSTANCE.CloseHandle(hProcess);
            }
        }
    }
}
