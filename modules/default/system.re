_IR """
declare void @exit(i32)
declare i32 @system(i8*)
"""

namespace System:
    // Exits the program with a precise exit code
    _Builtin func exit(code: int) -> none = """
    entry:
        call void @exit(i32 %code)
        ret void
    """

    // Executes an OS command by passing the specified command string to the system's command interpreter (shell)
    _Builtin func run(path: str) -> int = """
    entry:
        %result = call i32 @system(i8* %path)
        ret i32 %result
    """