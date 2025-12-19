_IR """
; libc memory functions
declare i8* @malloc(i64)
declare void @free(i8*)
declare i8* @realloc(i8*, i64)
declare i8* @memcpy(i8*, i8*, i64)
declare i8* @memmove(i8*, i8*, i64)
declare i8* @memset(i8*, i32, i64)
"""

_Builtin func alloc(size: long) -> anyptr = """
entry:
    %ptr = call i8* @malloc(i64 %size)
    ret i8* %ptr
"""

_Builtin func free(pos: anyptr) -> none = """
entry:
    call void @free(i8* %pos)
    ret void
"""

_Builtin func realloc(pos: anyptr, size: long) -> anyptr = """
entry:
    %newptr = call i8* @realloc(i8* %pos, i64 %size)
    ret i8* %newptr
"""

_Builtin func memcpy(dest: anyptr, src: anyptr, size: long) -> anyptr = """
entry:
    %res = call i8* @memcpy(i8* %dest, i8* %src, i64 %size)
    ret i8* %res
"""

_Builtin func memmove(dest: anyptr, src: anyptr, size: long) -> anyptr = """
entry:
    %res = call i8* @memmove(i8* %dest, i8* %src, i64 %size)
    ret i8* %res
"""

_Builtin func memset(pos: anyptr, value: int, size: long) -> anyptr = """
entry:
    %res = call i8* @memset(i8* %pos, i32 %value, i64 %size)
    ret i8* %res
"""
