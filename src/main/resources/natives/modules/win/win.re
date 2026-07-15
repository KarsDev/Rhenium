using win.keys

_NativeCPP("win/win") \
    anyptr createScreen(width: int, height: int, title: str) and \
    none closeScreen(screen: anyptr) and \
    bool isScreenRunning(screen: anyptr) and \
    none pollScreenEvents(screen: anyptr) and \
    none presentScreen(screen: anyptr) and \
    \
    none clearScreen(screen: anyptr) and \
    none setScreenColor(screen: anyptr, r: int, g: int, b: int, a: int) and \
    \
    none drawScreenPixel(screen: anyptr, x: int, y: int) and \
    \
    none drawScreenLine(  \
        screen: anyptr,   \
        x1: int, y1: int, \
        x2: int, y2: int  \
    ) and \
    \
    none drawScreenRect(        \
        screen: anyptr,         \
        x: int, y: int,         \
        width: int, height: int \
    ) and \
    \
    none fillScreenRect(        \
        screen: anyptr,         \
        x: int, y: int,         \
        width: int, height: int \
    ) and \
    \
    none drawScreenCircle( \
        screen: anyptr,    \
        x: int, y: int,    \
        radius: int        \
    ) and \
    \
    none fillScreenCircle( \
        screen: anyptr,    \
        x: int, y: int,    \
        radius: int        \
    ) and \
    \
    none drawScreenTriangle( \
        screen: anyptr,      \
        x1: int, y1: int,    \
        x2: int, y2: int,    \
        x3: int, y3: int     \
    ) and \
    \
    none fillScreenTriangle( \
        screen: anyptr,      \
        x1: int, y1: int,    \
        x2: int, y2: int,    \
        x3: int, y3: int     \
    ) and \
    \
    none drawScreenText( \
        screen: anyptr,  \
        text: str,       \
        x: int,          \
        y: int           \
    ) and \
    \
    bool drawScreenImage( \
        screen: anyptr,   \
        filename: str,    \
        x: int,           \
        y: int            \
    )