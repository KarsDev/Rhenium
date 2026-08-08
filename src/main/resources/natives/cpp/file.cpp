#include <filesystem>
#include <fstream>
#include <string>

namespace fs = std::filesystem;

static bool fileExists(const char* file)
{
    if (!file)
        return false;

    try {
        return fs::exists(file);
    }
    catch (...) {
        return false;
    }
}

static bool createFile(const char* file)
{
    if (!file)
        return false;

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

static bool isDirectory(const char* file)
{
    if (!file)
        return false;

    try {
        return fs::is_directory(file);
    }
    catch (...) {
        return false;
    }
}

static bool isFile(const char* file)
{
    if (!file)
        return false;

    try {
        return fs::is_regular_file(file);
    }
    catch (...) {
        return false;
    }
}

static bool renameFile(const char* file, const char* newName)
{
    if (!file || !newName)
        return false;

    try {
        fs::rename(file, newName);
        return true;
    }
    catch (...) {
        return false;
    }
}

static bool deleteFile(const char* file)
{
    if (!file)
        return false;

    try {
        return fs::remove(file);
    }
    catch (...) {
        return false;
    }
}


// ============================================================
// Public ABI
// ============================================================

extern "C" bool BFN_00(
    int operation,
    const char* arg1,
    const char* arg2
)
{
    switch (operation)
    {
        case 0:
            // fileExists(path)
            return fileExists(arg1);

        case 1:
            // createFile(path)
            return createFile(arg1);

        case 2:
            // isDirectory(path)
            return isDirectory(arg1);

        case 3:
            // isFile(path)
            return isFile(arg1);

        case 4:
            // renameFile(oldPath, newPath)
            return renameFile(arg1, arg2);

        case 5:
            // deleteFile(path)
            return deleteFile(arg1);

        default:
            return false;
    }
}