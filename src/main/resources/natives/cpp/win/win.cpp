#define UNICODE
#define NOMINMAX

#include <windows.h>
#include <windowsx.h>
#include <gdiplus.h>
#include <cstdint>
#include <string>
#include <new>
#include <algorithm>

#pragma comment(lib, "Gdi32.lib")
#pragma comment(lib, "User32.lib")
#pragma comment(lib, "Gdiplus.lib")

using MouseMoveCallback = void(*)(int, int);
using KeyCallback = void(*)(int, bool);

static MouseMoveCallback g_mouseMoveCallback = nullptr;
static KeyCallback g_keyCallback = nullptr;

extern "C"
{

int getMouseX()
{
    POINT p;
    GetCursorPos(&p);
    return p.x;
}

int getMouseY()
{
    POINT p;
    GetCursorPos(&p);
    return p.y;
}

bool isKeyPressed(int keyCode)
{
    return (GetAsyncKeyState(keyCode) & 0x8000) != 0;
}

void registerMouseMoveCallback(MouseMoveCallback cb)
{
    g_mouseMoveCallback = cb;
}

void registerKeyCallback(KeyCallback cb)
{
    g_keyCallback = cb;
}

}

using namespace Gdiplus;

struct Screen
{
    HWND hwnd = nullptr;
    HDC windowDC = nullptr;

    HDC backDC = nullptr;
    HBITMAP backBitmap = nullptr;
    HBITMAP oldBitmap = nullptr;

    int width = 0;
    int height = 0;

    COLORREF color = RGB(255, 255, 255);
    bool running = false;
};

static ULONG_PTR g_gdiplusToken = 0;
static bool g_gdiplusStarted = false;
static ATOM g_windowClassAtom = 0;

static std::wstring toWide(const char* text)
{
    if (!text)
        return L"";

    int len = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text, -1, nullptr, 0);
    if (len == 0)
        len = MultiByteToWideChar(CP_ACP, 0, text, -1, nullptr, 0);

    if (len <= 0)
        return L"";

    std::wstring out;
    out.resize(static_cast<size_t>(len - 1));

    if (MultiByteToWideChar(CP_UTF8, 0, text, -1, out.data(), len) == 0)
        MultiByteToWideChar(CP_ACP, 0, text, -1, out.data(), len);

    return out;
}

static void ensureGdiplus()
{
    if (g_gdiplusStarted)
        return;

    GdiplusStartupInput input;
    if (GdiplusStartup(&g_gdiplusToken, &input, nullptr) == Ok)
        g_gdiplusStarted = true;
}

static Screen* asScreen(void* p)
{
    return reinterpret_cast<Screen*>(p);
}

static void destroyBackBuffer(Screen* s)
{
    if (!s)
        return;

    if (s->backDC)
    {
        if (s->oldBitmap)
        {
            SelectObject(s->backDC, s->oldBitmap);
            s->oldBitmap = nullptr;
        }

        if (s->backBitmap)
        {
            DeleteObject(s->backBitmap);
            s->backBitmap = nullptr;
        }

        DeleteDC(s->backDC);
        s->backDC = nullptr;
    }
}

static bool createBackBuffer(Screen* s, int w, int h)
{
    if (!s || !s->windowDC || w <= 0 || h <= 0)
        return false;

    destroyBackBuffer(s);

    s->backDC = CreateCompatibleDC(s->windowDC);
    if (!s->backDC)
        return false;

    s->backBitmap = CreateCompatibleBitmap(s->windowDC, w, h);
    if (!s->backBitmap)
    {
        DeleteDC(s->backDC);
        s->backDC = nullptr;
        return false;
    }

    s->oldBitmap = (HBITMAP)SelectObject(s->backDC, s->backBitmap);
    if (!s->oldBitmap)
    {
        DeleteObject(s->backBitmap);
        s->backBitmap = nullptr;
        DeleteDC(s->backDC);
        s->backDC = nullptr;
        return false;
    }

    s->width = w;
    s->height = h;
    return true;
}

static void cleanupScreen(Screen* s)
{
    if (!s)
        return;

    destroyBackBuffer(s);

    if (s->windowDC && s->hwnd)
    {
        ReleaseDC(s->hwnd, s->windowDC);
        s->windowDC = nullptr;
    }

    s->hwnd = nullptr;
}

