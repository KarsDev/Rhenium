#include <cstdarg>
#include <cstdio>

extern "C" {
    int sprintf_external(char* buf, size_t buf_size, const char* fmt, ...) {
        if (!buf || !fmt || buf_size == 0)
            return -1;

        va_list args;
        va_start(args, fmt);
        int written = vsnprintf(buf, buf_size, fmt, args);
        va_end(args);

        return written;
    }
}
