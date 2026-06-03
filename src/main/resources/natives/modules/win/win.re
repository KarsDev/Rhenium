using win.keys

_NativeCPP("win/win") int BWI_00(x: bool) and bool isKeyPressed(keyCode: int)

func getMouseX() -> int:
    return BWI_00(true)

func getMouseY() -> int:
    return BWI_00(false)

// use isKeyPressed(keyCode: int) to check for pressed keys (native implementation)