/// LLVM declarations used by the builtin I/O helpers
_IR """
declare i32 @strlen(i8*)
declare i32 @printf(i8*, ...)
declare i8* @malloc(i64)
declare i32 @getchar()

; Reusable format strings for printf.
@fmt_newline    = private constant [4 x i8] c"%s\0A\00"
@fmt_no_newline = private constant [3 x i8] c"%s\00"
"""

// Types that can be converted to a string for printing
trait Writeable:
    // Return a string representation of the value
    func toString() -> str

// Generic println for any type that can be converted to a string
generic func println<T inherits Writeable>(toWrite: T):
    println(toWrite.toString())

// Print a string followed by a newline
_Builtin func println(s: str) -> none = """
entry:
    ; Get a pointer to the "%s\n" format string.
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_newline, i32 0, i32 0

    ; Print the string using printf(format, value).
    call i32 (i8*, ...) @printf(i8* %fmt_ptr, i8* %s)

    ret void
"""

// Print a string without adding a newline
_Builtin func print(s: str) -> none = """
entry:
    ; Get a pointer to the "%s" format string.
    %fmt_ptr = getelementptr [3 x i8], [3 x i8]* @fmt_no_newline, i32 0, i32 0

    ; Print the string using printf(format, value).
    call i32 (i8*, ...) @printf(i8* %fmt_ptr, i8* %s)

    ret void
"""

// Read a line of input from stdin into a heap-allocated buffer
//
// Behavior:
// - Reads until newline or EOF
// - Stops before overflowing the buffer
// - Null-terminates the resulting string
// - Returns the allocated buffer
_Builtin func inputLocal() -> str = """
entry:
    ; Maximum number of bytes to read, including the final null terminator.
    %cap = alloca i32
    store i32 4096, i32* %cap

    ; Allocate the input buffer on the heap.
    %cap_val = load i32, i32* %cap
    %cap_i64 = zext i32 %cap_val to i64
    %buf = call i8* @malloc(i64 %cap_i64)

    ; Loop index: current write position in the buffer.
    %i_ptr = alloca i32
    store i32 0, i32* %i_ptr

    br label %read_loop

read_loop:
    ; Read one character from stdin.
    %ch = call i32 @getchar()

    ; Stop on EOF.
    %is_eof = icmp eq i32 %ch, -1
    br i1 %is_eof, label %finish, label %not_eof

not_eof:
    ; Stop on newline so we return a single line.
    %is_nl = icmp eq i32 %ch, 10        ; '\n' == 10
    br i1 %is_nl, label %finish, label %store_char

store_char:
    ; Load the current write index.
    %i = load i32, i32* %i_ptr

    ; Leave room for the terminating '\0'.
    %cap_minus1 = sub i32 %cap_val, 1
    %need_stop = icmp sge i32 %i, %cap_minus1
    br i1 %need_stop, label %finish, label %write_char

write_char:
    ; Write the character into buf[i].
    %ptr = getelementptr i8, i8* %buf, i32 %i
    %ch_trunc = trunc i32 %ch to i8
    store i8 %ch_trunc, i8* %ptr

    ; Advance the write index.
    %i_next = add i32 %i, 1
    store i32 %i_next, i32* %i_ptr

    br label %read_loop

finish:
    ; Null-terminate the string at the final write position.
    %final_i = load i32, i32* %i_ptr
    %term_ptr = getelementptr i8, i8* %buf, i32 %final_i
    store i8 0, i8* %term_ptr

    ; Return the heap-allocated C string.
    ret i8* %buf
"""

// print a prompt, then read one line of input
func input(s: str) -> str:
    print(s)
    return inputLocal()