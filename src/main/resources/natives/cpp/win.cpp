#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <string>
#include <new>
#include <limits>
#include <cstddef>
#include <cmath>
#include <fstream>
#include <vector>
#include <unordered_set>

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

#import <Cocoa/Cocoa.h>
#import <CoreGraphics/CoreGraphics.h>

#elif defined(__linux__)

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>

#endif

#ifdef __APPLE__
static void releaseAllPressedKeys();
@class ScreenWindowDelegate;
#endif

struct AppScreen
{
    int width = 0;
    int height = 0;

    bool running = false;

    int mouseX = 0;
    int mouseY = 0;

    // RGBA, 4 bytes per pixel, top-down.
    uint8_t* pixels = nullptr;

    uint8_t colorR = 255;
    uint8_t colorG = 255;
    uint8_t colorB = 255;
    uint8_t colorA = 255;

#ifdef _WIN32

    HWND hwnd = nullptr;
    HDC dc = nullptr;

#elif defined(__APPLE__)

    NSWindow* window = nil;
    ScreenWindowDelegate* delegate = nil;
#elif defined(__linux__)

    Display* display = nullptr;
    Window window = 0;
    GC gc = nullptr;
    Atom wmDelete = None;

#endif

    ~AppScreen()
    {
        delete[] pixels;
    }
};


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

    uint8_t* newPixels = new (std::nothrow) uint8_t[bytes];

    if (!newPixels)
        return false;

    std::memset(newPixels, 0, bytes);

    // Initialize every alpha channel as fully opaque.
    for (size_t i = 3; i < bytes; i += 4)
        newPixels[i] = 255;

    delete[] s->pixels;

    s->pixels = newPixels;
    s->width = width;
    s->height = height;

    return true;
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

@end

@interface ScreenView : NSView
{
@public
    AppScreen* owner;
}
@end

@implementation ScreenView

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

            CGContextTranslateCTM(
                context,
                0,
                NSHeight(bounds)
            );

            CGContextScaleCTM(context, 1, -1);

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

// Stores native key codes currently held down. These are the same key-code
// values delivered to registerKeyCallback on each platform.
static std::unordered_set<int> g_pressedKeys;

static void dispatchKeyEvent(int keyCode, bool pressed)
{
    if (pressed)
    {
        // Never allow allocation failure to escape through a native event loop.
        try
        {
            g_pressedKeys.insert(keyCode);
        }
        catch (...)
        {
        }
    }
    else
    {
        g_pressedKeys.erase(keyCode);
    }

    if (g_keyCallback)
        g_keyCallback(keyCode, pressed);
}

// Mouse buttons use the existing callback channel, but are not keyboard keys.
static void dispatchMouseButtonEvent(int buttonCode, bool pressed)
{
    if (g_keyCallback)
        g_keyCallback(buttonCode, pressed);
}

static void releaseAllPressedKeys()
{
    while (!g_pressedKeys.empty())
    {
        const int keyCode = *g_pressedKeys.begin();
        dispatchKeyEvent(keyCode, false);
    }
}

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
    return g_pressedKeys.find(keyCode) != g_pressedKeys.end();
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

        case WM_CLOSE:
            DestroyWindow(hwnd);
            return 0;

        case WM_DESTROY:
            if (s)
                s->running = false;
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
            dispatchKeyEvent((int)wParam, true);
            return 0;

        case WM_KEYUP:
            dispatchKeyEvent((int)wParam, false);
            return 0;

        case WM_SYSKEYDOWN:
            dispatchKeyEvent((int)wParam, true);
            return DefWindowProcW(hwnd, msg, wParam, lParam);

        case WM_SYSKEYUP:
            dispatchKeyEvent((int)wParam, false);
            return DefWindowProcW(hwnd, msg, wParam, lParam);

        case WM_LBUTTONDOWN:
            if (g_keyCallback)
                dispatchMouseButtonEvent(0, true);
            return 0;

        case WM_LBUTTONUP:
            if (g_keyCallback)
                dispatchMouseButtonEvent(0, false);
            return 0;

        case WM_RBUTTONDOWN:
            if (g_keyCallback)
                dispatchMouseButtonEvent(1, true);
            return 0;

        case WM_RBUTTONUP:
            if (g_keyCallback)
                dispatchMouseButtonEvent(1, false);
            return 0;

        case WM_MBUTTONDOWN:
            if (g_keyCallback)
                dispatchMouseButtonEvent(2, true);
            return 0;

        case WM_MBUTTONUP:
            if (g_keyCallback)
                dispatchMouseButtonEvent(2, false);
            return 0;
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

