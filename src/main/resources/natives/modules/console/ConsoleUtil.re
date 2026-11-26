_NativeCPP("ConsoleUtil") int BCU_00(option: int, param1: int, param2: int)

/*
 Console Color enum
 Used for Console::setConsoleColor(ConsoleColor)
*/
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
    
    // Initializes the Console Library. Call this before any of the following functions
    func initConsole() -> bool:
        return BCU_00(0, 0, 0) == 1

    // Returns whether the console is interactive (user can type inputs)
    func isInteractive() -> bool:
        return BCU_00(1, 0, 0) == 1
    
    // Hides the console window
    func hide() -> bool:
        return BCU_00(2, 0, 0) == 1
    
    // Shows the console window
    func show() -> bool:
        return BCU_00(3, 0, 0) == 1
    
    // Clears the console output
    func clear() -> none:
        BCU_00(4, 0, 0)

    // Clears a line in the console output
    func clearLine() -> none:
        BCU_00(5, 0, 0)
    
    // Plays a beep sound from the console
    func beep() -> none:
        BCU_00(6, 0, 0)
    
    // Resets the console text color
    func resetColor() -> none:
        BCU_00(7, 0, 0)
    
    // Goes to a specific line and char in the console output
    func gotoXY(x: int, y: int) -> none:
        BCU_00(8, x, y)
    
    // Sets the console text color
    func setColor(color: ConsoleColor) -> none:
        BCU_00(9, color, 0)
    
    // Gets a user input char
    func getChar() -> int:
        return BCU_00(10, 0, 0)
    
    // Gets console X size
    func getSizeX() -> int:
        return (BCU_00(11, 0, 0) >> 16) & 0xFFFF

    // Gets console Y size
    func getSizeY() -> int:
        return BCU_00(11, 0, 0) & 0xFFFF
        
