#include <string>
#include <fstream>
#include <filesystem>

namespace fs = std::filesystem;

bool fileExists(const char* file)
{
    return fs::exists(file);
}

bool createFile(const char* file)
{
    if (fileExists(file))
        return true;

    try {
        fs::path p(file);

        if (p.has_parent_path())
            fs::create_directories(p.parent_path());

        std::ofstream ofs(file);
        return ofs.good();
    }
    catch (...) {
        return false;
    }
}

bool isDirectory(const char* file)
{
    return fs::is_directory(file);
}

bool isFile(const char* file)
{
    return fs::is_regular_file(file);
}

bool renameFile(const char* file, const char* newName)
{
    try {
        fs::rename(file, newName);
        return true;
    }
    catch (...) {
        return false;
    }
}

bool deleteFile(const char* file)
{
    return fs::remove(file);
}

extern "C" {
    bool BFN_00(int operation, char* file) {
        switch (operation){
            case 0:
                return fileExists(file);
            case 1:
                return createFile(file);
            case 2:
                return isDirectory(file);
            case 3:
                return isFile(file);
            case 4: {
                std::string full_string = file;
                const std::string DELIMITER = "//";

                size_t delimiter_pos = full_string.find(DELIMITER);

                if (delimiter_pos != std::string::npos) {
                    std::string name = full_string.substr(0, delimiter_pos);

                    size_t newName_start_pos = delimiter_pos + DELIMITER.length();
                    std::string newName = full_string.substr(newName_start_pos);
                    
                    return renameFile(name.c_str(), newName.c_str());
                }
                break;
            }
            case 5:
                return deleteFile(file);
        }
        
        return false;
    }
}