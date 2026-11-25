#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <string>

#ifdef _WIN32
    #ifndef WIN32_LEAN_AND_MEAN
        #define WIN32_LEAN_AND_MEAN
    #endif

    #ifndef NOMINMAX
        #define NOMINMAX
    #endif

    #include <windows.h>
    #include <conio.h>
    #include <io.h>
    #include <cstdio>

    #ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
        #define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
    #endif
#else
    #include <poll.h>
    #include <sys/ioctl.h>
    #include <termios.h>
    #include <unistd.h>
#endif

inline bool isInteractive() {
#ifdef _WIN32
    return _isatty(_fileno(stdin)) && _isatty(_fileno(stdout));
#else
    return isatty(STDIN_FILENO) && isatty(STDOUT_FILENO);
#endif
}

inline bool init() {
#ifdef _WIN32
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    HANDLE out = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD mode = 0;

    if (!GetConsoleMode(out, &mode))
        return false;

    return SetConsoleMode(
        out,
        mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING
    ) != 0;
#else
    return true;
#endif
}

namespace detail {

#if !defined(_WIN32) && !defined(__APPLE__)
inline bool xdotool(const char* action) {
    const char* id = std::getenv("WINDOWID");

    if (!id || !*id)
        return false;

    const std::string wid(id);

    if (wid.find_first_not_of("0123456789") != std::string::npos)
        return false;

    const std::string cmd =
        std::string("xdotool ") + action + " " + wid +
        " >/dev/null 2>&1";

    return std::system(cmd.c_str()) == 0;
}
#endif

#ifdef __APPLE__
inline bool macSetVisible(bool visible) {
    const char* term = std::getenv("TERM_PROGRAM");
    const std::string t = term ? term : "";

    std::string app;

    if (t == "Apple_Terminal")
        app = "Terminal";
    else if (t == "iTerm.app")
        app = "iTerm2";
    else
        return false;

    const std::string cmd =
        "osascript -e 'tell application \"System Events\" to set visible of "
        "process \"" + app + "\" to " + (visible ? "true" : "false") +
        "' >/dev/null 2>&1";

    return std::system(cmd.c_str()) == 0;
}
#endif

inline void ansi(const std::string& code) {
    if (isInteractive())
        std::cout << code << std::flush;
}

}

inline bool hide() {
#ifdef _WIN32
    HWND w = GetConsoleWindow();

    if (!w)
        return false;

    ShowWindow(w, SW_HIDE);
    return true;
#elif defined(__APPLE__)
    return detail::macSetVisible(false);
#else
    return detail::xdotool("windowunmap");
#endif
}

inline bool show() {
#ifdef _WIN32
    HWND w = GetConsoleWindow();

    if (!w)
        return false;

    ShowWindow(w, SW_SHOW);
    SetForegroundWindow(w);
    return true;
#elif defined(__APPLE__)
    return detail::macSetVisible(true);
#else
    return detail::xdotool("windowmap");
#endif
}


#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <string>

#ifdef _WIN32
    #ifndef WIN32_LEAN_AND_MEAN
        #define WIN32_LEAN_AND_MEAN
    #endif

    #ifndef NOMINMAX
        #define NOMINMAX
    #endif

    #include <windows.h>
    #include <conio.h>
    #include <io.h>
    #include <cstdio>

    #ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
        #define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
    #endif
#else
    #include <poll.h>
    #include <sys/ioctl.h>
    #include <termios.h>
    #include <unistd.h>
#endif

namespace console {

inline bool isInteractive() {
#ifdef _WIN32
    return _isatty(_fileno(stdin)) && _isatty(_fileno(stdout));
#else
    return isatty(STDIN_FILENO) && isatty(STDOUT_FILENO);
#endif
}

inline bool init() {
#ifdef _WIN32
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    HANDLE out = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD mode = 0;

    if (!GetConsoleMode(out, &mode))
        return false;

    return SetConsoleMode(
        out,
        mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING
    ) != 0;
#else
    return true;
#endif
}

namespace detail {

#if !defined(_WIN32) && !defined(__APPLE__)
inline bool xdotool(const char* action) {
    const char* id = std::getenv("WINDOWID");

    if (!id || !*id)
        return false;

    const std::string wid(id);

    if (wid.find_first_not_of("0123456789") != std::string::npos)
        return false;

    const std::string cmd =
        std::string("xdotool ") + action + " " + wid +
        " >/dev/null 2>&1";

    return std::system(cmd.c_str()) == 0;
}
#endif

#ifdef __APPLE__
inline bool macSetVisible(bool visible) {
    const char* term = std::getenv("TERM_PROGRAM");
    const std::string t = term ? term : "";

    std::string app;

    if (t == "Apple_Terminal")
        app = "Terminal";
    else if (t == "iTerm.app")
        app = "iTerm2";
    else
        return false;

    const std::string cmd =
        "osascript -e 'tell application \"System Events\" to set visible of "
        "process \"" + app + "\" to " + (visible ? "true" : "false") +
        "' >/dev/null 2>&1";

    return std::system(cmd.c_str()) == 0;
}
#endif

inline void ansi(const std::string& code) {
    if (isInteractive())
        std::cout << code << std::flush;
}

}

inline bool hide() {
#ifdef _WIN32
    HWND w = GetConsoleWindow();

    if (!w)
        return false;

    ShowWindow(w, SW_HIDE);
    return true;
#elif defined(__APPLE__)
    return detail::macSetVisible(false);
#else
    return detail::xdotool("windowunmap");
#endif
}

inline bool show() {
#ifdef _WIN32
    HWND w = GetConsoleWindow();

    if (!w)
        return false;

    ShowWindow(w, SW_SHOW);
    SetForegroundWindow(w);
    return true;
#elif defined(__APPLE__)
    return detail::macSetVisible(true);
#else
    return detail::xdotool("windowmap");
#endif
}

inline void clear() {
    detail::ansi("\x1b[2J\x1b[3J\x1b[H");
}

inline void clearLine() {
    detail::ansi("\r\x1b[2K");
}

inline void gotoXY(int x, int y) {
    detail::ansi(
        "\x1b[" + std::to_string(y + 1) + ";" +
        std::to_string(x + 1) + "H"
    );
}

inline void setTitle(const std::string& title) {
    detail::ansi("\x1b]0;" + title + "\x07");
}

inline void beep() {
    detail::ansi("\a");
}

enum class Color {
    Black = 30,
    Red,
    Green,
    Yellow,
    Blue,
    Magenta,
    Cyan,
    White,
    Default = 39
};

inline void setColor(Color fg) {
    detail::ansi(
        "\x1b[" + std::to_string(static_cast<int>(fg)) + "m"
    );
}

inline void resetColor() {
    detail::ansi("\x1b[0m");
}

struct Size {
    int columns;
    int rows;
};

inline Size size() {
#ifdef _WIN32
    CONSOLE_SCREEN_BUFFER_INFO i;

    if (GetConsoleScreenBufferInfo(
        GetStdHandle(STD_OUTPUT_HANDLE), &i
    )) {
        return {
            i.srWindow.Right - i.srWindow.Left + 1,
            i.srWindow.Bottom - i.srWindow.Top + 1
        };
    }
#else
    winsize w{};

    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &w) == 0 && w.ws_col > 0)
        return { w.ws_col, w.ws_row };