static bool registerWindowClass()
{
    if (g_windowClassAtom)
        return true;

    const wchar_t* className = L"WinWinScreenClass";

    WNDCLASSEXW wc{};
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = [](HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) -> LRESULT
    {
        Screen* s = reinterpret_cast<Screen*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));

        switch (msg)
        {
        case WM_NCCREATE:
        {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            Screen* screen = reinterpret_cast<Screen*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(screen));
            if (screen)
                screen->hwnd = hwnd;
            return DefWindowProcW(hwnd, msg, wParam, lParam);
        }

        case WM_SIZE:
        {
            if (s && s->windowDC)
            {
                int w = LOWORD(lParam);
                int h = HIWORD(lParam);
                if (w > 0 && h > 0)
                    createBackBuffer(s, w, h);
            }
            return 0;
        }

        case WM_CLOSE:
            DestroyWindow(hwnd);
            return 0;

        case WM_DESTROY:
            if (s)
                s->running = false;
            PostQuitMessage(0);
            return 0;
        case WM_MOUSEMOVE:
        {
            if (g_mouseMoveCallback)
                g_mouseMoveCallback(
                    GET_X_LPARAM(lParam),
                    GET_Y_LPARAM(lParam)
                );
            return 0;
        }

        case WM_KEYDOWN:
            if (g_keyCallback)
                g_keyCallback((int)wParam, true);
            return 0;

        case WM_KEYUP:
            if (g_keyCallback)
                g_keyCallback((int)wParam, false);
            return 0;

        case WM_LBUTTONDOWN:
            if (g_keyCallback)
                g_keyCallback(0, true);      // MOUSE_LEFT
            return 0;

        case WM_LBUTTONUP:
            if (g_keyCallback)
                g_keyCallback(0, false);
            return 0;

        case WM_RBUTTONDOWN:
            if (g_keyCallback)
                g_keyCallback(1, true);      // MOUSE_RIGHT
            return 0;

        case WM_RBUTTONUP:
            if (g_keyCallback)
                g_keyCallback(1, false);
            return 0;

        case WM_MBUTTONDOWN:
            if (g_keyCallback)
                g_keyCallback(2, true);      // MOUSE_MIDDLE
            return 0;

        case WM_MBUTTONUP:
            if (g_keyCallback)
                g_keyCallback(2, false);
            return 0;
        }

        return DefWindowProcW(hwnd, msg, wParam, lParam);
    };
    wc.hInstance = GetModuleHandleW(nullptr);
    wc.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wc.lpszClassName = className;

    g_windowClassAtom = RegisterClassExW(&wc);
    return g_windowClassAtom != 0;
}

static void selectSolidPenAndBrush(HDC dc, COLORREF color, HPEN& oldPenOut, HBRUSH& oldBrushOut, HPEN& penOut, HBRUSH brush)
{
    penOut = CreatePen(PS_SOLID, 1, color);
    oldPenOut = (HPEN)SelectObject(dc, penOut);
    oldBrushOut = (HBRUSH)SelectObject(dc, brush);
}

extern "C" {

void* createScreen(int width, int height, const char* title)
{
    if (width <= 0 || height <= 0)
        return nullptr;

    if (!registerWindowClass())
        return nullptr;

    ensureGdiplus();

    Screen* s = new (std::nothrow) Screen();
    if (!s)
        return nullptr;

    s->running = true;
    s->color = RGB(255, 255, 255);

    std::wstring wtitle = toWide(title ? title : "Screen");
    RECT rc{ 0, 0, width, height };

    DWORD style = WS_OVERLAPPEDWINDOW;
    AdjustWindowRect(&rc, style, FALSE);

    HWND hwnd = CreateWindowExW(
        0,
        L"WinWinScreenClass",
        wtitle.c_str(),
        style,
        CW_USEDEFAULT,
        CW_USEDEFAULT,
        rc.right - rc.left,
        rc.bottom - rc.top,
        nullptr,
        nullptr,
        GetModuleHandleW(nullptr),
        s
    );

    if (!hwnd)
    {
        delete s;
        return nullptr;
    }

    s->hwnd = hwnd;
    s->windowDC = GetDC(hwnd);
    if (!s->windowDC)
    {
        DestroyWindow(hwnd);
        delete s;
        return nullptr;
    }

    if (!createBackBuffer(s, width, height))
    {
        cleanupScreen(s);
        DestroyWindow(hwnd);
        delete s;
        return nullptr;
    }

    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);

    return s;
}

void closeScreen(void* screen)
{
    Screen* s = asScreen(screen);
    if (!s)
        return;

    s->running = false;

    if (s->hwnd && IsWindow(s->hwnd))
        DestroyWindow(s->hwnd);

    cleanupScreen(s);
    delete s;
}

bool isScreenRunning(void* screen)
{
    Screen* s = asScreen(screen);
    if (!s)
        return false;

    return s->running && s->hwnd && IsWindow(s->hwnd);
}

void pollScreenEvents(void* screen)
{
    Screen* s = asScreen(screen);
    if (!s)
        return;

    MSG msg;
    while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE))
    {
        if (msg.message == WM_QUIT)
            s->running = false;

        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
}

void presentScreen(void* screen)
{
    Screen* s = asScreen(screen);
    if (!s || !s->windowDC || !s->backDC)
        return;

    BitBlt(
        s->windowDC,
        0,
        0,
        s->width,
        s->height,
        s->backDC,
        0,
        0,
        SRCCOPY
    );
}

void clearScreen(void* screen)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    RECT r{ 0, 0, s->width, s->height };
    HBRUSH brush = CreateSolidBrush(s->color);
    FillRect(s->backDC, &r, brush);
    DeleteObject(brush);
}