static void drawScreenToX11(AppScreen* s)
{
    if (!s || !s->display || !s->window ||
        !s->gc || !s->pixels)
        return;

    int screen = DefaultScreen(s->display);

    Visual* visual = DefaultVisual(s->display, screen);
    int depth = DefaultDepth(s->display, screen);

    XImage* image = XCreateImage(
        s->display,
        visual,
        depth,
        ZPixmap,
        0,
        nullptr,
        s->width,
        s->height,
        32,
        0
    );

    if (!image)
        return;

    image->data = static_cast<char*>(
        std::calloc(
            static_cast<size_t>(s->height),
            static_cast<size_t>(image->bytes_per_line)
        )
    );

    if (!image->data)
    {
        XDestroyImage(image);
        return;
    }

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

    XPutImage(
        s->display,
        s->window,
        s->gc,
        image,
        0, 0,
        0, 0,
        s->width,
        s->height
    );

    XDestroyImage(image);

    XFlush(s->display);
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

    AppScreen* s = new (std::nothrow) AppScreen();

    if (!s)
        return nullptr;

    s->running = true;

    if (!resizeFramebuffer(s, width, height))
    {
        delete s;
        return nullptr;
    }

    const char* windowTitle = title ? title : "Screen";

#ifdef _WIN32

    if (!registerScreenClass())
    {
        delete s;
        return nullptr;
    }

    int len = MultiByteToWideChar(
        CP_UTF8, 0, windowTitle, -1, nullptr, 0
    );

    if (len <= 0)
    {
        delete s;
        return nullptr;
    }

    std::wstring wideTitle(len, L'\0');

    MultiByteToWideChar(
        CP_UTF8, 0, windowTitle, -1,
        (LPWSTR) wideTitle.data(), len
    );

    RECT rect{ 0, 0, width, height };

    AdjustWindowRect(
        &rect,
        WS_OVERLAPPEDWINDOW,
        FALSE
    );

    s->hwnd = CreateWindowExW(
        0,
        L"NativeScreenWindow",
        wideTitle.c_str(),
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

    if (!s->hwnd)
    {
        delete s;
        return nullptr;
    }

    ShowWindow(s->hwnd, SW_SHOW);
    UpdateWindow(s->hwnd);


#elif defined(__linux__)

s->display = XOpenDisplay(nullptr);

if (!s->display)
{
    delete s;
    return nullptr;
}

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

    delete s;
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

XStoreName(s->display, s->window, windowTitle);

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

    delete s;
    return nullptr;
}

XMapWindow(s->display, s->window);
XFlush(s->display);


#elif defined(__APPLE__)

@autoreleasepool
{
    [NSApplication sharedApplication];

    [NSApp setActivationPolicy:
        NSApplicationActivationPolicyRegular];

    [NSApp finishLaunching];

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
        delete s;
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

        delete s;
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

        delete s;
        return nullptr;
    }

    view->owner = s;

    [s->window setContentView:view];

#if !__has_feature(objc_arc)
    [view release];
#endif

    [s->window setAcceptsMouseMovedEvents:YES];

    [s->window setTitle:nsTitle];
    [s->window makeKeyAndOrderFront:nil];

    [NSApp activateIgnoringOtherApps:YES];
}

#else

    delete s;
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

    delete s;
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

    MSG msg{};

    while (PeekMessageW(
        &msg,
        nullptr,
        0,
        0,
        PM_REMOVE))
    {
        if (msg.message == WM_QUIT)
        {
            s->running = false;
            continue;
        }

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
                {
                    KeySym key = XLookupKeysym(
                        &event.xkey,
                        0
                    );

                    dispatchKeyEvent((int)key, true);
                }
                break;
            }

            case KeyRelease:
            {
                {
                    KeySym key = XLookupKeysym(
                        &event.xkey,
                        0
                    );

                    dispatchKeyEvent((int)key, false);
                }
                break;
            }

            case ButtonPress:
            {
                if (!g_keyCallback)
                    break;

                switch (event.xbutton.button)
                {
                    case Button1:
                        dispatchMouseButtonEvent(0, true);
                        break;

                    case Button3:
                        dispatchMouseButtonEvent(1, true);
                        break;

                    case Button2:
                        dispatchMouseButtonEvent(2, true);
                        break;
                }
                break;
            }

            case ButtonRelease:
            {
                if (!g_keyCallback)
                    break;

                switch (event.xbutton.button)
                {
                    case Button1:
                        dispatchMouseButtonEvent(0, false);
                        break;

                    case Button3:
                        dispatchMouseButtonEvent(1, false);
                        break;

                    case Button2:
                        dispatchMouseButtonEvent(2, false);
                        break;
                }
                break;
            }

            case Expose:
                if (event.xexpose.count == 0)
                    drawScreenToX11(s);
                break;
        }
    }


