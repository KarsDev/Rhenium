using file

// Native C++ backend for file writing
_NativeCPP("FileWriter") bool BFW_00(op: int, path: str, data: str)

// FileWriter struct
struct FileWriter:
    file: File
    isOpen: bool
    append: bool

// Implement FileWriter functions
impl FileWriter:
    init(file: File):
        this.file = file
        this.isOpen = false
        this.append = false

    // Opens the file for writing (truncate)
    func open() -> bool:
        if (this.isOpen):
            return false
            
        if (this.file.exists() == false):
            raise "Tried to open a FileWriter of a file that does not exist"

        result = BFW_00(0, this.file.name, "")
        this.isOpen = result
        this.append = false
        return result

    // Opens the file for appending
    func openAppend() -> bool:
        if (this.isOpen):
            raise "Use FileWriter#open() before FileWriter#openAppend"

        result = BFW_00(1, this.file.name, "")
        this.isOpen = result
        this.append = true
        return result

    // Writes raw text to the file
    func write(text: str) -> bool:
        if (this.isOpen == false):
            raise "Use FileWriter#open() before FileWriter#write"

        return BFW_00(2, this.file.name, text)

    // Writes text followed by a newline
    func writeLine(text: str) -> bool:
        if (this.isOpen == false):
            raise "Use FileWriter#open() before FileWriter#writeLine"

        return BFW_00(2, this.file.name, text + "\n")

    // Flushes the file
    func flush() -> bool:
        if (this.isOpen == false):
            raise "Use FileWriter#open() before flush"

        return BFW_00(3, this.file.name, "")

    // Closes the file
    func close() -> bool:
        if (this.isOpen == false):
            raise "Use FileWriter#open() before FileWriter#close"

        result = BFW_00(4, this.file.name, "")
        this.isOpen = false
        return result