void setScreenColor(void* screen, int r, int g, int b, int a)
{
    Screen* s = asScreen(screen);
    if (!s)
        return;

    (void)a; // alpha is ignored in GDI
    s->color = RGB(
        std::max(0, std::min(255, r)),
        std::max(0, std::min(255, g)),
        std::max(0, std::min(255, b))
    );
}

void drawScreenPixel(void* screen, int x, int y)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    SetPixel(s->backDC, x, y, s->color);
}

void drawScreenLine(void* screen, int x1, int y1, int x2, int y2)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HGDIOBJ oldPen = SelectObject(s->backDC, pen);

    MoveToEx(s->backDC, x1, y1, nullptr);
    LineTo(s->backDC, x2, y2);

    SelectObject(s->backDC, oldPen);
    DeleteObject(pen);
}

void drawScreenRect(void* screen, int x, int y, int width, int height)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HGDIOBJ oldPen = SelectObject(s->backDC, pen);
    HGDIOBJ oldBrush = SelectObject(s->backDC, GetStockObject(HOLLOW_BRUSH));

    Rectangle(s->backDC, x, y, x + width, y + height);

    SelectObject(s->backDC, oldBrush);
    SelectObject(s->backDC, oldPen);
    DeleteObject(pen);
}

void fillScreenRect(void* screen, int x, int y, int width, int height)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    RECT r{ x, y, x + width, y + height };
    HBRUSH brush = CreateSolidBrush(s->color);
    FillRect(s->backDC, &r, brush);
    DeleteObject(brush);
}

void drawScreenCircle(void* screen, int x, int y, int radius)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HGDIOBJ oldPen = SelectObject(s->backDC, pen);
    HGDIOBJ oldBrush = SelectObject(s->backDC, GetStockObject(HOLLOW_BRUSH));

    Ellipse(s->backDC, x - radius, y - radius, x + radius, y + radius);

    SelectObject(s->backDC, oldBrush);
    SelectObject(s->backDC, oldPen);
    DeleteObject(pen);
}

void fillScreenCircle(void* screen, int x, int y, int radius)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HBRUSH brush = CreateSolidBrush(s->color);

    HGDIOBJ oldPen = SelectObject(s->backDC, pen);
    HGDIOBJ oldBrush = SelectObject(s->backDC, brush);

    Ellipse(s->backDC, x - radius, y - radius, x + radius, y + radius);

    SelectObject(s->backDC, oldBrush);
    SelectObject(s->backDC, oldPen);

    DeleteObject(brush);
    DeleteObject(pen);
}

void drawScreenTriangle(void* screen,
                        int x1, int y1,
                        int x2, int y2,
                        int x3, int y3)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    POINT pts[3] = {
        { x1, y1 },
        { x2, y2 },
        { x3, y3 }
    };

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HGDIOBJ oldPen = SelectObject(s->backDC, pen);
    HGDIOBJ oldBrush = SelectObject(s->backDC, GetStockObject(HOLLOW_BRUSH));

    Polygon(s->backDC, pts, 3);

    SelectObject(s->backDC, oldBrush);
    SelectObject(s->backDC, oldPen);
    DeleteObject(pen);
}

void fillScreenTriangle(void* screen,
                        int x1, int y1,
                        int x2, int y2,
                        int x3, int y3)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC)
        return;

    POINT pts[3] = {
        { x1, y1 },
        { x2, y2 },
        { x3, y3 }
    };

    HPEN pen = CreatePen(PS_SOLID, 1, s->color);
    HBRUSH brush = CreateSolidBrush(s->color);

    HGDIOBJ oldPen = SelectObject(s->backDC, pen);
    HGDIOBJ oldBrush = SelectObject(s->backDC, brush);

    Polygon(s->backDC, pts, 3);

    SelectObject(s->backDC, oldBrush);
    SelectObject(s->backDC, oldPen);

    DeleteObject(brush);
    DeleteObject(pen);
}

void drawScreenText(void* screen, const char* text, int x, int y)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC || !text)
        return;

    std::wstring wtext = toWide(text);

    SetTextColor(s->backDC, s->color);
    SetBkMode(s->backDC, TRANSPARENT);

    HFONT font = (HFONT)GetStockObject(DEFAULT_GUI_FONT);
    HGDIOBJ oldFont = SelectObject(s->backDC, font);

    TextOutW(s->backDC, x, y, wtext.c_str(), (int)wtext.size());

    SelectObject(s->backDC, oldFont);
}

bool drawScreenImage(void* screen, const char* filename, int x, int y)
{
    Screen* s = asScreen(screen);
    if (!s || !s->backDC || !filename)
        return false;

    ensureGdiplus();
    std::wstring wfile = toWide(filename);
    if (wfile.empty())
        return false;

    Bitmap image(wfile.c_str());
    if (image.GetLastStatus() != Ok)
        return false;

    Graphics g(s->backDC);
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.DrawImage(&image, x, y);

    return true;
}

} // extern "C"