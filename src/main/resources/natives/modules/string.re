_IR """
; Function declarations for string manipulation
declare i32 @strlen(i8*)
declare void @memcpy(i8*, i8*, i64)
declare i8* @malloc(i64)
declare i32 @strcmp(i8*)

; Replacement for sprintf
declare i32 @sprintf_external(i8*, i64, i8*, ...)

; Format strings for conversion
; Without newline (for to string)
@fmt_int = private constant [3 x i8] c"%d\00"
@fmt_long = private constant [4 x i8] c"%ld\00"
@fmt_float = private constant [3 x i8] c"%f\00"
@fmt_double = private constant [7 x i8] c"%0.16f\00"
@fmt_bool = private constant [3 x i8] c"%s\00"

@true_str = private constant [5 x i8] c"true\00"
@false_str = private constant [6 x i8] c"false\00"
"""

_NativeCPP("defaults") _Builtin

_Builtin func byteToStr(b: byte) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %b_int = sext i8 %b to i32
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 12, i8* %fmt_ptr, i32 %b_int)
    ret i8* %buf
"""

_Builtin func shortToStr(s: short) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %s_int = sext i16 %s to i32
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 12, i8* %fmt_ptr, i32 %s_int)
    ret i8* %buf
"""

_Builtin func intToStr(i: int) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 12, i8* %fmt_ptr, i32 %i)
    ret i8* %buf
"""

_Builtin func longToStr(l: long) -> str = """
entry:
    %buf = call i8* @malloc(i64 24)
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_long, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 24, i8* %fmt_ptr, i64 %l)
    ret i8* %buf
"""

_Builtin func floatToStr(f: float) -> str = """
entry:
    %buf = call i8* @malloc(i64 32)
    %f_double = fpext float %f to double
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_float, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 32, i8* %fmt_ptr, double %f_double)
    ret i8* %buf
"""

_Builtin func doubleToStr(d: double) -> str = """
entry:
    %buf = call i8* @malloc(i64 32)
    %fmt_ptr = getelementptr [7 x i8], [7 x i8]* @fmt_double, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 32, i8* %fmt_ptr, double %d)
    ret i8* %buf
"""

_Builtin func boolToStr(b: bool) -> str = """
entry:
    %sel = select i1 %b,
        i8* getelementptr([5 x i8], [5 x i8]* @true_str, i32 0, i32 0),
        i8* getelementptr([6 x i8], [6 x i8]* @false_str, i32 0, i32 0)
    ret i8* %sel
"""

_Builtin func charToStr(c: char) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %c_int = sext i8 %c to i32
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(
        i8* %buf, i64 12, i8* %fmt_ptr, i32 %c_int)
    ret i8* %buf
"""

_Builtin func charToStrAscii(c: char) -> str = """
entry:
    ; Allocate 2 bytes: one for the char, one for the null terminator
    %buf = call i8* @malloc(i64 2)

    ; Store the char directly
    store i8 %c, i8* %buf

    ; Write null terminator
    %nullpos = getelementptr i8, i8* %buf, i32 1
    store i8 0, i8* %nullpos

    ret i8* %buf
"""

_Builtin func strConcat(s1: str, s2: str) -> str = """
entry:
    %len1 = call i32 @strlen(i8* %s1)
    %len2 = call i32 @strlen(i8* %s2)
    %sum = add i32 %len1, %len2
    %size = add i32 %sum, 1
    %size64 = zext i32 %size to i64
    %buf = call i8* @malloc(i64 %size64)
    call void @memcpy(i8* %buf, i8* %s1, i32 %len1)
    %dest = getelementptr i8, i8* %buf, i32 %len1
    call void @memcpy(i8* %dest, i8* %s2, i32 %len2)
    %end = getelementptr i8, i8* %buf, i32 %sum
    store i8 0, i8* %end
    ret i8* %buf
"""

_Builtin func strcmp_eq(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %eq = icmp eq i32 %cmp, 0
    ret i1 %eq
"""

_Builtin func strcmp_neq(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %neq = icmp ne i32 %cmp, 0
    ret i1 %neq
"""