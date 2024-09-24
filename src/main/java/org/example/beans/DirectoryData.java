package org.example.beans;

import java.util.List;

/**
 * @author Eric.Lee
 * Date: 2024/9/23
 */
public record DirectoryData(String rootPath, List<String> numericFolders) {
}
