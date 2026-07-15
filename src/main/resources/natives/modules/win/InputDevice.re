_NativeCPP("win/win") \
    int getMouseX() and \
    int getMouseY() and \
    \
    bool isKeyPressed(keyCode: int) and \
    \
    none registerMouseMoveCallback(cb: lambda(int, int) -> none) and \
    none registerKeyCallback(cb: lambda(int, bool) -> none)
    