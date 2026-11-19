// win.cpp - software-framebuffer window library (Windows / Linux X11 / macOS)
// not thread-safe.  On macOS all calls must be on the main thread.

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <cstddef>
#include <cstdio>
#include <cmath>
#include <limits>
#include <new>

#ifndef SCREEN_ALPHA_BLEND
#define SCREEN_ALPHA_BLEND 1
#endif

#ifdef _WIN32

#ifndef UNICODE
#define UNICODE
#endif

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <windows.h>
#include <windowsx.h>

#elif defined(__APPLE__)

#ifndef __OBJC__
#error "On macOS this file must be compiled as Objective-C++ (clang -x objective-c++)."
#endif

#import <Cocoa/Cocoa.h>
#import <CoreGraphics/CoreGraphics.h>

#elif defined(__linux__)

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/XKBlib.h>
#include <X11/keysym.h>

#endif


// ============================================================
// PORTABLE KEY CODES
// ============================================================
// Values match Windows virtual-key codes where one exists, but they are the
// same on every platform.  Unknown physical keys are not reported at all.

enum ScreenKey : int
{
    SCREEN_KEY_NONE = 0,

    // Mouse buttons (reported through the key callback / isKeyPressed).
    SCREEN_MOUSE_LEFT   = 1,
    SCREEN_MOUSE_RIGHT  = 2,
    SCREEN_MOUSE_MIDDLE = 4,

    SCREEN_KEY_BACKSPACE = 8,
    SCREEN_KEY_TAB       = 9,
    SCREEN_KEY_ENTER     = 13,
    SCREEN_KEY_SHIFT     = 16,
    SCREEN_KEY_CTRL      = 17,
    SCREEN_KEY_ALT       = 18,
    SCREEN_KEY_PAUSE     = 19,
    SCREEN_KEY_CAPS_LOCK = 20,
    SCREEN_KEY_ESCAPE    = 27,
    SCREEN_KEY_SPACE     = 32,
    SCREEN_KEY_PAGE_UP   = 33,
    SCREEN_KEY_PAGE_DOWN = 34,
    SCREEN_KEY_END       = 35,
    SCREEN_KEY_HOME      = 36,
    SCREEN_KEY_LEFT      = 37,
    SCREEN_KEY_UP        = 38,
    SCREEN_KEY_RIGHT     = 39,
    SCREEN_KEY_DOWN      = 40,
    SCREEN_KEY_INSERT    = 45,
    SCREEN_KEY_DELETE    = 46,

    SCREEN_KEY_0 = 48, SCREEN_KEY_1, SCREEN_KEY_2, SCREEN_KEY_3, SCREEN_KEY_4,
    SCREEN_KEY_5, SCREEN_KEY_6, SCREEN_KEY_7, SCREEN_KEY_8, SCREEN_KEY_9,

    SCREEN_KEY_A = 65, SCREEN_KEY_B, SCREEN_KEY_C, SCREEN_KEY_D, SCREEN_KEY_E,
    SCREEN_KEY_F, SCREEN_KEY_G, SCREEN_KEY_H, SCREEN_KEY_I, SCREEN_KEY_J,
    SCREEN_KEY_K, SCREEN_KEY_L, SCREEN_KEY_M, SCREEN_KEY_N, SCREEN_KEY_O,
    SCREEN_KEY_P, SCREEN_KEY_Q, SCREEN_KEY_R, SCREEN_KEY_S, SCREEN_KEY_T,
    SCREEN_KEY_U, SCREEN_KEY_V, SCREEN_KEY_W, SCREEN_KEY_X, SCREEN_KEY_Y,
    SCREEN_KEY_Z,

    SCREEN_KEY_META = 91,   // Windows key / Super / Command

    SCREEN_KEY_NUMPAD_0 = 96, SCREEN_KEY_NUMPAD_1, SCREEN_KEY_NUMPAD_2,
    SCREEN_KEY_NUMPAD_3, SCREEN_KEY_NUMPAD_4, SCREEN_KEY_NUMPAD_5,
    SCREEN_KEY_NUMPAD_6, SCREEN_KEY_NUMPAD_7, SCREEN_KEY_NUMPAD_8,
    SCREEN_KEY_NUMPAD_9,
    SCREEN_KEY_NUMPAD_MULTIPLY = 106,
    SCREEN_KEY_NUMPAD_ADD      = 107,
    SCREEN_KEY_NUMPAD_SUBTRACT = 109,
    SCREEN_KEY_NUMPAD_DECIMAL  = 110,
    SCREEN_KEY_NUMPAD_DIVIDE   = 111,

    SCREEN_KEY_F1 = 112, SCREEN_KEY_F2, SCREEN_KEY_F3, SCREEN_KEY_F4,
    SCREEN_KEY_F5, SCREEN_KEY_F6, SCREEN_KEY_F7, SCREEN_KEY_F8,
    SCREEN_KEY_F9, SCREEN_KEY_F10, SCREEN_KEY_F11, SCREEN_KEY_F12,

    SCREEN_KEY_NUM_LOCK    = 144,
    SCREEN_KEY_SCROLL_LOCK = 145,

    SCREEN_KEY_SEMICOLON     = 186,   // ;
    SCREEN_KEY_EQUAL         = 187,   // =
    SCREEN_KEY_COMMA         = 188,   // ,
    SCREEN_KEY_MINUS         = 189,   // -
    SCREEN_KEY_PERIOD        = 190,   // .
    SCREEN_KEY_SLASH         = 191,   // /
    SCREEN_KEY_GRAVE         = 192,   // `
    SCREEN_KEY_LEFT_BRACKET  = 219,   // [
    SCREEN_KEY_BACKSLASH     = 220,   // backslash
    SCREEN_KEY_RIGHT_BRACKET = 221,   // ]
    SCREEN_KEY_APOSTROPHE    = 222    // '
};

enum : int { SCREEN_KEY_COUNT = 256 };   // size of the pressed-state table

static_assert(SCREEN_KEY_9 == 57 && SCREEN_KEY_Z == 90 &&
              SCREEN_KEY_NUMPAD_9 == 105 && SCREEN_KEY_F12 == 123,
              "ScreenKey ranges must stay contiguous");

#ifdef __APPLE__
static void releaseAllPressedKeys();
struct AppScreen;
static void applyMacStyle(AppScreen* s);
@class ScreenWindowDelegate;
#endif

struct AppScreen
{
    int width = 0;
    int height = 0;

    bool running = false;

    int mouseX = 0;
    int mouseY = 0;

    // RGBA, 4 bytes per pixel, top-down.  Alpha is always 255 (opaque window).
    uint8_t* pixels = nullptr;

    uint8_t colorR = 255;
    uint8_t colorG = 255;
    uint8_t colorB = 255;
    uint8_t colorA = 255;

    char* title = nullptr;      // UTF-8 copy owned by the screen
    bool resizable = true;
    bool fullscreen = false;

#ifdef _WIN32

    HWND hwnd = nullptr;
    HDC dc = nullptr;
    WINDOWPLACEMENT savedPlacement{};   // windowed placement, for leaving fullscreen

#elif defined(__APPLE__)

    NSWindow* window = nil;
    ScreenWindowDelegate* delegate = nil;
    AppScreen* nextScreen = nullptr;   // registry of live screens

#elif defined(__linux__)

    Display* display = nullptr;
    Window window = 0;
    GC gc = nullptr;
    Atom wmDelete = None;
    XImage* ximage = nullptr;          // cached presentation image
    int savedWidth = 0;                // windowed size, for leaving fullscreen
    int savedHeight = 0;

#endif

    // No user-declared destructor: it would be implicitly noexcept and, with
    // exceptions enabled, force a C++ personality reference around std::free.
    // (Under ARC the implicit destructor just releases the Cocoa objects.)
};

// AppScreen is allocated with calloc + placement new so the library needs no
// C++ runtime (operator new/delete).
static AppScreen* allocScreenObject()
{
    void* memory = std::calloc(1, sizeof(AppScreen));

    if (!memory)
        return nullptr;

    return new (memory) AppScreen();
}

static void freeScreenObject(AppScreen* s)
{
    if (!s)
        return;

    std::free(s->pixels);
    std::free(s->title);
    s->~AppScreen();
    std::free(s);
}

// Stores a private copy of the title (null becomes an empty title).
static bool storeTitle(AppScreen* s, const char* text)
{
    if (!text)
        text = "";

    const size_t len = std::strlen(text);
    char* copy = static_cast<char*>(std::malloc(len + 1));

    if (!copy)
        return false;

    std::memcpy(copy, text, len + 1);

    std::free(s->title);
    s->title = copy;
    return true;
}

#ifdef __APPLE__

// Every live screen, so one poll can route events for all windows.
static AppScreen* g_screenList = nullptr;

static void addScreenToList(AppScreen* s)
{
    s->nextScreen = g_screenList;
    g_screenList = s;
}

static void removeScreenFromList(AppScreen* s)
{
    for (AppScreen** link = &g_screenList; *link; link = &(*link)->nextScreen)
    {
        if (*link == s)
        {
            *link = s->nextScreen;
            s->nextScreen = nullptr;
            return;
        }
    }
}

static AppScreen* findScreenByWindow(NSWindow* window)
{
    if (!window)
        return nullptr;

    for (AppScreen* p = g_screenList; p; p = p->nextScreen)
    {
        if (p->window == window)
            return p;
    }

    return nullptr;
}

#endif


static bool resizeFramebuffer(AppScreen* s, int width, int height)
{
    if (!s || width <= 0 || height <= 0)
        return false;

    if (s->pixels &&
        s->width == width &&
        s->height == height)
    {
        return true;
    }

    const size_t w = static_cast<size_t>(width);
    const size_t h = static_cast<size_t>(height);

    const size_t maxSize = std::numeric_limits<size_t>::max();

    if (w > maxSize / 4 / h)
        return false;

    const size_t bytes = w * h * 4;

    uint8_t* newPixels = static_cast<uint8_t*>(std::malloc(bytes));

    if (!newPixels)
        return false;

    std::memset(newPixels, 0, bytes);

    // Initialize every alpha channel as fully opaque.
    for (size_t i = 3; i < bytes; i += 4)
        newPixels[i] = 255;

    // Keep whatever part of the old image still fits.
    if (s->pixels && s->width > 0 && s->height > 0)
    {
        const size_t copyW = static_cast<size_t>(
            s->width < width ? s->width : width);
        const size_t copyH = static_cast<size_t>(
            s->height < height ? s->height : height);
        const size_t oldW = static_cast<size_t>(s->width);

        for (size_t row = 0; row < copyH; ++row)
        {
            std::memcpy(
                newPixels + row * w * 4,
                s->pixels + row * oldW * 4,
                copyW * 4
            );
        }
    }

    std::free(s->pixels);

    s->pixels = newPixels;
    s->width = width;
    s->height = height;

    return true;
}

// Writes the current draw color at a byte offset into the framebuffer.
static inline void writeColorAt(AppScreen* s, std::size_t index)
{
    uint8_t* p = s->pixels + index;

#if SCREEN_ALPHA_BLEND
    const unsigned a = s->colorA;

    if (a != 255)
    {
        if (a == 0)
            return;

        const unsigned ia = 255u - a;

        p[0] = static_cast<uint8_t>((s->colorR * a + p[0] * ia + 127u) / 255u);
        p[1] = static_cast<uint8_t>((s->colorG * a + p[1] * ia + 127u) / 255u);
        p[2] = static_cast<uint8_t>((s->colorB * a + p[2] * ia + 127u) / 255u);
        p[3] = 255;
        return;
    }

    p[0] = s->colorR;
    p[1] = s->colorG;
    p[2] = s->colorB;
    p[3] = 255;
#else
    p[0] = s->colorR;
    p[1] = s->colorG;
    p[2] = s->colorB;
    p[3] = s->colorA;
#endif
}

#ifdef __APPLE__

@interface ScreenWindowDelegate : NSObject <NSWindowDelegate>
{
@public
    AppScreen* owner;
}
@end

@implementation ScreenWindowDelegate

- (void)windowWillClose:(NSNotification*)notification
{
    (void)notification;

    if (owner)
        owner->running = false;
}

- (void)windowDidResignKey:(NSNotification*)notification
{
    (void)notification;
    releaseAllPressedKeys();
}

// Fullscreen can also be entered / left by the user (green button).
- (void)windowWillEnterFullScreen:(NSNotification*)notification
{
    (void)notification;

    if (owner)
        owner->fullscreen = true;
}

- (void)windowWillExitFullScreen:(NSNotification*)notification
{
    (void)notification;

    if (owner)
        owner->fullscreen = false;
}

- (void)windowDidExitFullScreen:(NSNotification*)notification
{
    (void)notification;

    // Re-apply the resizable flag that fullscreen had to override.
    if (owner)
        applyMacStyle(owner);
}

@end

@interface ScreenView : NSView
{
@public
    AppScreen* owner;
}
@end