#elif defined(__APPLE__)

    if (!s->window)
        return;

    @autoreleasepool
    {
        while (s->running)
        {
            NSEvent* event = [NSApp
                nextEventMatchingMask:NSEventMaskAny
                untilDate:[NSDate distantPast]
                inMode:NSDefaultRunLoopMode
                dequeue:YES
            ];

            if (!event)
                break;

            if ([event window] == s->window)
            {
                switch ([event type])
                {
                    case NSEventTypeMouseMoved:
                    case NSEventTypeLeftMouseDragged:
                    case NSEventTypeRightMouseDragged:
                    case NSEventTypeOtherMouseDragged:
                    {
                        NSPoint p = [[s->window contentView]
                            convertPoint:[event locationInWindow]
                            fromView:nil
                        ];

                        NSRect bounds = [[s->window contentView] bounds];

                        s->mouseX = (int)p.x;
                        s->mouseY = (int)(NSHeight(bounds) - p.y);
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

                    case NSEventTypeKeyDown:
                        dispatchKeyEvent((int)[event keyCode], true);
                        break;

                    case NSEventTypeKeyUp:
                        dispatchKeyEvent((int)[event keyCode], false);
                        break;

                    case NSEventTypeLeftMouseDown:
                        if (g_keyCallback)
                            dispatchMouseButtonEvent(0, true);
                        break;

                    case NSEventTypeLeftMouseUp:
                        if (g_keyCallback)
                            dispatchMouseButtonEvent(0, false);
                        break;

                    case NSEventTypeRightMouseDown:
                        if (g_keyCallback)
                            dispatchMouseButtonEvent(1, true);
                        break;

                    case NSEventTypeRightMouseUp:
                        if (g_keyCallback)
                            dispatchMouseButtonEvent(1, false);
                        break;

                    case NSEventTypeOtherMouseDown:
                        if ([event buttonNumber] == 2 && g_keyCallback)
                            dispatchMouseButtonEvent(2, true);
                        break;

                    case NSEventTypeOtherMouseUp:
                        if ([event buttonNumber] == 2 && g_keyCallback)
                            dispatchMouseButtonEvent(2, false);
                        break;

                    default:
                        break;
                }
            }

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

    for (std::size_t i = 0; i < pixelCount; ++i)
    {
        const std::size_t index = i * 4;

        s->pixels[index]     = s->colorR;
        s->pixels[index + 1] = s->colorG;
        s->pixels[index + 2] = s->colorB;
        s->pixels[index + 3] = s->colorA;
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
    s->pixels[index]     = s->colorR;
    s->pixels[index + 1] = s->colorG;
    s->pixels[index + 2] = s->colorB;
    s->pixels[index + 3] = s->colorA;
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

    s->pixels[index]     = s->colorR;
    s->pixels[index + 1] = s->colorG;
    s->pixels[index + 2] = s->colorB;
    s->pixels[index + 3] = s->colorA;
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

        s->pixels[index]     = s->colorR;
        s->pixels[index + 1] = s->colorG;
        s->pixels[index + 2] = s->colorB;
        s->pixels[index + 3] = s->colorA;

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

            s->pixels[index]     = s->colorR;
            s->pixels[index + 1] = s->colorG;
            s->pixels[index + 2] = s->colorB;
            s->pixels[index + 3] = s->colorA;
        }
    }
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

    // Reject circles completely outside the framebuffer.
    if (cx + r < 0 || cy + r < 0 ||
        cx - r >= s->width || cy - r >= s->height)
        return;

    int64_t dx = 0;
    int64_t dy = r;

    int64_t decision = 1 - r;

    while (dx <= dy)
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

            s->pixels[index]     = s->colorR;
            s->pixels[index + 1] = s->colorG;
            s->pixels[index + 2] = s->colorB;
            s->pixels[index + 3] = s->colorA;
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

        s->pixels[index]     = s->colorR;
        s->pixels[index + 1] = s->colorG;
        s->pixels[index + 2] = s->colorB;
        s->pixels[index + 3] = s->colorA;
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

    const int64_t originX = x;

    int64_t penX = x;
    int64_t penY = y;

    const int64_t maxValue =
        std::numeric_limits<int64_t>::max();

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

            if (penY > maxValue - 9)
                break;

            penY += 9;
            continue;
        }

        // Handle tabs (four character spaces).
        if (c == '\t')
        {
            if (penX > maxValue - 24)
                break;

            penX += 24;
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

        // Stop if additional text cannot become visible.
        if (penX >= s->width && penX >= originX)
            break;

        if (penY >= s->height)
            break;
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

    std::ifstream file(filename, std::ios::binary);

    if (!file)
        return false;

    // Read BMP file and DIB headers.
    uint8_t header[54]{};

    if (!file.read(
        reinterpret_cast<char*>(header),
        sizeof(header)))
    {
        return false;
    }

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
    if (bmpWidth <= 0 ||
        bmpHeight == 0 ||
        bmpHeight == INT32_MIN)
    {
        return false;
    }

    if (planes != 1)
        return false;

    // Support 24-bit and 32-bit BMP.
    if (bitsPerPixel != 24 &&
        bitsPerPixel != 32)
    {
        return false;
    }

    // Only uncompressed BMP files.
    if (compression != 0)
        return false;

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

    if (rowStride >
            std::numeric_limits<size_t>::max() ||
        imageDataSize >
            std::numeric_limits<size_t>::max() ||
        imageDataSize >
            static_cast<uint64_t>(
                std::numeric_limits<std::streamsize>::max()
            ))
    {
        return false;
    }

    // Check that the pixel data exists in the file.
    file.seekg(0, std::ios::end);

    const std::streampos endPosition = file.tellg();

    if (endPosition == std::streampos(-1))
        return false;

    const std::streamoff fileSize =
        static_cast<std::streamoff>(endPosition);

    if (fileSize < 0)
        return false;

    if (pixelOffset < 14ull + dibSize ||
        pixelOffset > static_cast<uint64_t>(fileSize))
    {
        return false;
    }

    if (imageDataSize >
        static_cast<uint64_t>(fileSize) - pixelOffset)
    {
        return false;
    }

    // Read all pixel data before modifying the framebuffer.
    std::vector<uint8_t> imageData;

    try
    {
        imageData.resize(
            static_cast<size_t>(imageDataSize)
        );
    }
    catch (...)
    {
        return false;
    }

    file.seekg(
        static_cast<std::streamoff>(pixelOffset),
        std::ios::beg
    );

    if (!file.read(
        reinterpret_cast<char*>(imageData.data()),
        static_cast<std::streamsize>(imageDataSize)))
    {
        return false;
    }

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
            imageData.data() + fileY * rowStride;

        for (uint64_t sourceX = 0;
             sourceX < width;
             ++sourceX)
        {
            const int64_t destinationX =
                static_cast<int64_t>(x) +
                static_cast<int64_t>(sourceX);

            // Clip against framebuffer boundaries.
            if (destinationX < 0 ||
                destinationX >= s->width)
            {
                continue;
            }

            const size_t sourceIndex =
                static_cast<size_t>(
                    sourceX * bytesPerPixel
                );

            const uint8_t b = row[sourceIndex];
            const uint8_t g = row[sourceIndex + 1];
            const uint8_t r = row[sourceIndex + 2];

            const size_t destinationIndex =
                (
                    static_cast<size_t>(destinationY) *
                    static_cast<size_t>(s->width) +
                    static_cast<size_t>(destinationX)
                ) * 4;

            // BMP uses BGR/BGRA.
            // Store pixels in RGBA format.
            s->pixels[destinationIndex]     = r;
            s->pixels[destinationIndex + 1] = g;
            s->pixels[destinationIndex + 2] = b;
            s->pixels[destinationIndex + 3] = 255;
        }
    }

    return true;
}