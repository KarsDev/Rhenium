_IR """
; External C function declarations for memory and string handling
declare i32 @strlen(i8*)               ; Returns length of string
declare i8* @malloc(i64)               ; Allocates memory on the heap
declare i32 @strcmp(i8*, i8*)           ; Compares two strings
declare i8* @memcpy(i8*, i8*, i64)     ; Copies memory blocks

; Custom external wrapper for sprintf to handle variable arguments safely
declare i32 @sprintf_external(i8*, i64, i8*, ...)

; Global format strings used by sprintf
@fmt_int = private constant [3 x i8] c"%d\00"
@fmt_long = private constant [4 x i8] c"%ld\00"
@fmt_float = private constant [3 x i8] c"%f\00"
@fmt_double = private constant [7 x i8] c"%0.16f\00"
@fmt_bool = private constant [3 x i8] c"%s\00"
@fmt_ptr_str = private constant [5 x i8] c"0x%p\00"

; Boolean literals
@true_str = private constant [5 x i8] c"true\00"
@false_str = private constant [6 x i8] c"false\00"
"""

_NativeCPP("defaults") _Builtin

// Converts a byte (i8) to a string representation of its numeric value
_Builtin global func byteToStr(b: byte) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)            ; Buffer for max i32 string length
    %b_int = sext i8 %b to i32                 ; Sign-extend byte to 32-bit int
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 12, i8* %fmt_ptr, i32 %b_int)
    ret i8* %buf
"""

// Converts a short (i16) to its numeric string representation
_Builtin global func shortToStr(s: short) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %s_int = sext i16 %s to i32                ; Sign-extend short to 32-bit int
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 12, i8* %fmt_ptr, i32 %s_int)
    ret i8* %buf
"""

// Converts a standard 32-bit integer to a string
_Builtin global func intToStr(i: int) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 12, i8* %fmt_ptr, i32 %i)
    ret i8* %buf
"""

// Converts a 64-bit long to a string
_Builtin global func longToStr(l: long) -> str = """
entry:
    %buf = call i8* @malloc(i64 24)            ; Larger buffer for 64-bit integers
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_long, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 24, i8* %fmt_ptr, i64 %l)
    ret i8* %buf
"""

// Converts a float to a string (promoted to double for varargs)
_Builtin global func floatToStr(f: float) -> str = """
entry:
    %buf = call i8* @malloc(i64 32)
    %f_double = fpext float %f to double       ; Variadic functions require float -> double promotion
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_float, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 32, i8* %fmt_ptr, double %f_double)
    ret i8* %buf
"""

// Converts a double to a string with 16 decimal places
_Builtin global func doubleToStr(d: double) -> str = """
entry:
    %buf = call i8* @malloc(i64 32)
    %fmt_ptr = getelementptr [7 x i8], [7 x i8]* @fmt_double, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 32, i8* %fmt_ptr, double %d)
    ret i8* %buf
"""

// Returns a pointer to the static "true" or "false" string constants
_Builtin global func boolToStr(b: bool) -> str = """
entry:
    %sel = select i1 %b, i8* getelementptr([5 x i8], [5 x i8]* @true_str, i32 0, i32 0), i8* getelementptr([6 x i8], [6 x i8]* @false_str, i32 0, i32 0)
    ret i8* %sel
"""

// Converts a character (numeric value) to its integer string representation
_Builtin global func charToStr(c: char) -> str = """
entry:
    %buf = call i8* @malloc(i64 12)
    %c_int = sext i8 %c to i32
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_int, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 12, i8* %fmt_ptr, i32 %c_int)
    ret i8* %buf
"""

// Converts a character to a 1-character string (ASCII)
_Builtin global func charToStrAscii(c: char) -> str = """
entry:
    %buf = call i8* @malloc(i64 2)             ; 1 byte for char + 1 for null terminator
    store i8 %c, i8* %buf                      ; Place char at index 0
    %nullpos = getelementptr i8, i8* %buf, i32 1
    store i8 0, i8* %nullpos                   ; Place null terminator at index 1
    ret i8* %buf
"""

// Converts a memory address (pointer) to a hexadecimal string
_Builtin global func ptrToStr(p: anyptr) -> str = """
entry:
    %buf = call i8* @malloc(i64 18)            ; Enough for "0x" + 16 hex chars + null
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_ptr_str, i32 0, i32 0
    call i32 (i8*, i64, i8*, ...) @sprintf_external(i8* %buf, i64 18, i8* %fmt_ptr, ptr %p)
    ret i8* %buf
"""

// Concatenates two strings by allocating a new buffer and copying both
_Builtin global func strConcat(s1: str, s2: str) -> str = """
entry:
    %len1 = call i32 @strlen(i8* %s1)
    %len2 = call i32 @strlen(i8* %s2)
    %sum = add i32 %len1, %len2
    %size = add i32 %sum, 1                    ; +1 for null terminator
    %size64 = zext i32 %size to i64
    %buf = call i8* @malloc(i64 %size64)
    
    call i8* @memcpy(i8* %buf, i8* %s1, i32 %len1) ; Copy first string
    %dest = getelementptr i8, i8* %buf, i32 %len1
    call i8* @memcpy(i8* %dest, i8* %s2, i32 %len2) ; Copy second string at offset
    
    %end = getelementptr i8, i8* %buf, i32 %sum
    store i8 0, i8* %end                       ; Set null terminator
    ret i8* %buf
"""

// Returns true if strings are equal
_Builtin global func strcmp_eq(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %eq = icmp eq i32 %cmp, 0
    ret i1 %eq
"""

// Returns true if strings are not equal
_Builtin global func strcmp_neq(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %neq = icmp ne i32 %cmp, 0
    ret i1 %neq
"""