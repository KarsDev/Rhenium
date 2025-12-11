_IR """
; Function declarations for IO
declare i32 @strlen(i8*)
declare i32 @printf(i8*, ...)
declare i8* @malloc(i64)
declare i32 @getchar()


; Format strings
@fmt_newline    = private constant [4 x i8] c"%s\0A\00"
@fmt_no_newline = private constant [3 x i8] c"%s\00"
"""


_Builtin func println(s: str) -> none = """
entry:
    ; Get pointer to the format string
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_newline, i32 0, i32 0

    ; Call printf(fmt, s)
    call i32 (i8*, ...) @printf(i8* %fmt_ptr, i8* %s)

    ret void
"""

_Builtin func print(s: str) -> none = """
entry:
    ; Get pointer to the format string
    %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @fmt_no_newline, i32 0, i32 0

    ; Call printf(fmt, s)
    call i32 (i8*, ...) @printf(i8* %fmt_ptr, i8* %s)

    ret void
"""

_Builtin func inputLocal() -> str = """
entry:
    ; capacity = 4096
    %cap = alloca i32
    store i32 4096, i32* %cap

    ; allocate buffer
    %cap_val = load i32, i32* %cap
    %cap_i64 = zext i32 %cap_val to i64
    %buf = call i8* @malloc(i64 %cap_i64)

    ; i = 0
    %i_ptr = alloca i32
    store i32 0, i32* %i_ptr

    br label %read_loop

read_loop:
    ; call getchar()
    %ch = call i32 @getchar()

    ; check EOF (-1)
    %is_eof = icmp eq i32 %ch, -1
    br i1 %is_eof, label %finish, label %not_eof

not_eof:
    ; if newline break
    %is_nl = icmp eq i32 %ch, 10        ; '\n' == 10
    br i1 %is_nl, label %finish, label %store_char

store_char:
    ; load i
    %i = load i32, i32* %i_ptr

    ; if i >= cap-1, stop (avoid overflow)
    %cap_minus1 = sub i32 %cap_val, 1
    %need_stop = icmp sge i32 %i, %cap_minus1
    br i1 %need_stop, label %finish, label %write_char

write_char:
    ; write char into buffer: buf[i] = (i8) ch
    %ptr = getelementptr i8, i8* %buf, i32 %i
    %ch_trunc = trunc i32 %ch to i8
    store i8 %ch_trunc, i8* %ptr

    ; i = i + 1
    %i_next = add i32 %i, 1
    store i32 %i_next, i32* %i_ptr

    br label %read_loop

finish:
    ; load final i
    %final_i = load i32, i32* %i_ptr

    ; buf[final_i] = 0
    %term_ptr = getelementptr i8, i8* %buf, i32 %final_i
    store i8 0, i8* %term_ptr

    ret i8* %buf
"""

func input(s: str) -> str:
    print(s)
    return inputLocal()