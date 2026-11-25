_NativeCPP("ConsoleUtil") int BCU_00(option: int, param1: int, param2: int)

enum ConsoleColor:
    Black         = 30
    Red           = 31
    Green         = 32
    Yellow        = 33
    Blue          = 34
    Magenta       = 35
    Cyan          = 36
    White         = 37
    Default       = 39

namespace Console:
    func initConsole() -> bool:
        return BCU_00(0, 0, 0) == 1

    func isInteractive() -> bool:
        return BCU_00(1, 0, 0) == 1
    
    func hide() -> bool:
        return BCU_00(2, 0, 0) == 1
    
    func show() -> bool:
        return BCU_00(3, 0, 0) == 1
    
    func clear() -> none:
        BCU_00(4, 0, 0)

    func clearLine() -> none:
        BCU_00(5, 0, 0)
    
    func beep() -> none:
        BCU_00(6, 0, 0)
    
    func resetColor() -> none:
        BCU_00(7, 0, 0)
    
    func gotoXY(x: int, y: int) -> none:
        BCU_00(8, x, y)
    
    func setColor(color: ConsoleColor) -> none:
        BCU_00(9, color, 0)
    
    func getChar() -> int:
        return BCU_00(10, 0, 0)
    
    func getSizeX() -> int:
        return (BCU_00(11, 0, 0) >> 16) & 0xFFFF

    func getSizeY() -> int:
        return BCU_00(11, 0, 0) & 0xFFFF
        