@implementation ScreenView
- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (BOOL)isOpaque
{
    return YES;
}

// Key events are consumed by pollScreenEvents. Swallowing them here stops
// AppKit from beeping because nothing in the responder chain handled them.
- (void)keyDown:(NSEvent*)event
{
    (void)event;
}

- (void)keyUp:(NSEvent*)event
{
    (void)event;
}

- (void)drawRect:(NSRect)dirtyRect
{
    (void)dirtyRect;

    AppScreen* s = owner;

    if (!s || !s->pixels)
        return;

    NSRect bounds = [self bounds];

    int width = static_cast<int>(NSWidth(bounds));
    int height = static_cast<int>(NSHeight(bounds));

    if (width <= 0 || height <= 0)
        return;

    if (width != s->width || height != s->height)
        resizeFramebuffer(s, width, height);

    if (!s->pixels)
        return;

    size_t bytes =
        static_cast<size_t>(s->width) *
        static_cast<size_t>(s->height) * 4;

    CGDataProviderRef provider = CGDataProviderCreateWithData(
        nullptr,
        s->pixels,
        bytes,
        nullptr
    );

    if (!provider)
        return;

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();

    if (!colorSpace)
    {
        CGDataProviderRelease(provider);
        return;
    }

    CGImageRef image = CGImageCreate(
        s->width,
        s->height,
        8,
        32,
        s->width * 4,
        colorSpace,
        kCGBitmapByteOrder32Big | kCGImageAlphaNoneSkipLast,
        provider,
        nullptr,
        false,
        kCGRenderingIntentDefault
    );

    if (image)
    {
        CGContextRef context =
            [[NSGraphicsContext currentContext] CGContext];

        if (context)
        {
            CGContextSaveGState(context);

            // The view is not flipped, so CGContextDrawImage already draws the
            // top-down framebuffer upright. (No flip transform needed.)
            CGContextDrawImage(
                context,
                CGRectMake(
                    0,
                    0,
                    NSWidth(bounds),
                    NSHeight(bounds)
                ),
                image
            );

            CGContextRestoreGState(context);
        }

        CGImageRelease(image);
    }

    CGColorSpaceRelease(colorSpace);
    CGDataProviderRelease(provider);
}

@end

#endif


using MouseMoveCallback = void(*)(int, int);
using KeyCallback = void(*)(int, bool);

static MouseMoveCallback g_mouseMoveCallback = nullptr;
static KeyCallback g_keyCallback = nullptr;

// The mouse getters do not take a screen argument, so they expose the most
// recently reported cursor position from any managed screen.
static int g_mouseX = 0;
static int g_mouseY = 0;

// Pressed state indexed by ScreenKey (keys and mouse buttons).
static bool g_keyDown[SCREEN_KEY_COUNT] = {};

// Updates the pressed table and notifies the callback, but only when the
// state actually changes: auto-repeat and duplicate events are filtered here.
static void dispatchKeyEvent(int keyCode, bool pressed)
{
    if (keyCode <= 0 || keyCode >= SCREEN_KEY_COUNT)
        return;

    if (g_keyDown[keyCode] == pressed)
        return;

    g_keyDown[keyCode] = pressed;

    if (g_keyCallback)
        g_keyCallback(keyCode, pressed);
}

static void releaseAllPressedKeys()
{
    for (int k = 1; k < SCREEN_KEY_COUNT; ++k)
    {
        if (g_keyDown[k])
            dispatchKeyEvent(k, false);
    }
}

#ifdef __APPLE__

static bool isModifierKey(int k)
{
    return k == SCREEN_KEY_SHIFT || k == SCREEN_KEY_CTRL ||
           k == SCREEN_KEY_ALT   || k == SCREEN_KEY_META ||
           k == SCREEN_KEY_CAPS_LOCK;
}

static bool isMouseButtonCode(int k)
{
    return k == SCREEN_MOUSE_LEFT || k == SCREEN_MOUSE_RIGHT ||
           k == SCREEN_MOUSE_MIDDLE;
}

// macOS never delivers key-up events for keys released while Command is held.
static void releaseAllNonModifierKeys()
{
    for (int k = 1; k < SCREEN_KEY_COUNT; ++k)
    {
        if (g_keyDown[k] && !isModifierKey(k) && !isMouseButtonCode(k))
            dispatchKeyEvent(k, false);
    }
}

#endif

extern "C"
int getMouseX()
{
    return g_mouseX;
}

extern "C"
int getMouseY()
{
    return g_mouseY;
}

extern "C"
bool isKeyPressed(int keyCode)
{
    return keyCode > 0 &&
           keyCode < SCREEN_KEY_COUNT &&
           g_keyDown[keyCode];
}

extern "C"
void registerMouseMoveCallback(MouseMoveCallback callback)
{
    g_mouseMoveCallback = callback;
}

extern "C"
void registerKeyCallback(KeyCallback callback)
{
    g_keyCallback = callback;
}




// ============================================================
// WINDOWS
// ============================================================

#ifdef _WIN32

// Returns a malloc'd wide string (free with std::free), or nullptr.
static wchar_t* utf8ToWide(const char* text)
{
    const int len = MultiByteToWideChar(CP_UTF8, 0, text, -1, nullptr, 0);

    if (len <= 0)
        return nullptr;

    wchar_t* out = static_cast<wchar_t*>(
        std::malloc(sizeof(wchar_t) * static_cast<size_t>(len))
    );

    if (!out)
        return nullptr;

    if (MultiByteToWideChar(CP_UTF8, 0, text, -1, out, len) <= 0)
    {
        std::free(out);
        return nullptr;
    }

    return out;
}

static bool isPortableKeyCode(int c)
{
    return c == 8 || c == 9 || c == 13 ||
           (c >= 16 && c <= 20) || c == 27 || c == 32 ||
           (c >= 33 && c <= 40) || c == 45 || c == 46 ||
           (c >= 48 && c <= 57) || (c >= 65 && c <= 90) ||
           c == 91 ||
           (c >= 96 && c <= 107) || (c >= 109 && c <= 123) ||
           c == 144 || c == 145 ||
           (c >= 186 && c <= 192) || (c >= 219 && c <= 222);
}

static int translateWindowsKey(WPARAM vk)
{
    switch (vk)
    {
        case VK_LWIN:
        case VK_RWIN:     return SCREEN_KEY_META;
        case VK_LSHIFT:
        case VK_RSHIFT:   return SCREEN_KEY_SHIFT;
        case VK_LCONTROL:
        case VK_RCONTROL: return SCREEN_KEY_CTRL;
        case VK_LMENU:
        case VK_RMENU:    return SCREEN_KEY_ALT;
        default:          break;
    }

    if (vk >= 256)
        return SCREEN_KEY_NONE;

    return isPortableKeyCode(static_cast<int>(vk))
        ? static_cast<int>(vk)
        : SCREEN_KEY_NONE;
}


struct ScreenDibInfo
{
    BITMAPINFOHEADER header;
    DWORD masks[3];
};

static void drawScreenToDC(AppScreen* s, HDC dc)
{
    if (!s || !s->pixels || !dc)
        return;

    ScreenDibInfo info{};

    info.header.biSize = sizeof(BITMAPINFOHEADER);
    info.header.biWidth = s->width;
    info.header.biHeight = -s->height;
    info.header.biPlanes = 1;
    info.header.biBitCount = 32;
    info.header.biCompression = BI_BITFIELDS;

    // Interpret the memory as RGBA.
    info.masks[0] = 0x000000FF; // Red
    info.masks[1] = 0x0000FF00; // Green
    info.masks[2] = 0x00FF0000; // Blue

    SetStretchBltMode(dc, COLORONCOLOR);

    StretchDIBits(
        dc,
        0, 0,
        s->width, s->height,
        0, 0,
        s->width, s->height,
        s->pixels,
        reinterpret_cast<BITMAPINFO*>(&info),
        DIB_RGB_COLORS,
        SRCCOPY
    );
}

static LRESULT CALLBACK screenWindowProc(
    HWND hwnd,
    UINT msg,
    WPARAM wParam,
    LPARAM lParam)
{
    AppScreen* s = reinterpret_cast<AppScreen*>(
        GetWindowLongPtrW(hwnd, GWLP_USERDATA)
    );

    switch (msg)
    {
        case WM_NCCREATE:
        {
            auto* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            s = static_cast<AppScreen*>(cs->lpCreateParams);

            SetWindowLongPtrW(
                hwnd,
                GWLP_USERDATA,
                reinterpret_cast<LONG_PTR>(s)
            );

            return DefWindowProcW(hwnd, msg, wParam, lParam);
        }

        case WM_NCDESTROY:
            // The AppScreen may be freed right after the window is gone.
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, 0);
            return DefWindowProcW(hwnd, msg, wParam, lParam);

        case WM_CLOSE:
            DestroyWindow(hwnd);
            return 0;

        case WM_DESTROY:
            if (s)
            {
                s->running = false;
                s->hwnd = nullptr;   // never keep a handle that may be reused
            }
            return 0;

        case WM_KILLFOCUS:
            releaseAllPressedKeys();
            return 0;

        case WM_SIZE:
            if (s)
            {
                int width = LOWORD(lParam);
                int height = HIWORD(lParam);

                if (width > 0 && height > 0)
                    resizeFramebuffer(s, width, height);
            }
            return 0;

        case WM_MOUSEMOVE:
            if (s)
            {
                s->mouseX = GET_X_LPARAM(lParam);
                s->mouseY = GET_Y_LPARAM(lParam);
                g_mouseX = s->mouseX;
                g_mouseY = s->mouseY;

                if (g_mouseMoveCallback)
                {
                    g_mouseMoveCallback(
                        s->mouseX,
                        s->mouseY
                    );
                }
            }
            return 0;

        case WM_KEYDOWN:
        case WM_KEYUP:
        case WM_SYSKEYDOWN:
        case WM_SYSKEYUP:
        {
            const bool pressed =
                (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN);

            const int code = translateWindowsKey(wParam);

            // Auto-repeat is filtered inside dispatchKeyEvent.
            if (code != SCREEN_KEY_NONE)
                dispatchKeyEvent(code, pressed);

            if (msg == WM_SYSKEYDOWN || msg == WM_SYSKEYUP)
                return DefWindowProcW(hwnd, msg, wParam, lParam);

            return 0;
        }

        case WM_LBUTTONDOWN:
        case WM_LBUTTONUP:
        case WM_RBUTTONDOWN:
        case WM_RBUTTONUP:
        case WM_MBUTTONDOWN:
        case WM_MBUTTONUP:
        {
            int button = SCREEN_MOUSE_LEFT;
            bool pressed = false;

            switch (msg)
            {
                case WM_LBUTTONDOWN: button = SCREEN_MOUSE_LEFT;   pressed = true;  break;
                case WM_LBUTTONUP:   button = SCREEN_MOUSE_LEFT;   pressed = false; break;
                case WM_RBUTTONDOWN: button = SCREEN_MOUSE_RIGHT;  pressed = true;  break;
                case WM_RBUTTONUP:   button = SCREEN_MOUSE_RIGHT;  pressed = false; break;
                case WM_MBUTTONDOWN: button = SCREEN_MOUSE_MIDDLE; pressed = true;  break;
                default:             button = SCREEN_MOUSE_MIDDLE; pressed = false; break;
            }

            dispatchKeyEvent(button, pressed);

            // Capture the mouse while any button is held so a release outside
            // the client area is still delivered (otherwise the button sticks).
            if (wParam & (MK_LBUTTON | MK_RBUTTON | MK_MBUTTON))
            {
                if (GetCapture() != hwnd)
                    SetCapture(hwnd);
            }
            else if (GetCapture() == hwnd)
            {
                ReleaseCapture();
            }

            return 0;
        }

        case WM_PAINT:
        {
            PAINTSTRUCT ps{};
            HDC dc = BeginPaint(hwnd, &ps);

            if (s)
                drawScreenToDC(s, dc);

            EndPaint(hwnd, &ps);
            return 0;
        }

        case WM_ERASEBKGND:
            return 1;
    }

    return DefWindowProcW(hwnd, msg, wParam, lParam);
}



static bool registerScreenClass()
{
    static bool registered = false;

    if (registered)
        return true;

    WNDCLASSEXW wc{};
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = screenWindowProc;
    wc.hInstance = GetModuleHandleW(nullptr);
    wc.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    wc.lpszClassName = L"NativeScreenWindow";

    if (!RegisterClassExW(&wc))
    {
        if (GetLastError() != ERROR_CLASS_ALREADY_EXISTS)
            return false;
    }

    registered = true;
    return true;
}

#endif

// ============================================================
// LINUX
// ============================================================

#ifdef __linux__

