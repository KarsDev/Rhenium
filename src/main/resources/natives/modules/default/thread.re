// Include the native C++ thread file with builtin functions
_NativeCPP("thread") _Builtin

// Declare the external join function for the IR to use
_IR """
declare void @rhenium_run(i8*)     ; run the thread
declare i8* @rhenium_await(i8*)    ; awaits the result
declare i8* @rhenium_destroy(i8*)  ; destroys the thread handle
"""

// Declare struct Thread used by the compiler
_Builtin struct Thread:
    handle: anyptr

impl Thread:
    // Executes the thread
    // Please remember that if the program stops after calling run() it probably won't execute the thread
    _Builtin func run() -> none = """
        ; 1. Load the 'handle' field.
        %handle_ptr = getelementptr %struct.Thread_anyptr, %struct.Thread_anyptr* %self, i32 0, i32 0
        %handle = load i8*, i8** %handle_ptr

        ; 2. Call the C++ backend
        call void @rhenium_run(i8* %handle)

        ret void
    """

    // Runs and awaits for the result
    _Builtin func await() -> anyptr = """
        ; 1. Load the 'handle' field.
        %handle_ptr = getelementptr %struct.Thread_anyptr, %struct.Thread_anyptr* %self, i32 0, i32 0
        %handle = load i8*, i8** %handle_ptr

        ; 2. Call the C++ backend
        %result = call i8* @rhenium_await(i8* %handle)

        ; 3. Return the result
        ret i8* %result
    """

    // Destroyes the thread, call this to not have code leaks
    _Builtin func destroy() -> none = """
        ; 1. Load the 'handle' field.
        %handle_ptr = getelementptr %struct.Thread_anyptr, %struct.Thread_anyptr* %self, i32 0, i32 0
        %handle = load i8*, i8** %handle_ptr

        ; 2. Call the C++ backend
        call void @rhenium_destroy(i8* %handle)

        ret void
    """

    // Runs the thread and destroys safely
    func runAndDestroy() -> none:
        (@self).run()
        (@self).destroy()

    // Awaits the thread and destroys safely
    func awaitAndDestroy() -> anyptr:
        res = (@self).await()
        (@self).destroy()
        return res