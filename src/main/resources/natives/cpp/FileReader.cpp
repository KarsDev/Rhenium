#include <string>
#include <fstream>
#include <unordered_map>
#include <memory>
#include <mutex>
#include <iterator>
#include <cstdint>
#include <cstdlib>
#include <cerrno>

struct ReadHandle {
    std::ifstream stream;
    std::mutex mutex;

    explicit ReadHandle(const std::string& path) : stream(path, std::ios::binary) {}
};

static std::unordered_map<int, std::shared_ptr<ReadHandle>> readers;
static std::mutex readersMutex;
static int nextHandle = 1;

static bool parseNonNegative(const char* s, size_t len, int64_t& out) {
    if (len == 0) return false;

    int64_t value = 0;

    for (size_t i = 0; i < len; i++) {
        if (s[i] < '0' || s[i] > '9') return false;

        int digit = s[i] - '0';

        if (value > (INT64_MAX - digit) / 10) return false;

        value = value * 10 + digit;
    }

    out = value;
    return true;
}

static int64_t streamSize(std::ifstream& s) {
    s.clear();
    s.seekg(0, std::ios::end);

    std::streamoff size = s.tellg();

    s.clear();

    return size < 0 ? 0 : static_cast<int64_t>(size);
}

extern "C" {
    // 0 = open
    // 1 = read all (text)
    // 2 = close
    // 3 = read bytes ("offset,count")
    // 4 = read one byte ("offset", "-1" if out of range)
    // 5 = file size in bytes

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
            reader->stream.clear();
            reader->stream.seekg(0, std::ios::beg);

            buffer.assign(
                std::istreambuf_iterator<char>(reader->stream),
                std::istreambuf_iterator<char>()
            );

            return buffer.c_str();
        }

        if (op == 5) {
            buffer = std::to_string(streamSize(reader->stream));
            return buffer.c_str();
        }

        if (op == 3) {
            if (!path) return "";

            std::string arg(path);
            size_t comma = arg.find(',');

            if (comma == std::string::npos) return "";

            int64_t offset = 0;
            int64_t count = 0;

            if (!parseNonNegative(arg.data(), comma, offset)) return "";
            if (!parseNonNegative(arg.data() + comma + 1, arg.size() - comma - 1, count)) return "";

            int64_t size = streamSize(reader->stream);

            if (offset >= size || count == 0) return "";

            if (count > size - offset)
                count = size - offset;

            reader->stream.clear();
            reader->stream.seekg(static_cast<std::streamoff>(offset), std::ios::beg);

            std::string raw(static_cast<size_t>(count), '\0');

            reader->stream.read(&raw[0], static_cast<std::streamsize>(count));

            size_t got = static_cast<size_t>(reader->stream.gcount());

            static const char* digits = "0123456789ABCDEF";

            buffer.clear();
            buffer.reserve(got * 2);

            for (size_t i = 0; i < got; i++) {
                unsigned char c = static_cast<unsigned char>(raw[i]);
                buffer.push_back(digits[c >> 4]);
                buffer.push_back(digits[c & 0x0F]);
            }

            reader->stream.clear();

            return buffer.c_str();
        }

        if (op == 4) {
            if (!path) return "-1";

            std::string arg(path);
            int64_t offset = 0;

            if (!parseNonNegative(arg.data(), arg.size(), offset)) return "-1";

            int64_t size = streamSize(reader->stream);

            if (offset >= size) return "-1";

            reader->stream.clear();
            reader->stream.seekg(static_cast<std::streamoff>(offset), std::ios::beg);

            char c = 0;

            if (!reader->stream.get(c)) {
                reader->stream.clear();
                return "-1";
            }

            reader->stream.clear();

            buffer = std::to_string(static_cast<unsigned int>(static_cast<unsigned char>(c)));
            return buffer.c_str();
        }

        return "0";
    }
}