// Maps an X11 keysym (index 0, i.e. ignoring modifiers) to a ScreenKey.
static int translateKeysym(KeySym k)
{
    if (k >= XK_a && k <= XK_z)
        return SCREEN_KEY_A + static_cast<int>(k - XK_a);

    if (k >= XK_A && k <= XK_Z)
        return SCREEN_KEY_A + static_cast<int>(k - XK_A);

    if (k >= XK_0 && k <= XK_9)
        return SCREEN_KEY_0 + static_cast<int>(k - XK_0);

    if (k >= XK_F1 && k <= XK_F12)
        return SCREEN_KEY_F1 + static_cast<int>(k - XK_F1);

    if (k >= XK_KP_0 && k <= XK_KP_9)
        return SCREEN_KEY_NUMPAD_0 + static_cast<int>(k - XK_KP_0);

    switch (k)
    {
        case XK_space:        return SCREEN_KEY_SPACE;
        case XK_BackSpace:    return SCREEN_KEY_BACKSPACE;
        case XK_Tab:
        case XK_ISO_Left_Tab: return SCREEN_KEY_TAB;
        case XK_Return:
        case XK_KP_Enter:     return SCREEN_KEY_ENTER;
        case XK_Escape:       return SCREEN_KEY_ESCAPE;
        case XK_Pause:        return SCREEN_KEY_PAUSE;
        case XK_Scroll_Lock:  return SCREEN_KEY_SCROLL_LOCK;
        case XK_Num_Lock:     return SCREEN_KEY_NUM_LOCK;
        case XK_Caps_Lock:
        case XK_Shift_Lock:   return SCREEN_KEY_CAPS_LOCK;

        case XK_Home:         return SCREEN_KEY_HOME;
        case XK_End:          return SCREEN_KEY_END;
        case XK_Left:         return SCREEN_KEY_LEFT;
        case XK_Up:           return SCREEN_KEY_UP;
        case XK_Right:        return SCREEN_KEY_RIGHT;
        case XK_Down:         return SCREEN_KEY_DOWN;
        case XK_Page_Up:      return SCREEN_KEY_PAGE_UP;
        case XK_Page_Down:    return SCREEN_KEY_PAGE_DOWN;
        case XK_Insert:       return SCREEN_KEY_INSERT;
        case XK_Delete:       return SCREEN_KEY_DELETE;

        case XK_Shift_L:
        case XK_Shift_R:      return SCREEN_KEY_SHIFT;
        case XK_Control_L:
        case XK_Control_R:    return SCREEN_KEY_CTRL;
        case XK_Alt_L:
        case XK_Alt_R:
        case XK_Meta_L:
        case XK_Meta_R:
        case XK_ISO_Level3_Shift: return SCREEN_KEY_ALT;
        case XK_Super_L:
        case XK_Super_R:      return SCREEN_KEY_META;

        // With NumLock off (and for index 0 lookups) the keypad reports its
        // navigation keysyms, so map those back to the keypad keys.
        case XK_KP_Insert:    return SCREEN_KEY_NUMPAD_0;
        case XK_KP_End:       return SCREEN_KEY_NUMPAD_1;
        case XK_KP_Down:      return SCREEN_KEY_NUMPAD_2;
        case XK_KP_Page_Down: return SCREEN_KEY_NUMPAD_3;
        case XK_KP_Left:      return SCREEN_KEY_NUMPAD_4;
        case XK_KP_Begin:     return SCREEN_KEY_NUMPAD_5;
        case XK_KP_Right:     return SCREEN_KEY_NUMPAD_6;
        case XK_KP_Home:      return SCREEN_KEY_NUMPAD_7;
        case XK_KP_Up:        return SCREEN_KEY_NUMPAD_8;
        case XK_KP_Page_Up:   return SCREEN_KEY_NUMPAD_9;
        case XK_KP_Delete:
        case XK_KP_Decimal:   return SCREEN_KEY_NUMPAD_DECIMAL;
        case XK_KP_Multiply:  return SCREEN_KEY_NUMPAD_MULTIPLY;
        case XK_KP_Add:       return SCREEN_KEY_NUMPAD_ADD;
        case XK_KP_Subtract:  return SCREEN_KEY_NUMPAD_SUBTRACT;
        case XK_KP_Divide:    return SCREEN_KEY_NUMPAD_DIVIDE;

        case XK_semicolon:    return SCREEN_KEY_SEMICOLON;
        case XK_equal:        return SCREEN_KEY_EQUAL;
        case XK_comma:        return SCREEN_KEY_COMMA;
        case XK_minus:        return SCREEN_KEY_MINUS;
        case XK_period:       return SCREEN_KEY_PERIOD;
        case XK_slash:        return SCREEN_KEY_SLASH;
        case XK_grave:        return SCREEN_KEY_GRAVE;
        case XK_bracketleft:  return SCREEN_KEY_LEFT_BRACKET;
        case XK_backslash:    return SCREEN_KEY_BACKSLASH;
        case XK_bracketright: return SCREEN_KEY_RIGHT_BRACKET;
        case XK_apostrophe:   return SCREEN_KEY_APOSTROPHE;

        default:              return SCREEN_KEY_NONE;
    }
}

static int translateXButton(unsigned int button)
{
    switch (button)
    {
        case Button1: return SCREEN_MOUSE_LEFT;
        case Button2: return SCREEN_MOUSE_MIDDLE;
        case Button3: return SCREEN_MOUSE_RIGHT;
        default:      return SCREEN_KEY_NONE;   // wheel buttons (4-7), etc.
    }
}
static unsigned long channelToMask(
    uint8_t value,
    unsigned long mask)
{
    if (!mask)
        return 0;

    unsigned int shift = 0;

    while (((mask >> shift) & 1UL) == 0)
        ++shift;

    unsigned long maxValue = mask >> shift;

    return (
        ((static_cast<unsigned long>(value) * maxValue + 127) / 255)
        << shift
    ) & mask;
}
// Finds the bit position of an 8-bit channel mask; false if the mask is not
// a plain 8-bit field.
static bool x11ChannelShift(unsigned long mask, unsigned int& shift)
{
    if (!mask)
        return false;

    shift = 0;

    while (((mask >> shift) & 1UL) == 0)
        ++shift;

    return (mask >> shift) == 0xFFUL;
}

static bool hostIsLittleEndian()
{
    const uint16_t one = 1;
    uint8_t first = 0;
    std::memcpy(&first, &one, 1);
    return first == 1;
}

// Keeps one XImage per screen instead of rebuilding it every frame.
static bool ensureX11Image(AppScreen* s)
{
    if (s->ximage &&
        s->ximage->width == s->width &&
        s->ximage->height == s->height)
    {
        return true;
    }

    if (s->ximage)
    {
        XDestroyImage(s->ximage);
        s->ximage = nullptr;
    }

    const int screen = DefaultScreen(s->display);

    XImage* image = XCreateImage(
        s->display,
        DefaultVisual(s->display, screen),
        static_cast<unsigned int>(DefaultDepth(s->display, screen)),
        ZPixmap,
        0,
        nullptr,
        static_cast<unsigned int>(s->width),
        static_cast<unsigned int>(s->height),
        32,
        0
    );

    if (!image)
        return false;

    image->data = static_cast<char*>(
        std::calloc(
            static_cast<size_t>(s->height),
            static_cast<size_t>(image->bytes_per_line)
        )
    );

    if (!image->data)
    {
        XDestroyImage(image);
        return false;
    }

    s->ximage = image;
    return true;
}

static void drawScreenToX11(AppScreen* s)
{
    if (!s || !s->display || !s->window ||
        !s->gc || !s->pixels)
        return;

    if (!ensureX11Image(s))
        return;

    XImage* image = s->ximage;

    bool fast = false;
    unsigned int rShift = 0, gShift = 0, bShift = 0;

#ifndef SCREEN_X11_FORCE_SLOW
    // Fast path: 32 bits per pixel, plain 8-bit channels, host byte order.
    fast =
        image->bits_per_pixel == 32 &&
        (image->byte_order == LSBFirst) == hostIsLittleEndian() &&
        x11ChannelShift(image->red_mask, rShift) &&
        x11ChannelShift(image->green_mask, gShift) &&
        x11ChannelShift(image->blue_mask, bShift);
#endif

    if (fast)
    {
        for (int y = 0; y < s->height; ++y)
        {
            uint32_t* dst = reinterpret_cast<uint32_t*>(
                image->data +
                static_cast<size_t>(y) *
                static_cast<size_t>(image->bytes_per_line)
            );

            const uint8_t* src =
                s->pixels +
                static_cast<size_t>(y) * static_cast<size_t>(s->width) * 4;

            for (int x = 0; x < s->width; ++x, src += 4)
            {
                dst[x] =
                    (static_cast<uint32_t>(src[0]) << rShift) |
                    (static_cast<uint32_t>(src[1]) << gShift) |
                    (static_cast<uint32_t>(src[2]) << bShift);
            }
        }
    }
    else
    {
        // Generic path for unusual visuals.
        for (int y = 0; y < s->height; ++y)
        {
            for (int x = 0; x < s->width; ++x)
            {
                size_t index =
                    (static_cast<size_t>(y) * s->width + x) * 4;

                uint8_t r = s->pixels[index];
                uint8_t g = s->pixels[index + 1];
                uint8_t b = s->pixels[index + 2];

                unsigned long pixel =
                    channelToMask(r, image->red_mask) |
                    channelToMask(g, image->green_mask) |
                    channelToMask(b, image->blue_mask);

                XPutPixel(image, x, y, pixel);
            }
        }
    }

    XPutImage(
        s->display,
        s->window,
        s->gc,
        image,
        0, 0,
        0, 0,
        static_cast<unsigned int>(s->width),
        static_cast<unsigned int>(s->height)
    );

    XFlush(s->display);
}

// Sets the window title (legacy Latin-1 name plus UTF-8 _NET_WM_NAME).
static void applyX11Title(AppScreen* s)
{
    if (!s->display || !s->window || !s->title)
        return;

    XStoreName(s->display, s->window, s->title);

    XChangeProperty(
        s->display,
        s->window,
        XInternAtom(s->display, "_NET_WM_NAME", False),
        XInternAtom(s->display, "UTF8_STRING", False),
        8,
        PropModeReplace,
        reinterpret_cast<const unsigned char*>(s->title),
        static_cast<int>(std::strlen(s->title))
    );
}

// A fixed-size window is expressed to the window manager as min == max.
// No hints are set while resizable or fullscreen.
static void applyX11SizeHints(AppScreen* s, int width, int height)
{
    XSizeHints hints{};

    if (!s->resizable && !s->fullscreen && width > 0 && height > 0)
    {
        hints.flags = PMinSize | PMaxSize;
        hints.min_width = hints.max_width = width;
        hints.min_height = hints.max_height = height;
    }

    XSetWMNormalHints(s->display, s->window, &hints);
}

#endif
// ============================================================
// MACOS
// ============================================================

#ifdef __APPLE__

