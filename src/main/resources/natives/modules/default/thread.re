// Include the native C++ thread file with builtin functions
_NativeCPP("thread") _Builtin

// Declare the external join function for the IR to use
_IR """
declare i8* @rhenium_join(i8*) ; join a rhenium thread
"""

// Declare struct Thread used by the compiler
struct Thread:
    handle: anyptr

impl Thread:
    // Executes the thread
    _Builtin func join() -> anyptr = """
        ; 1. Load the 'handle' field.
        ; Index 0, 0 gets the first field of the %struct.Thread (the i8*)
        %handle_ptr = getelementptr %struct.Thread, %struct.Thread* %self, i32 0, i32 0
        %handle = load i8*, i8** %handle_ptr

        ; 2. Call the C++ backend
        %result = call i8* @rhenium_join(i8* %handle)

        ; 3. Return the result
        ret i8* %result
    """