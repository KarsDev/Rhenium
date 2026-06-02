_IR """
declare void @exit(i32)
declare i32 @system(i8*)
"""

_Builtin func exit(code: int) -> none = """
entry:
    call void @exit(i32 %code)
    ret void
"""

_Builtin func run(path: str) -> int = """
entry:
    %result = call i32 @system(i8* %path)
    ret i32 %result
"""