// Physical key (kVK_*) to ScreenKey. Positions follow the US ANSI layout.
static int translateMacKeyCode(unsigned short keyCode)
{
    switch (keyCode)
    {
        case 0:   return SCREEN_KEY_A;
        case 11:  return SCREEN_KEY_B;
        case 8:   return SCREEN_KEY_C;
        case 2:   return SCREEN_KEY_D;
        case 14:  return SCREEN_KEY_E;
        case 3:   return SCREEN_KEY_F;
        case 5:   return SCREEN_KEY_G;
        case 4:   return SCREEN_KEY_H;
        case 34:  return SCREEN_KEY_I;
        case 38:  return SCREEN_KEY_J;
        case 40:  return SCREEN_KEY_K;
        case 37:  return SCREEN_KEY_L;
        case 46:  return SCREEN_KEY_M;
        case 45:  return SCREEN_KEY_N;
        case 31:  return SCREEN_KEY_O;
        case 35:  return SCREEN_KEY_P;
        case 12:  return SCREEN_KEY_Q;
        case 15:  return SCREEN_KEY_R;
        case 1:   return SCREEN_KEY_S;
        case 17:  return SCREEN_KEY_T;
        case 32:  return SCREEN_KEY_U;
        case 9:   return SCREEN_KEY_V;
        case 13:  return SCREEN_KEY_W;
        case 7:   return SCREEN_KEY_X;
        case 16:  return SCREEN_KEY_Y;
        case 6:   return SCREEN_KEY_Z;

        case 29:  return SCREEN_KEY_0;
        case 18:  return SCREEN_KEY_1;
        case 19:  return SCREEN_KEY_2;
        case 20:  return SCREEN_KEY_3;
        case 21:  return SCREEN_KEY_4;
        case 23:  return SCREEN_KEY_5;
        case 22:  return SCREEN_KEY_6;
        case 26:  return SCREEN_KEY_7;
        case 28:  return SCREEN_KEY_8;
        case 25:  return SCREEN_KEY_9;

        case 24:  return SCREEN_KEY_EQUAL;
        case 27:  return SCREEN_KEY_MINUS;
        case 30:  return SCREEN_KEY_RIGHT_BRACKET;
        case 33:  return SCREEN_KEY_LEFT_BRACKET;
        case 39:  return SCREEN_KEY_APOSTROPHE;
        case 41:  return SCREEN_KEY_SEMICOLON;
        case 42:  return SCREEN_KEY_BACKSLASH;
        case 43:  return SCREEN_KEY_COMMA;
        case 44:  return SCREEN_KEY_SLASH;
        case 47:  return SCREEN_KEY_PERIOD;
        case 50:  return SCREEN_KEY_GRAVE;

        case 36:
        case 76:  return SCREEN_KEY_ENTER;      // Return, keypad Enter
        case 48:  return SCREEN_KEY_TAB;
        case 49:  return SCREEN_KEY_SPACE;
        case 51:  return SCREEN_KEY_BACKSPACE;
        case 53:  return SCREEN_KEY_ESCAPE;

        case 117: return SCREEN_KEY_DELETE;     // forward delete
        case 114: return SCREEN_KEY_INSERT;     // Help key
        case 115: return SCREEN_KEY_HOME;
        case 119: return SCREEN_KEY_END;
        case 116: return SCREEN_KEY_PAGE_UP;
        case 121: return SCREEN_KEY_PAGE_DOWN;
        case 123: return SCREEN_KEY_LEFT;
        case 124: return SCREEN_KEY_RIGHT;
        case 125: return SCREEN_KEY_DOWN;
        case 126: return SCREEN_KEY_UP;

        case 122: return SCREEN_KEY_F1;
        case 120: return SCREEN_KEY_F2;
        case 99:  return SCREEN_KEY_F3;
        case 118: return SCREEN_KEY_F4;
        case 96:  return SCREEN_KEY_F5;
        case 97:  return SCREEN_KEY_F6;
        case 98:  return SCREEN_KEY_F7;
        case 100: return SCREEN_KEY_F8;
        case 101: return SCREEN_KEY_F9;
        case 109: return SCREEN_KEY_F10;
        case 103: return SCREEN_KEY_F11;
        case 111: return SCREEN_KEY_F12;

        case 82:  return SCREEN_KEY_NUMPAD_0;
        case 83:  return SCREEN_KEY_NUMPAD_1;
        case 84:  return SCREEN_KEY_NUMPAD_2;
        case 85:  return SCREEN_KEY_NUMPAD_3;
        case 86:  return SCREEN_KEY_NUMPAD_4;
        case 87:  return SCREEN_KEY_NUMPAD_5;
        case 88:  return SCREEN_KEY_NUMPAD_6;
        case 89:  return SCREEN_KEY_NUMPAD_7;
        case 91:  return SCREEN_KEY_NUMPAD_8;
        case 92:  return SCREEN_KEY_NUMPAD_9;
        case 67:  return SCREEN_KEY_NUMPAD_MULTIPLY;
        case 69:  return SCREEN_KEY_NUMPAD_ADD;
        case 78:  return SCREEN_KEY_NUMPAD_SUBTRACT;
        case 65:  return SCREEN_KEY_NUMPAD_DECIMAL;
        case 75:  return SCREEN_KEY_NUMPAD_DIVIDE;
        case 71:  return SCREEN_KEY_NUM_LOCK;   // keypad Clear

        default:  return SCREEN_KEY_NONE;
    }
}

// Routes one NSEvent to the screen that owns its window.
static void handleCocoaEvent(AppScreen* s, NSEvent* event)
{
    NSView* content = [s->window contentView];

    const NSPoint p = [content convertPoint:[event locationInWindow]
                                   fromView:nil];
    const NSRect bounds = [content bounds];

    // Clicks on the title bar / window frame are handled by AppKit in a
    // tracking loop that swallows the matching mouse-up, so only count
    // presses inside the content area.
    const bool inside = NSPointInRect(p, bounds);

    switch ([event type])
    {
        case NSEventTypeMouseMoved:
        case NSEventTypeLeftMouseDragged:
        case NSEventTypeRightMouseDragged:
        case NSEventTypeOtherMouseDragged:
        {
            if ([event type] == NSEventTypeMouseMoved)
            {
                // A plain move means no button is down: heal any stuck state.
                const NSUInteger buttons = [NSEvent pressedMouseButtons];

                if (!(buttons & 1)) dispatchKeyEvent(SCREEN_MOUSE_LEFT, false);
                if (!(buttons & 2)) dispatchKeyEvent(SCREEN_MOUSE_RIGHT, false);
                if (!(buttons & 4)) dispatchKeyEvent(SCREEN_MOUSE_MIDDLE, false);
            }

            s->mouseX = static_cast<int>(p.x);
            s->mouseY = static_cast<int>(NSHeight(bounds) - p.y);
            g_mouseX = s->mouseX;
            g_mouseY = s->mouseY;

            if (g_mouseMoveCallback)
                g_mouseMoveCallback(s->mouseX, s->mouseY);

            break;
        }

        case NSEventTypeKeyDown:
        {
            const int code = translateMacKeyCode([event keyCode]);

            if (code != SCREEN_KEY_NONE)
                dispatchKeyEvent(code, true);

            break;
        }

        case NSEventTypeKeyUp:
        {
            const int code = translateMacKeyCode([event keyCode]);

            if (code != SCREEN_KEY_NONE)
                dispatchKeyEvent(code, false);

            break;
        }

        case NSEventTypeFlagsChanged:
        {
            // Modifier keys never produce KeyDown/KeyUp; derive the state from
            // the modifier flags. Caps Lock is a toggle on macOS and is ignored.
            const NSEventModifierFlags flags = [event modifierFlags];
            const unsigned short keyCode = [event keyCode];

            switch (keyCode)
            {
                case 56:
                case 60:
                    dispatchKeyEvent(SCREEN_KEY_SHIFT,
                        (flags & NSEventModifierFlagShift) != 0);
                    break;

                case 59:
                case 62:
                    dispatchKeyEvent(SCREEN_KEY_CTRL,
                        (flags & NSEventModifierFlagControl) != 0);
                    break;

                case 58:
                case 61:
                    dispatchKeyEvent(SCREEN_KEY_ALT,
                        (flags & NSEventModifierFlagOption) != 0);
                    break;

                case 54:
                case 55:
                {
                    const bool down = (flags & NSEventModifierFlagCommand) != 0;
                    dispatchKeyEvent(SCREEN_KEY_META, down);

                    if (!down)
                        releaseAllNonModifierKeys();

                    break;
                }

                default:
                    break;
            }

            break;
        }

        case NSEventTypeLeftMouseDown:
            if (inside)
                dispatchKeyEvent(SCREEN_MOUSE_LEFT, true);
            break;

        case NSEventTypeLeftMouseUp:
            dispatchKeyEvent(SCREEN_MOUSE_LEFT, false);
            break;

        case NSEventTypeRightMouseDown:
            if (inside)
                dispatchKeyEvent(SCREEN_MOUSE_RIGHT, true);
            break;

        case NSEventTypeRightMouseUp:
            dispatchKeyEvent(SCREEN_MOUSE_RIGHT, false);
            break;

        case NSEventTypeOtherMouseDown:
            if (inside && [event buttonNumber] == 2)
                dispatchKeyEvent(SCREEN_MOUSE_MIDDLE, true);
            break;

        case NSEventTypeOtherMouseUp:
            if ([event buttonNumber] == 2)
                dispatchKeyEvent(SCREEN_MOUSE_MIDDLE, false);
            break;

        default:
            break;
    }
}

#endif




// ============================================================
// CREATE SCREEN
// ============================================================

extern "C"
void* createScreen(int width, int height, const char* title)
{
    if (width <= 0 || height <= 0)
        return nullptr;

    AppScreen* s = allocScreenObject();

    if (!s)
        return nullptr;

    s->running = true;

    if (!resizeFramebuffer(s, width, height))
    {
        freeScreenObject(s);
        return nullptr;
    }

    const char* windowTitle = title ? title : "Screen";

    if (!storeTitle(s, windowTitle))
    {
        freeScreenObject(s);
        return nullptr;
    }

#ifdef _WIN32
    if (!registerScreenClass())
    {
        freeScreenObject(s);
        return nullptr;
    }

    wchar_t* wideTitle = utf8ToWide(windowTitle);

    if (!wideTitle)
    {
        freeScreenObject(s);
        return nullptr;
    }

    RECT rect{ 0, 0, width, height };

    AdjustWindowRect(
        &rect,
        WS_OVERLAPPEDWINDOW,
        FALSE
    );

    s->hwnd = CreateWindowExW(
        0,
        L"NativeScreenWindow",
        wideTitle,
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT,
        CW_USEDEFAULT,
        rect.right - rect.left,
        rect.bottom - rect.top,
        nullptr,
        nullptr,
        GetModuleHandleW(nullptr),
        s
    );

    std::free(wideTitle);

    if (!s->hwnd)
    {
        freeScreenObject(s);
        return nullptr;
    }

    ShowWindow(s->hwnd, SW_SHOW);
    UpdateWindow(s->hwnd);


#elif defined(__linux__)

s->display = XOpenDisplay(nullptr);

if (!s->display)
{
    freeScreenObject(s);
    return nullptr;
}

// Report real key releases only (no synthetic release/press pairs while a
// key is held down).
XkbSetDetectableAutoRepeat(s->display, True, nullptr);

int screen = DefaultScreen(s->display);

s->window = XCreateSimpleWindow(
    s->display,
    RootWindow(s->display, screen),
    0, 0,
    width, height,
    1,
    BlackPixel(s->display, screen),
    WhitePixel(s->display, screen)
);

if (!s->window)
{
    XCloseDisplay(s->display);
    s->display = nullptr;

    freeScreenObject(s);
    return nullptr;
}

s->wmDelete = XInternAtom(
    s->display,
    "WM_DELETE_WINDOW",
    False
);

XSetWMProtocols(
    s->display,
    s->window,
    &s->wmDelete,
    1
);

applyX11Title(s);

XSelectInput(
    s->display,
    s->window,
    ExposureMask |
    StructureNotifyMask |
    FocusChangeMask |
    KeyPressMask |
    KeyReleaseMask |
    PointerMotionMask |
    ButtonPressMask |
    ButtonReleaseMask
);

s->gc = XCreateGC(
    s->display,
    s->window,
    0,
    nullptr
);

if (!s->gc)
{
    XDestroyWindow(s->display, s->window);
    XCloseDisplay(s->display);

    s->window = 0;
    s->display = nullptr;

    freeScreenObject(s);
    return nullptr;
}

XMapWindow(s->display, s->window);
XFlush(s->display);


#elif defined(__APPLE__)

@autoreleasepool
{
    [NSApplication sharedApplication];

    // Launch the application object only once, however many screens exist.
    static bool cocoaLaunched = false;

    if (!cocoaLaunched)
    {
        [NSApp setActivationPolicy:
            NSApplicationActivationPolicyRegular];

        [NSApp finishLaunching];

        cocoaLaunched = true;
    }

    NSString* nsTitle =
        [NSString stringWithUTF8String:windowTitle];

    if (!nsTitle)
        nsTitle = @"Screen";

    NSRect rect = NSMakeRect(100, 100, width, height);

    s->window = [[NSWindow alloc]
        initWithContentRect:rect
        styleMask:
            NSWindowStyleMaskTitled |
            NSWindowStyleMaskClosable |
            NSWindowStyleMaskMiniaturizable |
            NSWindowStyleMaskResizable
        backing:NSBackingStoreBuffered
        defer:NO
    ];

    if (!s->window)
    {
        freeScreenObject(s);
        return nullptr;
    }

    [s->window setReleasedWhenClosed:NO];

    s->delegate = [[ScreenWindowDelegate alloc] init];

    if (!s->delegate)
    {
        [s->window close];

#if !__has_feature(objc_arc)
        [s->window release];
#endif

        s->window = nil;

        freeScreenObject(s);
        return nullptr;
    }

    s->delegate->owner = s;

    [s->window setDelegate:s->delegate];

    ScreenView* view = [[ScreenView alloc]
        initWithFrame:NSMakeRect(0, 0, width, height)
    ];

    if (!view)
    {
        [s->window setDelegate:nil];

        s->delegate->owner = nullptr;

#if !__has_feature(objc_arc)
        [s->delegate release];
        [s->window release];
#endif

        s->delegate = nil;
        s->window = nil;

        freeScreenObject(s);
        return nullptr;
    }

    view->owner = s;

    [s->window setContentView:view];

    [s->window makeFirstResponder:view];

#if !__has_feature(objc_arc)
    [view release];
#endif

    [s->window setAcceptsMouseMovedEvents:YES];

    [s->window setTitle:nsTitle];
    [s->window makeKeyAndOrderFront:nil];

    [NSApp activateIgnoringOtherApps:YES];

    addScreenToList(s);
}

#else

    freeScreenObject(s);
    return nullptr;

#endif

    return s;
}



extern "C"
void closeScreen(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s)
        return;

    s->running = false;

#ifdef _WIN32

    if (s->hwnd && IsWindow(s->hwnd))
        DestroyWindow(s->hwnd);

    s->hwnd = nullptr;


