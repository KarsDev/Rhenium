#include <string>
#include <fstream>
#include <unordered_map>
#include <mutex>

std::unordered_map<std::string, std::ofstream> openFiles;
std::mutex fileMutex;

// Helper function to get file stream (thread-safe)
std::ofstream& getFileStream(const std::string& path) {
    return openFiles[path];
}

// Open file for writing (truncate)
bool openFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    if (openFiles.find(path) != openFiles.end()) {
        // Already open
        return false;
    }

    std::ofstream ofs(path, std::ios::out | std::ios::trunc);
    if (!ofs.is_open()) return false;

    openFiles[path] = std::move(ofs);
    return true;
}

// Open file for appending
bool openFileAppend(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    if (openFiles.find(path) != openFiles.end()) {
        return false;
    }

    std::ofstream ofs(path, std::ios::out | std::ios::app);
    if (!ofs.is_open()) return false;

    openFiles[path] = std::move(ofs);
    return true;
}

// Write data to file
bool writeFile(const std::string& path, const std::string& data) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second << data;
    return it->second.good();
}

// Flush file
bool flushFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.flush();
    return it->second.good();
}

// Close file
bool closeFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.close();
    openFiles.erase(it);
    return true;
}

extern "C" {
    bool BFW_00(int op, char* path, char* data) {
        std::string p(path);
        std::string d(data ? data : "");

        switch (op) {
            case 0: return openFile(p);       // Open (truncate)
            case 1: return openFileAppend(p); // Open append
            case 2: return writeFile(p, d);   // Write
            case 3: return flushFile(p);      // Flush
            case 4: return closeFile(p);      // Close
        }
        return false;
    }
}
