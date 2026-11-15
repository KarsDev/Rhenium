#include <string>
#include <fstream>
#include <unordered_map>
#include <memory>
#include <mutex>
#include <iterator>
#include <cstdint>

struct ReadHandle {
    std::ifstream stream;
    std::mutex mutex;

    explicit ReadHandle(const std::string& path) : stream(path, std::ios::binary) {}
};

static std::unordered_map<int, std::shared_ptr<ReadHandle>> readers;
static std::mutex readersMutex;
static int nextHandle = 1;

extern "C" {
    // 0 = open
    // 1 = read all
    // 2 = close

    const char* BFR_00(int op, int handle, const char* path) {
        static thread_local std::string buffer;

        if (op == 0) {
            if (!path) return "0";

            auto reader = std::make_shared<ReadHandle>(path);

            if (!reader->stream.is_open())
                return "0";

            std::lock_guard<std::mutex> lock(readersMutex);

            int id = nextHandle++;
            readers.emplace(id, std::move(reader));

            buffer = std::to_string(id);
            return buffer.c_str();
        }

        std::shared_ptr<ReadHandle> reader;
        {
            std::lock_guard<std::mutex> lock(readersMutex);

            auto it = readers.find(handle);

            if (it == readers.end())
                return "0";

            reader = it->second;

            if (op == 2) {
                readers.erase(it);
                return "1";
            }
        }

        std::lock_guard<std::mutex> lock(reader->mutex);

        if (op == 1) {
            buffer.assign(
                std::istreambuf_iterator<char>(reader->stream),
                std::istreambuf_iterator<char>()
            );

            return buffer.c_str();
        }

        return "0";
    }
}