#elif defined(__linux__)

    if (s->display)
    {
        if (s->ximage)
        {
            XDestroyImage(s->ximage);
            s->ximage = nullptr;
        }

        if (s->gc)
        {
            XFreeGC(s->display, s->gc);
            s->gc = nullptr;
        }

        if (s->window)
        {
            XDestroyWindow(s->display, s->window);
            s->window = 0;
        }

        XSync(s->display, False);

        XCloseDisplay(s->display);
        s->display = nullptr;
    }


#elif defined(__APPLE__)

    @autoreleasepool
    {
        removeScreenFromList(s);

        if (s->window)
        {
            NSView* content = [s->window contentView];

            if ([content isKindOfClass:[ScreenView class]])
            {
                ScreenView* view = (ScreenView*)content;
                view->owner = nullptr;
            }

            [s->window setDelegate:nil];

            if (s->delegate)
            {
                s->delegate->owner = nullptr;

#if !__has_feature(objc_arc)
                [s->delegate release];
#endif

                s->delegate = nil;
            }

            [s->window close];

#if !__has_feature(objc_arc)
            [s->window release];
#endif

            s->window = nil;
        }
    }

#endif

    freeScreenObject(s);
}

extern "C"
bool isScreenRunning(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running)
        return false;

#ifdef _WIN32

    return s->hwnd && IsWindow(s->hwnd);

#elif defined(__linux__)

    return s->display != nullptr &&
           s->window != 0 &&
           s->running;

#elif defined(__APPLE__)

    return s->window != nil && s->running;

#else

    return false;

#endif
}


extern "C"
void pollScreenEvents(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running)
        return;

#ifdef _WIN32

    HWND target = s->hwnd;

    if (!target || !IsWindow(target))
    {
        s->running = false;
        return;
    }

    MSG msg{};

    while (s->running &&
           PeekMessageW(
               &msg,
               target,
               0,
               0,
               PM_REMOVE))
    {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

#elif defined(__linux__)

    if (!s->display || !s->window)
        return;

    while (s->running && XPending(s->display) > 0)
    {
        XEvent event{};
        XNextEvent(s->display, &event);

        switch (event.type)
        {
            case ClientMessage:
            {
                if ((Atom)event.xclient.data.l[0] == s->wmDelete)
                {
                    s->running = false;

                    Window oldWindow = s->window;
                    s->window = 0;

                    XDestroyWindow(s->display, oldWindow);
                    XFlush(s->display);
                }
                break;
            }

            case DestroyNotify:
                s->running = false;
                s->window = 0;
                break;

            case FocusOut:
                releaseAllPressedKeys();
                break;

            case ConfigureNotify:
                resizeFramebuffer(
                    s,
                    event.xconfigure.width,
                    event.xconfigure.height
                );
                break;

            case MotionNotify:
            {
                s->mouseX = event.xmotion.x;
                s->mouseY = event.xmotion.y;
                g_mouseX = s->mouseX;
                g_mouseY = s->mouseY;

                if (g_mouseMoveCallback)
                {
                    g_mouseMoveCallback(
                        s->mouseX,
                        s->mouseY
                    );
                }
                break;
            }

            case KeyPress:
            {
                const int code = translateKeysym(
                    XLookupKeysym(&event.xkey, 0)
                );

                if (code != SCREEN_KEY_NONE)
                    dispatchKeyEvent(code, true);

                break;
            }

            case KeyRelease:
            {
                const int code = translateKeysym(
                    XLookupKeysym(&event.xkey, 0)
                );

                if (code != SCREEN_KEY_NONE)
                    dispatchKeyEvent(code, false);

                break;
            }

            case ButtonPress:
            {
                const int code = translateXButton(event.xbutton.button);

                if (code != SCREEN_KEY_NONE)
                    dispatchKeyEvent(code, true);

                break;
            }

            case ButtonRelease:
            {
                const int code = translateXButton(event.xbutton.button);

                if (code != SCREEN_KEY_NONE)
                    dispatchKeyEvent(code, false);

                break;
            }

            case Expose:
                if (event.xexpose.count == 0)
                    drawScreenToX11(s);
                break;
        }
    }


#elif defined(__APPLE__)

    @autoreleasepool
    {
        // Drain the whole event queue and route each event to the screen that
        // owns its window, so polling any screen services every window.
        while (true)
        {
            NSEvent* event = [NSApp
                nextEventMatchingMask:NSEventMaskAny
                untilDate:[NSDate distantPast]
                inMode:NSDefaultRunLoopMode
                dequeue:YES
            ];

            if (!event)
                break;

            AppScreen* target = findScreenByWindow([event window]);

            if (target)
                handleCocoaEvent(target, event);

            [NSApp sendEvent:event];
        }

        [NSApp updateWindows];
    }

#endif
}


extern "C"
void presentScreen(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running || !s->pixels)
        return;

#ifdef _WIN32

    if (!s->hwnd || !IsWindow(s->hwnd))
        return;

    InvalidateRect(s->hwnd, nullptr, FALSE);
    UpdateWindow(s->hwnd);

#elif defined(__linux__)

    if (!s->display || !s->window)
        return;

    drawScreenToX11(s);

#elif defined(__APPLE__)

    if (!s->window)
        return;

    @autoreleasepool
    {
        [[s->window contentView] setNeedsDisplay:YES];
        [s->window displayIfNeeded];
    }

#endif
}

static uint8_t clampColor(int value)
{
    if (value < 0)
        return 0;

    if (value > 255)
        return 255;

    return static_cast<uint8_t>(value);
}


extern "C"
void setScreenColor(
    void* screen,
    int r,
    int g,
    int b,
    int a)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s)
        return;

    s->colorR = clampColor(r);
    s->colorG = clampColor(g);
    s->colorB = clampColor(b);
    s->colorA = clampColor(a);
}

extern "C"
void clearScreen(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels)
        return;

    const std::size_t pixelCount =
        static_cast<std::size_t>(s->width) *
        static_cast<std::size_t>(s->height);

    // Clearing replaces the pixels (no blending); the window stays opaque.
#if SCREEN_ALPHA_BLEND
    const uint8_t alpha = 255;
#else
    const uint8_t alpha = s->colorA;
#endif

    for (std::size_t i = 0; i < pixelCount; ++i)
    {
        const std::size_t index = i * 4;

        s->pixels[index]     = s->colorR;
        s->pixels[index + 1] = s->colorG;
        s->pixels[index + 2] = s->colorB;
        s->pixels[index + 3] = alpha;
    }
}


extern "C"
void drawScreenPixel(void* screen, int x, int y)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels)
        return;

    // Check framebuffer boundaries.
    if (x < 0 || y < 0 ||
        x >= s->width || y >= s->height)
        return;

    // Calculate pixel position in the RGBA buffer.
    const std::size_t index =
        (static_cast<std::size_t>(y) *
         static_cast<std::size_t>(s->width) +
         static_cast<std::size_t>(x)) * 4;

    // Write RGBA color.
    writeColorAt(s, index);
}


static void putScreenPixel64(
    AppScreen* s,
    int64_t x,
    int64_t y)
{
    if (!s || !s->pixels)
        return;

    if (x < 0 || y < 0 ||
        x >= s->width || y >= s->height)
        return;

    const std::size_t index =
        (static_cast<std::size_t>(y) *
         static_cast<std::size_t>(s->width) +
         static_cast<std::size_t>(x)) * 4;

    writeColorAt(s, index);
}

static const uint8_t FONT_5X7[36][5] =
{
    // A-Z
    {0x7E,0x11,0x11,0x11,0x7E},
    {0x7F,0x49,0x49,0x49,0x36},
    {0x3E,0x41,0x41,0x41,0x22},
    {0x7F,0x41,0x41,0x22,0x1C},
    {0x7F,0x49,0x49,0x49,0x41},
    {0x7F,0x09,0x09,0x09,0x01},
    {0x3E,0x41,0x49,0x49,0x7A},
    {0x7F,0x08,0x08,0x08,0x7F},
    {0x00,0x41,0x7F,0x41,0x00},
    {0x20,0x40,0x41,0x3F,0x01},
    {0x7F,0x08,0x14,0x22,0x41},
    {0x7F,0x40,0x40,0x40,0x40},
    {0x7F,0x02,0x0C,0x02,0x7F},
    {0x7F,0x04,0x08,0x10,0x7F},
    {0x3E,0x41,0x41,0x41,0x3E},
    {0x7F,0x09,0x09,0x09,0x06},
    {0x3E,0x41,0x51,0x21,0x5E},
    {0x7F,0x09,0x19,0x29,0x46},
    {0x46,0x49,0x49,0x49,0x31},
    {0x01,0x01,0x7F,0x01,0x01},
    {0x3F,0x40,0x40,0x40,0x3F},
    {0x1F,0x20,0x40,0x20,0x1F},
    {0x7F,0x20,0x18,0x20,0x7F},
    {0x63,0x14,0x08,0x14,0x63},
    {0x07,0x08,0x70,0x08,0x07},
    {0x61,0x51,0x49,0x45,0x43},

    // 0-9
    {0x3E,0x51,0x49,0x45,0x3E},
    {0x00,0x42,0x7F,0x40,0x00},
    {0x42,0x61,0x51,0x49,0x46},
    {0x21,0x41,0x45,0x4B,0x31},
    {0x18,0x14,0x12,0x7F,0x10},
    {0x27,0x45,0x45,0x45,0x39},
    {0x3C,0x4A,0x49,0x49,0x30},
    {0x01,0x71,0x09,0x05,0x03},
    {0x36,0x49,0x49,0x49,0x36},
    {0x06,0x49,0x49,0x29,0x1E}
};


static const uint8_t* getScreenGlyph(unsigned char c)
{
    // Convert lowercase to uppercase.
    if (c >= 'a' && c <= 'z')
        c = static_cast<unsigned char>(c - 'a' + 'A');

    if (c >= 'A' && c <= 'Z')
        return FONT_5X7[c - 'A'];

    if (c >= '0' && c <= '9')
        return FONT_5X7[26 + c - '0'];

    static const uint8_t space[5] = {0,0,0,0,0};
    static const uint8_t dot[5] = {0,0x60,0x60,0,0};
    static const uint8_t comma[5] = {0,0x40,0x20,0,0};
    static const uint8_t colon[5] = {0,0x36,0x36,0,0};
    static const uint8_t semicolon[5] = {0,0x56,0x36,0,0};
    static const uint8_t exclamation[5] = {0,0,0x5F,0,0};
    static const uint8_t question[5] = {0x02,0x01,0x51,0x09,0x06};
    static const uint8_t dash[5] = {0x08,0x08,0x08,0x08,0x08};
    static const uint8_t underscore[5] = {0x40,0x40,0x40,0x40,0x40};
    static const uint8_t plus[5] = {0x08,0x08,0x3E,0x08,0x08};
    static const uint8_t equal[5] = {0x14,0x14,0x14,0x14,0x14};
    static const uint8_t leftParen[5] = {0,0x1C,0x22,0x41,0};
    static const uint8_t rightParen[5] = {0,0x41,0x22,0x1C,0};
    static const uint8_t leftBracket[5] = {0,0x7F,0x41,0x41,0};
    static const uint8_t rightBracket[5] = {0,0x41,0x41,0x7F,0};
    static const uint8_t slash[5] = {0x20,0x10,0x08,0x04,0x02};
    static const uint8_t backslash[5] = {0x02,0x04,0x08,0x10,0x20};
    static const uint8_t star[5] = {0x14,0x08,0x3E,0x08,0x14};
    static const uint8_t apostrophe[5] = {0x02,0x01,0,0,0};
    static const uint8_t quotation[5] = {0x03,0,0x03,0,0};
    static const uint8_t hash[5] = {0x14,0x7F,0x14,0x7F,0x14};
    static const uint8_t less[5] = {0x08,0x14,0x22,0x41,0};
    static const uint8_t greater[5] = {0,0x41,0x22,0x14,0x08};
    static const uint8_t percent[5] = {0x23,0x13,0x08,0x64,0x62};

    switch (c)
    {
        case ' ': return space;
        case '.': return dot;
        case ',': return comma;
        case ':': return colon;
        case ';': return semicolon;
        case '!': return exclamation;
        case '?': return question;
        case '-': return dash;
        case '_': return underscore;
        case '+': return plus;
        case '=': return equal;
        case '(': return leftParen;
        case ')': return rightParen;
        case '[': return leftBracket;
        case ']': return rightBracket;
        case '/': return slash;
        case '\\': return backslash;
        case '*': return star;
        case '\'': return apostrophe;
        case '"': return quotation;
        case '#': return hash;
        case '<': return less;
        case '>': return greater;
        case '%': return percent;
        default:  return question;
    }
}

