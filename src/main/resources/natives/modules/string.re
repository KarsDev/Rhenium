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
_Builtin global func strEquals(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %eq = icmp eq i32 %cmp, 0
    ret i1 %eq
"""

// Returns true if strings are not equal
_Builtin global func strNotEquals(s1: str, s2: str) -> bool = """
entry:
    %cmp = call i32 @strcmp(i8* %s1, i8* %s2)
    %neq = icmp ne i32 %cmp, 0
    ret i1 %neq
"""

// Splits a string into an array of single-character strings
_Builtin func strSplit(s: str) -> arr -> str = """
entry:
    %len = call i32 @strlen(i8* %s)
    %arr = call i8** @malloc(i64 mul i64 (zext i32 %len to i64), 8) ; array of pointers

    %i = alloca i32
    store i32 0, i32* %i

    br label %loop

loop:
    %idx = load i32, i32* %i
    %cmp = icmp slt i32 %idx, %len
    br i1 %cmp, label %body, label %end

body:
    %char_ptr = call i8* @malloc(i64 2)         ; single char + null
    %char_val = call i8* getelementptr(i8, i8* %s, i32 %idx)
    %val = load i8, i8* %char_val
    store i8 %val, i8* %char_ptr
    %nullpos = getelementptr i8, i8* %char_ptr, i32 1
    store i8 0, i8* %nullpos

    %arr_elem_ptr = getelementptr i8*, i8** %arr, i32 %idx
    store i8* %char_ptr, i8** %arr_elem_ptr

    %next = add i32 %idx, 1
    store i32 %next, i32* %i
    br label %loop

end:
    ret i8** %arr
"""

// Returns a byte array of the string
_Builtin func strGetBytes(s: str) -> arr -> byte = """
entry:
    %len = call i32 @strlen(i8* %s)
    %arr = call i8* @malloc(i64 (zext i32 %len to i64))
    
    %i = alloca i32
    store i32 0, i32* %i
    br label %loop

loop:
    %idx = load i32, i32* %i
    %cmp = icmp slt i32 %idx, %len
    br i1 %cmp, label %body, label %end

body:
    %c_ptr = getelementptr i8, i8* %s, i32 %idx
    %c_val = load i8, i8* %c_ptr
    %dst = getelementptr i8, i8* %arr, i32 %idx
    store i8 %c_val, i8* %dst
    %next = add i32 %idx, 1
    store i32 %next, i32* %i
    br label %loop

end:
    ret i8* %arr
"""

// Returns index of first occurrence of val in s, -1 if not found
_Builtin func strIndexOf(s: str, val: str) -> int = """
entry:
    %len_s = call i32 @strlen(i8* %s)
    %len_val = call i32 @strlen(i8* %val)
    %i = alloca i32
    store i32 0, i32* %i
    %found = alloca i1
    store i1 false, i1* %found
    %index = alloca i32
    store i32 -1, i32* %index

    br label %loop

loop:
    %idx = load i32, i32* %i
    %cmp = icmp sle i32 %idx, sub i32 %len_s, %len_val
    br i1 %cmp, label %body, label %end

body:
    %s_ptr = getelementptr i8, i8* %s, i32 %idx
    %eq = call i32 @strncmp(i8* %s_ptr, i8* %val, i32 %len_val)
    %is_eq = icmp eq i32 %eq, 0
    br i1 %is_eq, label %found_label, label %next

found_label:
    store i1 true, i1* %found
    store i32 %idx, i32* %index
    br label %end

next:
    %next_idx = add i32 %idx, 1
    store i32 %next_idx, i32* %i
    br label %loop

end:
    %res = load i32, i32* %index
    ret i32 %res
"""

// Returns substring from begin to end-1
_Builtin func strSub(begin: int, end: int) -> str = """
entry:
    %len = sub i32 %end, %begin
    %buf = call i8* @malloc(i64 add (zext i32 %len to i64), 1)
    %src_ptr = getelementptr i8, i8* %s, i32 %begin
    call i8* @memcpy(i8* %buf, i8* %src_ptr, i32 %len)
    %nullpos = getelementptr i8, i8* %buf, i32 %len
    store i8 0, i8* %nullpos
    ret i8* %buf
"""

// Returns substring from begin to end of string
_Builtin func strSub(begin: int) -> str = """
entry:
    %len_s = call i32 @strlen(i8* %s)
    %len = sub i32 %len_s, %begin
    %buf = call i8* @malloc(i64 add (zext i32 %len to i64), 1)
    %src_ptr = getelementptr i8, i8* %s, i32 %begin
    call i8* @memcpy(i8* %buf, i8* %src_ptr, i32 %len)
    %nullpos = getelementptr i8, i8* %buf, i32 %len
    store i8 0, i8* %nullpos
    ret i8* %buf
"""

// Trims leading whitespace
_Builtin func strTrim(begin: int) -> str = """
entry:
    %len = call i32 @strlen(i8* %s)
    %start = alloca i32
    store i32 %begin, i32* %start

loop:
    %i = load i32, i32* %start
    %cmp = icmp slt i32 %i, %len
    br i1 %cmp, label %check, label %end

check:
    %c_ptr = getelementptr i8, i8* %s, i32 %i
    %c_val = load i8, i8* %c_ptr
    %is_space = icmp eq i8 %c_val, 32
    %is_tab = icmp eq i8 %c_val, 9
    %sp_or_tab = or i1 %is_space, %is_tab
    br i1 %sp_or_tab, label %inc, label %done

inc:
    %next = add i32 %i, 1
    store i32 %next, i32* %start
    br label %loop

done:
    %new_begin = load i32, i32* %start
    %sub = call strSub(i32 %new_begin)
    ret i8* %sub

end:
    %empty = call i8* @malloc(i64 1)
    store i8 0, i8* %empty
    ret i8* %empty
"""

// Strips trailing whitespace
_Builtin func strStrip(begin: int) -> str = """
entry:
    %len = call i32 @strlen(i8* %s)
    %end = alloca i32
    store i32 %len, i32* %end

loop:
    %i = load i32, i32* %end
    %cmp = icmp sgt i32 %i, %begin
    br i1 %cmp, label %check, label %done

check:
    %c_ptr = getelementptr i8, i8* %s, i32 sub(i32 %i, 1)
    %c_val = load i8, i8* %c_ptr
    %is_space = icmp eq i8 %c_val, 32
    %is_tab = icmp eq i8 %c_val, 9
    %sp_or_tab = or i1 %is_space, %is_tab
    br i1 %sp_or_tab, label %dec, label %done

dec:
    %prev = sub i32 %i, 1
    store i32 %prev, i32* %end
    br label %loop

done:
    %new_end = load i32, i32* %end
    %sub = call strSub(i32 %begin, i32 %new_end)
    ret i8* %sub
"""