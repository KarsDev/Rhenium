_IR """
declare void @exit(i32)
"""

_Builtin func exit(code: int) -> none = """
entry:
    call void @exit(i32 %code)
    ret void
"""