static bool clipScreenLine(
    int width,
    int height,
    int& x1,
    int& y1,
    int& x2,
    int& y2)
{
    enum
    {
        LEFT   = 1,
        RIGHT  = 2,
        TOP    = 4,
        BOTTOM = 8
    };

    auto getCode = [&](double x, double y)
    {
        int code = 0;

        if (x < 0)
            code |= LEFT;
        else if (x >= width)
            code |= RIGHT;

        if (y < 0)
            code |= TOP;
        else if (y >= height)
            code |= BOTTOM;

        return code;
    };

    double ax = x1;
    double ay = y1;
    double bx = x2;
    double by = y2;

    bool accepted = false;

    for (int i = 0; i < 8; ++i)
    {
        int codeA = getCode(ax, ay);
        int codeB = getCode(bx, by);

        if (!(codeA | codeB))
        {
            accepted = true;
            break;
        }

        if (codeA & codeB)
            return false;

        int code = codeA ? codeA : codeB;

        double x = 0;
        double y = 0;

        if (code & TOP)
        {
            x = ax + (bx - ax) * (0 - ay) / (by - ay);
            y = 0;
        }
        else if (code & BOTTOM)
        {
            y = height - 1;
            x = ax + (bx - ax) * (y - ay) / (by - ay);
        }
        else if (code & RIGHT)
        {
            x = width - 1;
            y = ay + (by - ay) * (x - ax) / (bx - ax);
        }
        else if (code & LEFT)
        {
            x = 0;
            y = ay + (by - ay) * (x - ax) / (bx - ax);
        }

        if (code == codeA)
        {
            ax = x;
            ay = y;
        }
        else
        {
            bx = x;
            by = y;
        }
    }

    if (!accepted)
        return false;

    x1 = static_cast<int>(std::lround(ax));
    y1 = static_cast<int>(std::lround(ay));
    x2 = static_cast<int>(std::lround(bx));
    y2 = static_cast<int>(std::lround(by));

    // Protect against rounding at framebuffer boundaries.
    x1 = (x1 < 0) ? 0 : (x1 >= width ? width - 1 : x1);
    y1 = (y1 < 0) ? 0 : (y1 >= height ? height - 1 : y1);
    x2 = (x2 < 0) ? 0 : (x2 >= width ? width - 1 : x2);
    y2 = (y2 < 0) ? 0 : (y2 >= height ? height - 1 : y2);

    return true;
}


extern "C"
void drawScreenLine(
    void* screen,
    int x1,
    int y1,
    int x2,
    int y2)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels)
        return;

    // Clip the line to the framebuffer.
    if (!clipScreenLine(
        s->width,
        s->height,
        x1, y1,
        x2, y2))
    {
        return;
    }

    // Use 64-bit integers to avoid overflow.
    int64_t x = x1;
    int64_t y = y1;

    const int64_t endX = x2;
    const int64_t endY = y2;

    const int64_t dx = std::llabs(endX - x);
    const int64_t dy = -std::llabs(endY - y);

    const int64_t sx = (x < endX) ? 1 : -1;
    const int64_t sy = (y < endY) ? 1 : -1;

    int64_t error = dx + dy;

    while (true)
    {
        // Write current pixel.
        const std::size_t index =
            (static_cast<std::size_t>(y) *
             static_cast<std::size_t>(s->width) +
             static_cast<std::size_t>(x)) * 4;

        writeColorAt(s, index);

        if (x == endX && y == endY)
            break;

        const int64_t error2 = 2 * error;

        if (error2 >= dy)
        {
            error += dy;
            x += sx;
        }

        if (error2 <= dx)
        {
            error += dx;
            y += sy;
        }
    }
}

extern "C"
void drawScreenRect(
    void* screen,
    int x,
    int y,
    int width,
    int height)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || width <= 0 || height <= 0)
        return;

    // Use 64-bit coordinates to prevent integer overflow.
    const int64_t left = x;
    const int64_t top = y;
    const int64_t right = left + width - 1;
    const int64_t bottom = top + height - 1;

    // Draw the top edge.
    if (top >= 0 && top < s->height)
    {
        const int64_t start = (left < 0) ? 0 : left;
        const int64_t end =
            (right >= s->width) ? s->width - 1 : right;

        for (int64_t px = start; px <= end; ++px)
            drawScreenPixel(s, static_cast<int>(px),
                            static_cast<int>(top));
    }

    // Draw the bottom edge.
    if (bottom >= 0 && bottom < s->height && bottom != top)
    {
        const int64_t start = (left < 0) ? 0 : left;
        const int64_t end =
            (right >= s->width) ? s->width - 1 : right;

        for (int64_t px = start; px <= end; ++px)
            drawScreenPixel(s, static_cast<int>(px),
                            static_cast<int>(bottom));
    }

    // Draw the left edge.
    if (left >= 0 && left < s->width)
    {
        const int64_t start = (top < 0) ? 0 : top;
        const int64_t end =
            (bottom >= s->height) ? s->height - 1 : bottom;

        for (int64_t py = start; py <= end; ++py)
            drawScreenPixel(s, static_cast<int>(left),
                            static_cast<int>(py));
    }

    // Draw the right edge.
    if (right >= 0 && right < s->width && right != left)
    {
        const int64_t start = (top < 0) ? 0 : top;
        const int64_t end =
            (bottom >= s->height) ? s->height - 1 : bottom;

        for (int64_t py = start; py <= end; ++py)
            drawScreenPixel(s, static_cast<int>(right),
                            static_cast<int>(py));
    }
}


extern "C"
void fillScreenRect(
    void* screen,
    int x,
    int y,
    int width,
    int height)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || width <= 0 || height <= 0)
        return;

    // Calculate rectangle boundaries using 64-bit integers.
    const int64_t left = x;
    const int64_t top = y;
    const int64_t right = left + width;
    const int64_t bottom = top + height;

    // Clip rectangle against framebuffer boundaries.
    const int64_t startX = (left < 0) ? 0 : left;
    const int64_t startY = (top < 0) ? 0 : top;

    const int64_t endX =
        (right > s->width) ? s->width : right;

    const int64_t endY =
        (bottom > s->height) ? s->height : bottom;

    if (startX >= endX || startY >= endY)
        return;

    // Fill rectangle.
    for (int64_t py = startY; py < endY; ++py)
    {
        for (int64_t px = startX; px < endX; ++px)
        {
            const std::size_t index =
                (static_cast<std::size_t>(py) *
                 static_cast<std::size_t>(s->width) +
                 static_cast<std::size_t>(px)) * 4;

            writeColorAt(s, index);
        }
    }
}


static uint64_t integerSqrt(uint64_t value);

// Circles at least this large are walked only over the arc that can touch the
// framebuffer (the loop is otherwise O(radius), ~30 s for radius 2 billion).
#ifndef SCREEN_CIRCLE_JUMP_RADIUS
#define SCREEN_CIRCLE_JUMP_RADIUS 65536
#endif

// Midpoint-circle loop over one octant range, plotting 8 symmetric points.
static void plotCircleOctants(
    AppScreen* s,
    int64_t cx,
    int64_t cy,
    int64_t dx,
    int64_t dy,
    int64_t decision,
    int64_t lastDx)
{
    while (dx <= dy && dx <= lastDx)
    {
        // Eight symmetrical points.
        putScreenPixel64(s, cx + dx, cy + dy);
        putScreenPixel64(s, cx - dx, cy + dy);

        putScreenPixel64(s, cx + dx, cy - dy);
        putScreenPixel64(s, cx - dx, cy - dy);

        putScreenPixel64(s, cx + dy, cy + dx);
        putScreenPixel64(s, cx - dy, cy + dx);

        putScreenPixel64(s, cx + dy, cy - dx);
        putScreenPixel64(s, cx - dy, cy - dx);

        if (decision < 0)
        {
            decision += 2 * dx + 3;
        }
        else
        {
            decision += 2 * (dx - dy) + 5;
            --dy;
        }

        ++dx;
    }
}

// dy of the midpoint-circle walk at a given dx: the largest y with
// y*y - y < r*r - dx*dx (verified against the step-by-step recursion).
static int64_t circleDyAt(int64_t r, int64_t dx)
{
    const uint64_t n = static_cast<uint64_t>(r * r - dx * dx);

    if (n == 0)
        return 0;

    return static_cast<int64_t>((integerSqrt(4 * n - 3) + 1) / 2);
}

extern "C"
void drawScreenCircle(
    void* screen,
    int x,
    int y,
    int radius)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || radius < 0)
        return;

    const int64_t cx = x;
    const int64_t cy = y;
    const int64_t r = radius;

    const int64_t w = s->width;
    const int64_t h = s->height;

    // Reject circles completely outside the framebuffer.
    if (cx + r < 0 || cy + r < 0 ||
        cx - r >= w || cy - r >= h)
        return;

    // Largest horizontal / vertical distance from the center to any
    // framebuffer pixel.
    const int64_t offLeft   = cx < 0 ? -cx : cx;
    const int64_t offRight  = (w - 1 - cx) < 0 ? -(w - 1 - cx) : (w - 1 - cx);
    const int64_t offTop    = cy < 0 ? -cy : cy;
    const int64_t offBottom = (h - 1 - cy) < 0 ? -(h - 1 - cy) : (h - 1 - cy);

    const int64_t maxOffX = offLeft > offRight ? offLeft : offRight;
    const int64_t maxOffY = offTop > offBottom ? offTop : offBottom;

    // If the whole framebuffer lies inside the circle, the outline cannot be
    // visible.
    const long double inner = static_cast<long double>(r) - 1.5L;

    if (inner > 0 &&
        static_cast<long double>(maxOffX) * maxOffX +
        static_cast<long double>(maxOffY) * maxOffY < inner * inner)
    {
        return;
    }

    // Points use dx as an x offset (first four) or a y offset (last four), so
    // nothing past the larger framebuffer extent can be visible.
    const int64_t maxOff = maxOffX > maxOffY ? maxOffX : maxOffY;

    if (r < SCREEN_CIRCLE_JUMP_RADIUS)
    {
        plotCircleOctants(s, cx, cy, 0, r, 1 - r, maxOff);
        return;
    }

    // Large circle. A point is visible only if dx lies in the set of offsets
    // that reach the framebuffer horizontally or vertically, so walk just
    // those dx spans, starting each one directly at the right (dy, decision).
    struct Span { int64_t lo; int64_t hi; };

    Span spans[4];
    int count = 0;

    const int64_t bounds[4][2] =
    {
        { -cx,           w - 1 - cx },
        { cx - (w - 1),  cx         },
        { -cy,           h - 1 - cy },
        { cy - (h - 1),  cy         }
    };

    for (int i = 0; i < 4; ++i)
    {
        int64_t lo = bounds[i][0] < 0 ? 0 : bounds[i][0];
        int64_t hi = bounds[i][1] > r ? r : bounds[i][1];

        if (lo > hi)
            continue;

        // Insertion sort by lo.
        int j = count++;

        while (j > 0 && spans[j - 1].lo > lo)
        {
            spans[j] = spans[j - 1];
            --j;
        }

        spans[j].lo = lo;
        spans[j].hi = hi;
    }

    // Merge overlapping spans so no pixel is plotted twice (matters when
    // blending translucent colors).
    int merged = 0;

    for (int i = 0; i < count; ++i)
    {
        if (merged > 0 && spans[i].lo <= spans[merged - 1].hi + 1)
        {
            if (spans[i].hi > spans[merged - 1].hi)
                spans[merged - 1].hi = spans[i].hi;
        }
        else
        {
            spans[merged++] = spans[i];
        }
    }

    for (int i = 0; i < merged; ++i)
    {
        const int64_t dx = spans[i].lo;
        const int64_t dy = circleDyAt(r, dx);

        if (dx > dy)
            continue;

        const int64_t decision =
            (dx + 1) * (dx + 1) + dy * dy - dy - r * r;

        plotCircleOctants(s, cx, cy, dx, dy, decision, spans[i].hi);
    }
}


static uint64_t integerSqrt(uint64_t value)
{
    uint64_t result = static_cast<uint64_t>(
        std::sqrt(static_cast<long double>(value))
    );

    // Correct any floating-point rounding.
    while (result < UINT64_MAX &&
           result + 1 <= value / (result + 1))
    {
        ++result;
    }

    while (result > 0 && result > value / result)
    {
        --result;
    }

    return result;
}


