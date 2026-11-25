#include <string>
#include <fstream>
#include <unordered_map>
#include <mutex>
#include <memory>
#include <filesystem>
#include <system_error>
#include <cstdint>

namespace fs = std::filesystem;

struct WriteFile {
    std::ofstream stream;
    bool append = false;
};

static std::unordered_map<std::string, WriteFile> openFiles;
static std::mutex fileMutex;

// Open file for writing (truncate)
static bool openFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    if (openFiles.find(path) != openFiles.end()) {
        // Already open
        return false;
    }

    std::ofstream ofs(path, std::ios::out | std::ios::trunc | std::ios::binary);
    if (!ofs.is_open()) return false;

    WriteFile& wf = openFiles[path];
    wf.stream = std::move(ofs);
    wf.append = false;
    return true;
}

// Open file for appending
static bool openFileAppend(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    if (openFiles.find(path) != openFiles.end()) {
        return false;
    }

    std::ofstream ofs(path, std::ios::out | std::ios::app | std::ios::binary);
    if (!ofs.is_open()) return false;

    WriteFile& wf = openFiles[path];
    wf.stream = std::move(ofs);
    wf.append = true;
    return true;
}

// Write data to file
static bool writeFile(const std::string& path, const std::string& data) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.stream.write(data.data(), static_cast<std::streamsize>(data.size()));
    return it->second.stream.good();
}

// Flush file
static bool flushFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.stream.flush();
    return it->second.stream.good();
}

// Close file
static bool closeFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.stream.close();
    openFiles.erase(it);
    return true;
}

static int hexValue(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

// Write a hex string as raw bytes. Validates the whole string before writing anything
static bool writeBytes(const std::string& path, const std::string& hex) {
    if (hex.size() % 2 != 0) return false;

    std::string raw;
    raw.reserve(hex.size() / 2);

    for (size_t i = 0; i < hex.size(); i += 2) {
        int hi = hexValue(hex[i]);
        int lo = hexValue(hex[i + 1]);

        if (hi < 0 || lo < 0) return false;

        raw.push_back(static_cast<char>((hi << 4) | lo));
    }

    return writeFile(path, raw);
}

// Write a single byte given as a decimal string "0" .. "255"
static bool writeByte(const std::string& path, const std::string& text) {
    if (text.empty() || text.size() > 3) return false;

    int value = 0;

    for (char c : text) {
        if (c < '0' || c > '9') return false;
        value = value * 10 + (c - '0');
    }

    if (value > 255) return false;

    char byte = static_cast<char>(static_cast<unsigned char>(value));

    return writeFile(path, std::string(1, byte));
}

// Truncate the file to zero length, keeping it open
static bool truncateFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(fileMutex);
    auto it = openFiles.find(path);
    if (it == openFiles.end()) return false;

    it->second.stream.flush();
    it->second.stream.close();

    // Reopen with trunc. This starts empty, so later writes land at the start either way
    std::ofstream ofs(path, std::ios::out | std::ios::trunc | std::ios::binary);

    if (!ofs.is_open()) {
        openFiles.erase(it);
        return false;
    }

    it->second.stream = std::move(ofs);
    return true;
}

// Current size in bytes
static std::string fileSize(const std::string& path) {
    {
        std::lock_guard<std::mutex> lock(fileMutex);
        auto it = openFiles.find(path);

        if (it != openFiles.end())
            it->second.stream.flush();
    }

    std::error_code ec;
    auto size = fs::file_size(path, ec);

    if (ec) return "0";

    return std::to_string(static_cast<unsigned long long>(size));
}

extern "C" {
    // 0 = open (truncate)
    // 1 = open append
    // 2 = write text
    // 3 = flush
    // 4 = close
    // 5 = write bytes (data = hex string)
    // 6 = write one byte (data = decimal string 0 through 255)
    // 7 = truncate to zero length
    bool BFW_00(int op, char* path, char* data) {
        if (!path) return false;

        std::string p(path);
        std::string d(data ? data : "");

        switch (op) {
            case 0: return openFile(p);       // Open (truncate)
            case 1: return openFileAppend(p); // Open append
            case 2: return writeFile(p, d);   // Write
            case 3: return flushFile(p);      // Flush
            case 4: return closeFile(p);      // Close
            case 5: return writeBytes(p, d);  // Write bytes (hex)
            case 6: return writeByte(p, d);   // Write single byte
            case 7: return truncateFile(p);   // Truncate
        }
        return false;
    }

    // 0 = file size in bytes
    const char* BFW_GFSB(int op, char* path) {
        static thread_local std::string buffer;

        if (!path) return "0";

        switch (op) {
            case 0:
                buffer = fileSize(std::string(path));
                return buffer.c_str();
        }

        return "0";
    }
}