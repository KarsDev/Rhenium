struct MousePos {
    int x;
    int y;
};

MousePos getMousePosition();

#ifdef _WIN32

    #define WIN32_LEAN_AND_MEAN
    #include <windows.h>
    #pragma comment(lib, "user32.lib")

    MousePos getMousePosition() {
        POINT p;
        GetCursorPos(&p);
        return { 
            static_cast<int>(p.x),
            static_cast<int>(p.y) 
        };
    }

#elif defined(__APPLE__)

    #include <CoreGraphics/CoreGraphics.h>

    MousePos getMousePosition() {
        CGEventRef event = CGEventCreate(nullptr);
        CGPoint p = CGEventGetLocation(event);
        CFRelease(event);

        return {
            static_cast<int>(p.x),
            static_cast<int>(p.y)
        };
    }

#elif defined(__linux__)

    #include <X11/Xlib.h>

    MousePos getMousePosition() {
        Display* display = XOpenDisplay(nullptr);

        if (!display)
            return {0, 0};

        Window root = DefaultRootWindow(display);

        Window retRoot, retChild;
        int rootX, rootY;
        int winX, winY;
        unsigned int mask;

        XQueryPointer(
            display,
            root,
            &retRoot,
            &retChild,
            &rootX,
            &rootY,
            &winX,
            &winY,
            &mask
        );

        XCloseDisplay(display);

        return {rootX, rootY};
    }

#else

    MousePos getMousePosition() {
        return {0, 0};
    }

#endif

extern "C" {
    int BWI_00(bool x) {
        return x ? getMousePosition().x : getMousePosition().y;
    }

    bool isKeyPressed(int keyCode);
}

#include <iostream>

#if defined(_WIN32)
    #include <windows.h>
#elif defined(__linux__)
    #include <X11/Xlib.h>
    #include <X11/keysym.h>
#endif

bool isKeyPressed(int keyCode) {
#if defined(_WIN32)
    return (GetAsyncKeyState(keyCode) & 0x8000) != 0;

#elif defined(__linux__)
    Display* dpy = XOpenDisplay(NULL);
    if (!dpy) return false;

    char keys_return[32];
    XQueryKeymap(dpy, keys_return);
    
    KeyCode kc = XKeysymToKeycode(dpy, XK_A); 
    bool pressed = !!(keys_return[kc >> 3] & (1 << (kc & 7)));
    
    XCloseDisplay(dpy);
    return pressed;
#else
    return false;
#endif
}