extern "C"
void fillScreenCircle(
    void* screen,
    int x,
    int y,
    int radius)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || radius < 0)
        return;

    const int64_t cx = x;
    const int64_t cy = y;
    const int64_t r = radius;

    // Reject circles completely outside the framebuffer.
    if (cx + r < 0 || cy + r < 0 ||
        cx - r >= s->width || cy - r >= s->height)
        return;

    const uint64_t radiusSquared =
        static_cast<uint64_t>(r) *
        static_cast<uint64_t>(r);

    // Restrict work to visible rows.
    const int64_t startY =
        (cy - r < 0) ? 0 : cy - r;

    const int64_t endY =
        (cy + r >= s->height)
            ? s->height - 1
            : cy + r;

    for (int64_t py = startY; py <= endY; ++py)
    {
        const int64_t dy = py - cy;
        const uint64_t absDy =
            static_cast<uint64_t>(dy < 0 ? -dy : dy);

        const uint64_t dySquared = absDy * absDy;

        if (dySquared > radiusSquared)
            continue;

        // Calculate the horizontal radius of this scanline.
        const uint64_t remaining =
            radiusSquared - dySquared;

        const int64_t halfWidth =
            static_cast<int64_t>(integerSqrt(remaining));

        // Calculate and clip horizontal boundaries.
        const int64_t left =
            (cx - halfWidth < 0)
                ? 0
                : cx - halfWidth;

        const int64_t right =
            (cx + halfWidth >= s->width)
                ? s->width - 1
                : cx + halfWidth;

        // Fill the scanline.
        for (int64_t px = left; px <= right; ++px)
        {
            const std::size_t index =
                (static_cast<std::size_t>(py) *
                 static_cast<std::size_t>(s->width) +
                 static_cast<std::size_t>(px)) * 4;

            writeColorAt(s, index);
        }
    }
}


extern "C"
void drawScreenTriangle(
    void* screen,
    int x1, int y1,
    int x2, int y2,
    int x3, int y3)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels)
        return;

    // First edge.
    drawScreenLine(s, x1, y1, x2, y2);

    // Second edge.
    drawScreenLine(s, x2, y2, x3, y3);

    // Third edge.
    drawScreenLine(s, x3, y3, x1, y1);
}



static void fillScreenSpan(
    AppScreen* s,
    int y,
    long double left,
    long double right)
{
    if (!s || !s->pixels ||
        y < 0 || y >= s->height)
        return;

    if (left > right)
    {
        const long double temp = left;
        left = right;
        right = temp;
    }

    // Pixel centers are located at x + 0.5.
    const long double first = std::ceil(left - 0.5L);
    const long double last = std::floor(right - 0.5L);

    if (last < 0 || first >= s->width)
        return;

    const int64_t start =
        (first < 0) ? 0 : static_cast<int64_t>(first);

    const int64_t end =
        (last >= s->width)
            ? s->width - 1
            : static_cast<int64_t>(last);

    for (int64_t x = start; x <= end; ++x)
    {
        const std::size_t index =
            (static_cast<std::size_t>(y) *
             static_cast<std::size_t>(s->width) +
             static_cast<std::size_t>(x)) * 4;

        writeColorAt(s, index);
    }
}


extern "C"
void fillScreenTriangle(
    void* screen,
    int x1, int y1,
    int x2, int y2,
    int x3, int y3)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels)
        return;

    // Use 64-bit coordinates for safe calculations.
    const int64_t ax = x1;
    const int64_t ay = y1;

    const int64_t bx = x2;
    const int64_t by = y2;

    const int64_t cx = x3;
    const int64_t cy = y3;

    // Detect degenerate triangles.
    const long double area =
        (static_cast<long double>(bx) - ax) *
        (static_cast<long double>(cy) - ay) -
        (static_cast<long double>(by) - ay) *
        (static_cast<long double>(cx) - ax);

    if (area == 0.0L)
        return;

    // Find vertical boundaries.
    int64_t minY = ay;
    int64_t maxY = ay;

    if (by < minY) minY = by;
    if (cy < minY) minY = cy;

    if (by > maxY) maxY = by;
    if (cy > maxY) maxY = cy;

    // Clip vertical boundaries.
    const int64_t startY =
        (minY < 0) ? 0 : minY;

    const int64_t endY =
        (maxY >= s->height)
            ? s->height - 1
            : maxY - 1;

    if (startY > endY)
        return;

    // Process each visible scanline.
    for (int64_t py = startY; py <= endY; ++py)
    {
        const long double scanY =
            static_cast<long double>(py) + 0.5L;

        long double intersections[3];
        int count = 0;

        // Check an edge for intersection with this scanline.
        auto addEdge = [&](int64_t xA, int64_t yA,
                           int64_t xB, int64_t yB)
        {
            if (yA == yB)
                return;

            // Ignore the upper endpoint of each edge.
            if (!(
                (scanY >= yA && scanY < yB) ||
                (scanY >= yB && scanY < yA)
            ))
            {
                return;
            }

            const long double t =
                (scanY - static_cast<long double>(yA)) /
                (static_cast<long double>(yB) - yA);

            const long double intersection =
                static_cast<long double>(xA) +
                t * (
                    static_cast<long double>(xB) - xA
                );

            intersections[count++] = intersection;
        };

        // Calculate intersections with all three edges.
        addEdge(ax, ay, bx, by);
        addEdge(bx, by, cx, cy);
        addEdge(cx, cy, ax, ay);

        // A non-degenerate triangle normally has two
        // intersections on every interior scanline.
        if (count < 2)
            continue;

        // Sort the intersections.
        if (intersections[0] > intersections[1])
        {
            const long double temp = intersections[0];
            intersections[0] = intersections[1];
            intersections[1] = temp;
        }

        // Fill between the two intersections.
        fillScreenSpan(
            s,
            static_cast<int>(py),
            intersections[0],
            intersections[1]
        );
    }
}


extern "C"
void drawScreenText(
    void* screen,
    const char* text,
    int x,
    int y)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || !text)
        return;

    // Everything below the framebuffer is invisible.
    if (y >= s->height)
        return;

    const int64_t originX = x;

    int64_t penX = x;
    int64_t penY = y;

    const int64_t maxValue =
        std::numeric_limits<int64_t>::max();

    // Once a line runs past the right edge the rest of that line is skipped,
    // but later lines (after '\n') are still drawn.
    bool skipLine = false;

    for (std::size_t i = 0; text[i] != '\0'; ++i)
    {
        unsigned char c =
            static_cast<unsigned char>(text[i]);

        // Handle carriage returns.
        if (c == '\r')
            continue;

        // Handle newlines.
        if (c == '\n')
        {
            penX = originX;
            skipLine = false;

            if (penY > maxValue - 9)
                break;

            penY += 9;

            if (penY >= s->height)
                break;

            continue;
        }

        if (skipLine)
            continue;

        // Handle tabs (four character spaces).
        if (c == '\t')
        {
            if (penX > maxValue - 24)
                break;

            penX += 24;

            if (penX >= s->width)
                skipLine = true;

            continue;
        }

        // Non-ASCII UTF-8 characters become '?'.
        if (c & 0x80)
        {
            while (text[i + 1] &&
                (static_cast<unsigned char>(text[i + 1]) & 0xC0) == 0x80)
            {
                ++i;
            }

            c = '?';
        }

        const uint8_t* glyph = getScreenGlyph(c);

        // Draw the 5x7 glyph.
        for (int col = 0; col < 5; ++col)
        {
            for (int row = 0; row < 7; ++row)
            {
                if (glyph[col] & (1u << row))
                {
                    putScreenPixel64(
                        s,
                        penX + col,
                        penY + row
                    );
                }
            }
        }

        // Advance to the next character.
        if (penX > maxValue - 6)
            break;

        penX += 6;

        if (penX >= s->width)
            skipLine = true;
    }
}

static uint16_t readBmpU16(const uint8_t* p)
{
    return static_cast<uint16_t>(
        p[0] |
        (static_cast<uint16_t>(p[1]) << 8)
    );
}

static uint32_t readBmpU32(const uint8_t* p)
{
    return
        static_cast<uint32_t>(p[0]) |
        (static_cast<uint32_t>(p[1]) << 8) |
        (static_cast<uint32_t>(p[2]) << 16) |
        (static_cast<uint32_t>(p[3]) << 24);
}

static int64_t readBmpI32(const uint8_t* p)
{
    uint32_t value = readBmpU32(p);

    if (value & 0x80000000u)
        return static_cast<int64_t>(value) - 0x100000000LL;

    return static_cast<int64_t>(value);
}


// UTF-8 path on every platform (Windows converts to a wide path).
static FILE* openFileUtf8(const char* path)
{
#ifdef _WIN32
    wchar_t* wide = utf8ToWide(path);

    if (!wide)
        return nullptr;

    FILE* f = _wfopen(wide, L"rb");

    std::free(wide);
    return f;
#else
    return std::fopen(path, "rb");
#endif
}

// One color channel of a 32-bit BMP pixel, described by its bit mask.
struct BmpChannel
{
    uint32_t mask = 0;
    unsigned shift = 0;
    uint32_t maxValue = 0;
};

static BmpChannel makeBmpChannel(uint32_t mask)
{
    BmpChannel c;
    c.mask = mask;

    if (mask)
    {
        while (((mask >> c.shift) & 1u) == 0)
            ++c.shift;

        c.maxValue = mask >> c.shift;
    }

    return c;
}

static uint8_t readBmpChannel(const BmpChannel& c, uint32_t pixel)
{
    if (!c.mask)
        return 0;

    const uint64_t v = (pixel & c.mask) >> c.shift;

    if (c.maxValue == 255)
        return static_cast<uint8_t>(v);

    return static_cast<uint8_t>(
        (v * 255u + c.maxValue / 2) / c.maxValue
    );
}


// Decodes the BMP in `file` into the framebuffer. The caller closes the file.
// Cleanup is explicit (no destructors) so the generated code needs no
// exception-unwinding support and works with or without -fexceptions.
static bool drawBmpFromFile(AppScreen* s, FILE* file, int x, int y)
{
    // Read BMP file and DIB headers.
    uint8_t header[54]{};

    if (std::fread(header, 1, sizeof(header), file) != sizeof(header))
        return false;

    // Check BMP signature.
    if (header[0] != 'B' || header[1] != 'M')
        return false;

    const uint32_t pixelOffset =
        readBmpU32(header + 10);

    const uint32_t dibSize =
        readBmpU32(header + 14);

    if (dibSize < 40)
        return false;

    const int64_t bmpWidth =
        readBmpI32(header + 18);

    const int64_t bmpHeight =
        readBmpI32(header + 22);

    const uint16_t planes =
        readBmpU16(header + 26);

    const uint16_t bitsPerPixel =
        readBmpU16(header + 28);

    const uint32_t compression =
        readBmpU32(header + 30);

    // Validate dimensions and format.
    if (bmpWidth <= 0 || bmpHeight == 0)
        return false;

    if (planes != 1)
        return false;

    // Support 24-bit and 32-bit BMP.
    if (bitsPerPixel != 24 &&
        bitsPerPixel != 32)
    {
        return false;
    }

    // 32-bit BMPs are BGRA by default; BI_BITFIELDS (3) supplies the masks.
    uint32_t redMask   = 0x00FF0000u;
    uint32_t greenMask = 0x0000FF00u;
    uint32_t blueMask  = 0x000000FFu;

    if (compression == 3)
    {
        if (bitsPerPixel != 32)
            return false;

        // Masks sit at file offset 54 for BITMAPINFOHEADER (right after the
        // header) and for V2+ headers (inside the header).
        if (dibSize != 40 && dibSize < 52)
            return false;

        if (dibSize == 40 && pixelOffset < 66)
            return false;

        uint8_t masks[12];

        if (std::fseek(file, 54, SEEK_SET) != 0 ||
            std::fread(masks, 1, sizeof(masks), file) != sizeof(masks))
        {
            return false;
        }

        redMask   = readBmpU32(masks);
        greenMask = readBmpU32(masks + 4);
        blueMask  = readBmpU32(masks + 8);
    }
    else if (compression != 0)
    {
        // RLE and other compressed formats are not supported.
        return false;
    }

    const uint64_t width =
        static_cast<uint64_t>(bmpWidth);

    const uint64_t height =
        static_cast<uint64_t>(
            bmpHeight < 0 ? -bmpHeight : bmpHeight
        );

    const bool topDown = bmpHeight < 0;

    const uint64_t bytesPerPixel =
        bitsPerPixel / 8;

    // BMP rows are aligned to four-byte boundaries.
    const uint64_t rowStride =
        ((width * bitsPerPixel + 31) / 32) * 4;

    if (height > UINT64_MAX / rowStride)
        return false;

    const uint64_t imageDataSize =
        rowStride * height;

    if (rowStride > std::numeric_limits<size_t>::max() ||
        imageDataSize > std::numeric_limits<size_t>::max())
    {
        return false;
    }

    // Check that the pixel data exists in the file.
    if (std::fseek(file, 0, SEEK_END) != 0)
        return false;

    const long endPosition = std::ftell(file);

    if (endPosition < 0)
        return false;

    const uint64_t fileSize = static_cast<uint64_t>(endPosition);

    if (pixelOffset < 14ull + dibSize ||
        pixelOffset > fileSize)
    {
        return false;
    }

    if (imageDataSize > fileSize - pixelOffset)
        return false;

    // Visible range of source columns, [firstX, lastX).
    int64_t firstX = -static_cast<int64_t>(x);

    if (firstX < 0)
        firstX = 0;

    int64_t lastX =
        static_cast<int64_t>(s->width) - static_cast<int64_t>(x);

    if (lastX > static_cast<int64_t>(width))
        lastX = static_cast<int64_t>(width);

    if (firstX >= lastX)
        return true;   // nothing visible horizontally

    // Read all pixel data before modifying the framebuffer.
    uint8_t* imageData = static_cast<uint8_t*>(
        std::malloc(static_cast<size_t>(imageDataSize))
    );

    if (!imageData)
        return false;

    if (std::fseek(file, static_cast<long>(pixelOffset), SEEK_SET) != 0 ||
        std::fread(imageData, 1, static_cast<size_t>(imageDataSize), file) !=
            static_cast<size_t>(imageDataSize))
    {
        std::free(imageData);
        return false;
    }

    const BmpChannel redChannel   = makeBmpChannel(redMask);
    const BmpChannel greenChannel = makeBmpChannel(greenMask);
    const BmpChannel blueChannel  = makeBmpChannel(blueMask);

    // Draw image into framebuffer.
    for (uint64_t fileY = 0; fileY < height; ++fileY)
    {
        // BMP normally stores rows bottom-up.
        const uint64_t sourceY =
            topDown ? fileY : height - 1 - fileY;

        const int64_t destinationY =
            static_cast<int64_t>(y) +
            static_cast<int64_t>(sourceY);

        // Skip invisible rows.
        if (destinationY < 0 ||
            destinationY >= s->height)
        {
            continue;
        }

        const uint8_t* row =
            imageData + fileY * rowStride;

        for (int64_t sourceX = firstX; sourceX < lastX; ++sourceX)
        {
            const int64_t destinationX =
                static_cast<int64_t>(x) + sourceX;

            const uint8_t* src =
                row + static_cast<size_t>(sourceX) * bytesPerPixel;

            uint8_t r, g, b;

            if (bytesPerPixel == 3)
            {
                // BMP stores BGR.
                b = src[0];
                g = src[1];
                r = src[2];
            }
            else
            {
                const uint32_t pixel = readBmpU32(src);

                r = readBmpChannel(redChannel, pixel);
                g = readBmpChannel(greenChannel, pixel);
                b = readBmpChannel(blueChannel, pixel);
            }

            const size_t destinationIndex =
                (
                    static_cast<size_t>(destinationY) *
                    static_cast<size_t>(s->width) +
                    static_cast<size_t>(destinationX)
                ) * 4;

            // Store pixels in RGBA format (images are opaque).
            s->pixels[destinationIndex]     = r;
            s->pixels[destinationIndex + 1] = g;
            s->pixels[destinationIndex + 2] = b;
            s->pixels[destinationIndex + 3] = 255;
        }
    }

    std::free(imageData);
    return true;
}

