#include <string>
#include <fstream>
#include <unordered_map>
#include <mutex>

std::unordered_map<std::string, std::ifstream> openReadFiles;
std::mutex readFileMutex;

// Helper function to get file stream (thread-safe)
std::ifstream& getReadFileStream(const std::string& path) {
    return openReadFiles[path];
}

// Open file for reading
bool openReadFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(readFileMutex);
    if (openReadFiles.find(path) != openReadFiles.end()) {
        // Already open
        return false;
    }

    std::ifstream ifs(path, std::ios::in);
    if (!ifs.is_open()) return false;

    openReadFiles[path] = std::move(ifs);
    return true;
}

// Read entire file content
std::string readFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(readFileMutex);
    auto it = openReadFiles.find(path);
    if (it == openReadFiles.end()) return "";

    std::ifstream& ifs = it->second;
    ifs.seekg(0, std::ios::beg); // Ensure start at beginning if needed
    std::string content((std::istreambuf_iterator<char>(ifs)),
                         std::istreambuf_iterator<char>());
    return content;
}

// Close file
bool closeReadFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(readFileMutex);
    auto it = openReadFiles.find(path);
    if (it == openReadFiles.end()) return false;

    it->second.close();
    openReadFiles.erase(it);
    return true;
}

extern "C" {
    // op codes: 0 = open, 1 = read, 2 = close
    const int READ_OPEN = 0;
    const int READ_READ = 1;
    const int READ_CLOSE = 2;

    char* BFR_00(int op, char* path) {
        static std::string buffer; // static to return pointer safely
        std::string p(path ? path : "");

        switch (op) {
            case READ_OPEN:
                return openReadFile(p) ? (char*)"1" : (char*)"0";
            case READ_READ:
                buffer = readFile(p);
                return (char*)buffer.c_str();
            case READ_CLOSE:
                return closeReadFile(p) ? (char*)"1" : (char*)"0";
        }
        return (char*)"0";
    }
}