#endif

    return { 80, 24 };
}

#ifndef _WIN32
class TermMode {
    termios old_{};
    bool active_ = false;

public:
    explicit TermMode(tcflag_t flagsToClear) {
        if (tcgetattr(STDIN_FILENO, &old_) != 0)
            return;

        termios t = old_;
        t.c_lflag &= ~flagsToClear;

        if (flagsToClear & ICANON) {
            t.c_cc[VMIN] = 1;
            t.c_cc[VTIME] = 0;
        }

        active_ = tcsetattr(STDIN_FILENO, TCSANOW, &t) == 0;
    }

    ~TermMode() {
        if (active_)
            tcsetattr(STDIN_FILENO, TCSANOW, &old_);
    }

    TermMode(const TermMode&) = delete;
    TermMode& operator=(const TermMode&) = delete;
};
#endif

inline bool kbhit() {
#ifdef _WIN32
    return _kbhit() != 0;
#else
    TermMode raw(ICANON | ECHO);
    pollfd p{ STDIN_FILENO, POLLIN, 0 };

    return poll(&p, 1, 0) > 0;
#endif
}

inline int getch() {
    std::cout << std::flush;

#ifdef _WIN32
    return _getch();
#else
    TermMode raw(ICANON | ECHO);
    unsigned char c = 0;

    return read(STDIN_FILENO, &c, 1) == 1 ? c : -1;
#endif
}

}

bool initConsole() {
    return console::init();
}

int consoleIsInteractive() {
    return console::isInteractive() ? 1 : 0;
}

int consoleHide() {
    return console::hide() ? 1 : 0;
}

int consoleShow() {
    return console::show() ? 1 : 0;
}

void consoleClear() {
    console::clear();
}

void consoleClearLine() {
    console::clearLine();
}

void consoleBeep() {
    console::beep();
}

void consoleResetColor() {
    console::resetColor();
}

void consoleGotoXY(int x, int y) {
    console::gotoXY(x < 0 ? 0 : x, y < 0 ? 0 : y);
}

void consoleSetTitle(const char* title) {
    if (title)
        console::setTitle(title);
}

void consoleSetColor(int color) {
    if ((color >= 30 && color <= 37) || color == 39)
        console::setColor(static_cast<console::Color>(color));
}

int consoleGetSize() {
    const console::Size s = console::size();
    return static_cast<int32_t>((s.columns << 16) | s.rows);
}

int consoleGetChar() {
    return console::getch();
}

enum BCU_Option {
    BCU_INIT = 0,
    BCU_IS_INTERACTIVE,
    BCU_HIDE,
    BCU_SHOW,
    BCU_CLEAR,
    BCU_CLEAR_LINE,
    BCU_BEEP,
    BCU_RESET_COLOR,
    BCU_GOTO_XY,
    BCU_SET_COLOR,
    BCU_GET_CHAR,
    BCU_GET_SIZE
};

extern "C" int BCU_00(int option, int param1, int param2) {
    switch (option) {
        case BCU_INIT:
            return initConsole();

        case BCU_IS_INTERACTIVE:
            return consoleIsInteractive();

        case BCU_HIDE:
            return consoleHide();

        case BCU_SHOW:
            return consoleShow();

        case BCU_CLEAR:
            consoleClear();
            return 1;

        case BCU_CLEAR_LINE:
            consoleClearLine();
            return 1;

        case BCU_BEEP:
            consoleBeep();
            return 1;

        case BCU_RESET_COLOR:
            consoleResetColor();
            return 1;

        case BCU_GOTO_XY:
            consoleGotoXY(param1, param2);
            return 1;

        case BCU_SET_COLOR:
            consoleSetColor(param1);
            return 1;

        case BCU_GET_CHAR:
            return consoleGetChar();

        case BCU_GET_SIZE:
            return consoleGetSize();

        default:
            return 0;
    }
}