extern "C"
bool drawScreenImage(
    void* screen,
    const char* filename,
    int x,
    int y)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->pixels || !filename)
        return false;

    FILE* file = openFileUtf8(filename);

    if (!file)
        return false;

    const bool ok = drawBmpFromFile(s, file, x, y);

    std::fclose(file);
    return ok;
}

// ============================================================
// WINDOW PROPERTIES
// ============================================================

#ifdef _WIN32

static DWORD screenWindowStyle(const AppScreen* s)
{
    DWORD style = WS_OVERLAPPEDWINDOW;

    if (!s->resizable)
        style &= ~static_cast<DWORD>(WS_THICKFRAME | WS_MAXIMIZEBOX);

    return style;
}

// Resizes the window so that its client area is width x height.
static void setWindowClientSize(AppScreen* s, int width, int height)
{
    const DWORD style = static_cast<DWORD>(
        GetWindowLongPtrW(s->hwnd, GWL_STYLE));
    const DWORD exStyle = static_cast<DWORD>(
        GetWindowLongPtrW(s->hwnd, GWL_EXSTYLE));

    RECT rect{ 0, 0, width, height };
    AdjustWindowRectEx(&rect, style, FALSE, exStyle);

    SetWindowPos(
        s->hwnd,
        nullptr,
        0, 0,
        rect.right - rect.left,
        rect.bottom - rect.top,
        SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE
    );
}

#endif

#ifdef __linux__

// Tells the window manager to add or remove the fullscreen state.
static void sendX11Fullscreen(AppScreen* s, bool enable)
{
    XEvent ev{};

    ev.xclient.type = ClientMessage;
    ev.xclient.window = s->window;
    ev.xclient.message_type =
        XInternAtom(s->display, "_NET_WM_STATE", False);
    ev.xclient.format = 32;
    ev.xclient.data.l[0] = enable ? 1 : 0;   // _NET_WM_STATE_ADD / REMOVE
    ev.xclient.data.l[1] = static_cast<long>(
        XInternAtom(s->display, "_NET_WM_STATE_FULLSCREEN", False));
    ev.xclient.data.l[2] = 0;
    ev.xclient.data.l[3] = 1;                // source: normal application

    XSendEvent(
        s->display,
        DefaultRootWindow(s->display),
        False,
        SubstructureNotifyMask | SubstructureRedirectMask,
        &ev
    );

    XFlush(s->display);
}

#endif

#ifdef __APPLE__

// Applies the resizable flag to the window (style mask and zoom button).
static void applyMacStyle(AppScreen* s)
{
    if (!s || !s->window)
        return;

    NSWindowStyleMask mask = [s->window styleMask];

    // A window that is (or is becoming) fullscreen must keep the flag.
    const bool keep = s->resizable || s->fullscreen ||
                      (mask & NSWindowStyleMaskFullScreen) != 0;

    if (keep)
        mask |= NSWindowStyleMaskResizable;
    else
        mask &= ~NSWindowStyleMaskResizable;

    [s->window setStyleMask:mask];

    // Needed so that setScreenFullscreen can work for any window.
    [s->window setCollectionBehavior:
        [s->window collectionBehavior] |
        NSWindowCollectionBehaviorFullScreenPrimary];

    // The green button is the zoom / fullscreen button.
    [[s->window standardWindowButton:NSWindowZoomButton]
        setEnabled:s->resizable];
}

#endif

// Width of the drawable area (the framebuffer) in pixels; 0 for a null screen.
extern "C"
int getScreenWidth(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    return s ? s->width : 0;
}

// Height of the drawable area (the framebuffer) in pixels; 0 for a null screen.
extern "C"
int getScreenHeight(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    return s ? s->height : 0;
}

// Sets the window title (UTF-8). A null title becomes an empty title.
extern "C"
void setScreenTitle(void* screen, const char* title)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s)
        return;

    if (!storeTitle(s, title))
        return;

#ifdef _WIN32

    if (s->hwnd && IsWindow(s->hwnd))
    {
        wchar_t* wide = utf8ToWide(s->title);

        if (wide)
        {
            SetWindowTextW(s->hwnd, wide);
            std::free(wide);
        }
    }

#elif defined(__linux__)

    if (s->display && s->window)
    {
        applyX11Title(s);
        XFlush(s->display);
    }

#elif defined(__APPLE__)

    if (s->window)
    {
        @autoreleasepool
        {
            NSString* ns = [NSString stringWithUTF8String:s->title];

            [s->window setTitle:(ns ? ns : @"")];
        }
    }

#endif
}

// Returns the current title. The pointer stays valid until the next
// setScreenTitle call on this screen or until the screen is closed.
extern "C"
const char* getScreenTitle(void* screen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->title)
        return "";

    return s->title;
}

// Resizes the drawable area (client area) to width x height pixels.
// Ignored while the window is fullscreen.
extern "C"
void setScreenSize(void* screen, int width, int height)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running || width <= 0 || height <= 0 || s->fullscreen)
        return;

#ifdef _WIN32

    if (!s->hwnd || !IsWindow(s->hwnd))
        return;

    if (IsZoomed(s->hwnd))
        ShowWindow(s->hwnd, SW_RESTORE);

    // WM_SIZE resizes the framebuffer synchronously.
    setWindowClientSize(s, width, height);

#elif defined(__linux__)

    if (!s->display || !s->window)
        return;

    // Update the hints first so a fixed-size window accepts the new size.
    applyX11SizeHints(s, width, height);

    XResizeWindow(
        s->display,
        s->window,
        static_cast<unsigned int>(width),
        static_cast<unsigned int>(height)
    );

    XFlush(s->display);

#elif defined(__APPLE__)

    if (!s->window)
        return;

    @autoreleasepool
    {
        [s->window setContentSize:NSMakeSize(width, height)];
    }

#endif

    // Make the new size visible immediately instead of waiting for the
    // platform's resize notification (which then finds nothing left to do).
    resizeFramebuffer(s, width, height);
}

// Allows or forbids resizing the window by the user. Returns false if the
// window is not available. While fullscreen the flag is remembered and
// applied when fullscreen ends.
extern "C"
bool setScreenResizable(void* screen, bool resizable)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running)
        return false;

    s->resizable = resizable;

#ifdef _WIN32

    if (!s->hwnd || !IsWindow(s->hwnd))
        return false;

    if (s->fullscreen)
        return true;

    if (!resizable && IsZoomed(s->hwnd))
        ShowWindow(s->hwnd, SW_RESTORE);

    // Keep the client size; the frame width differs between the styles.
    RECT client{};
    GetClientRect(s->hwnd, &client);

    LONG_PTR style = GetWindowLongPtrW(s->hwnd, GWL_STYLE);
    const LONG_PTR bits = WS_THICKFRAME | WS_MAXIMIZEBOX;

    style = resizable ? (style | bits) : (style & ~bits);

    SetWindowLongPtrW(s->hwnd, GWL_STYLE, style);

    SetWindowPos(
        s->hwnd, nullptr, 0, 0, 0, 0,
        SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER |
        SWP_NOACTIVATE | SWP_FRAMECHANGED
    );

    setWindowClientSize(s, client.right, client.bottom);
    return true;

#elif defined(__linux__)

    if (!s->display || !s->window)
        return false;

    if (!s->fullscreen)
    {
        applyX11SizeHints(s, s->width, s->height);
        XFlush(s->display);
    }

    return true;

#elif defined(__APPLE__)

    if (!s->window)
        return false;

    @autoreleasepool
    {
        applyMacStyle(s);
    }

    return true;

#else

    return false;

#endif
}

// Enters or leaves fullscreen. Returns false if the request could not be
// made. On macOS the transition is animated and finishes asynchronously.
extern "C"
bool setScreenFullscreen(void* screen, bool fullscreen)
{
    AppScreen* s = static_cast<AppScreen*>(screen);

    if (!s || !s->running)
        return false;

#ifdef _WIN32

    if (!s->hwnd || !IsWindow(s->hwnd))
        return false;

    if (s->fullscreen == fullscreen)
        return true;

    if (fullscreen)
    {
        MONITORINFO mi{};
        mi.cbSize = sizeof(mi);

        s->savedPlacement.length = sizeof(WINDOWPLACEMENT);

        if (!GetWindowPlacement(s->hwnd, &s->savedPlacement) ||
            !GetMonitorInfoW(
                MonitorFromWindow(s->hwnd, MONITOR_DEFAULTTONEAREST), &mi))
        {
            return false;
        }

        SetWindowLongPtrW(s->hwnd, GWL_STYLE, WS_POPUP | WS_VISIBLE);

        SetWindowPos(
            s->hwnd, HWND_TOP,
            mi.rcMonitor.left,
            mi.rcMonitor.top,
            mi.rcMonitor.right - mi.rcMonitor.left,
            mi.rcMonitor.bottom - mi.rcMonitor.top,
            SWP_NOOWNERZORDER | SWP_FRAMECHANGED
        );

        s->fullscreen = true;
    }
    else
    {
        s->fullscreen = false;

        SetWindowLongPtrW(
            s->hwnd, GWL_STYLE, screenWindowStyle(s) | WS_VISIBLE);

        SetWindowPlacement(s->hwnd, &s->savedPlacement);

        SetWindowPos(
            s->hwnd, nullptr, 0, 0, 0, 0,
            SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER |
            SWP_NOOWNERZORDER | SWP_FRAMECHANGED
        );
    }

    return true;

#elif defined(__linux__)

    if (!s->display || !s->window)
        return false;

    if (s->fullscreen == fullscreen)
        return true;

    if (fullscreen)
    {
        // Remember the windowed size, drop any fixed-size hints (they would
        // fight the window manager) and ask for fullscreen.
        s->savedWidth = s->width;
        s->savedHeight = s->height;
        s->fullscreen = true;

        applyX11SizeHints(s, s->width, s->height);
        sendX11Fullscreen(s, true);
    }
    else
    {
        s->fullscreen = false;

        applyX11SizeHints(s, s->savedWidth, s->savedHeight);
        sendX11Fullscreen(s, false);
    }

    return true;

#elif defined(__APPLE__)

    if (!s->window)
        return false;

    if (s->fullscreen == fullscreen)
        return true;

    s->fullscreen = fullscreen;

    @autoreleasepool
    {
        // On exit the style is restored by windowDidExitFullScreen.
        if (fullscreen)
            applyMacStyle(s);

        [s->window toggleFullScreen:nil];
    }

    return true;

#else

    (void)fullscreen;
    return false;

#endif
}