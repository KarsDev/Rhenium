_IR """
; External declarations of standard C library memory functions.
; These tell the LLVM linker to look for these symbols in libc.
declare i8* @malloc(i64)          ; Allocate memory
declare void @free(i8*)           ; Deallocate memory
declare i8* @realloc(i8*, i64)    ; Resize allocated memory
declare i8* @memcpy(i8*, i8*, i64)  ; Copy memory (non-overlapping)
declare i8* @memmove(i8*, i8*, i64) ; Copy memory (safe for overlapping)
declare i8* @memset(i8*, i32, i64)  ; Fill memory with a constant byte
"""

// Allocates a block of memory of the given 'size' (in bytes).
// Returns an 'anyptr' (void*) to the start of the block.
_Builtin func alloc(size: long) -> anyptr = """
entry:
    ; Calls the C malloc function with the 64-bit size integer.
    %ptr = call i8* @malloc(i64 %size)
    ; Returns the resulting pointer to the caller.
    ret i8* %ptr
"""

// Releases the memory block pointed to by 'pos'.
// If 'pos' is null, no action is performed.
_Builtin func free(pos: anyptr) -> none = """
entry:
    ; Calls the C free function to deallocate the memory at this address.
    call void @free(i8* %pos)
    ret void
"""

// Resizes the memory block pointed to by 'pos' to 'size' bytes.
// It may move the block to a new address if the current location lacks space.
_Builtin func realloc(pos: anyptr, size: long) -> anyptr = """
entry:
    ; Calls C realloc; returns the pointer to the (potentially new) memory block.
    %newptr = call i8* @realloc(i8* %pos, i64 %size)
    ret i8* %newptr
"""

// Copies 'size' bytes from 'src' to 'dest'.
// Warning: 'src' and 'dest' must not overlap; otherwise, behavior is undefined.
_Builtin func memcpy(dest: anyptr, src: anyptr, size: long) -> anyptr = """
entry:
    ; Calls C memcpy to perform a fast bitwise copy.
    %res = call i8* @memcpy(i8* %dest, i8* %src, i64 %size)
    ret i8* %res
"""

// Copies 'size' bytes from 'src' to 'dest'.
// Safe to use even if the source and destination memory regions overlap.
_Builtin func memmove(dest: anyptr, src: anyptr, size: long) -> anyptr = """
entry:
    ; Calls C memmove which handles overlapping buffers by using a temporary buffer.
    %res = call i8* @memmove(i8* %dest, i8* %src, i64 %size)
    ret i8* %res
"""

// Fills the first 'size' bytes of the memory area pointed to by 'pos'
// with the constant byte 'value'.
_Builtin func memset(pos: anyptr, value: int, size: long) -> anyptr = """
entry:
    ; Calls C memset; 'value' is passed as an i32 but treated as an unsigned char.
    %res = call i8* @memset(i8* %pos, i32 %value, i64 %size)
    ret i8* %res
"""