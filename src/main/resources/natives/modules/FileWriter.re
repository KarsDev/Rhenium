using file

// Native C++ backend for file writing
_NativeCPP("FileWriter") bool BFW_00(op: int, path: str, data: str)

// FileWriter struct
struct FileWriter:
    file: File
    isOpen: bool = false
    append: bool = false

// Implement FileWriter functions
impl FileWriter:

    // Opens the file for writing (truncate)
    func open() -> bool:
        if ((@self).isOpen):
            return false
            
        if ((@self).file.exists() == false):
            raise "Tried to open a FileWriter of a file that does not exist"

        result = BFW_00(0, (@self).file.name, "")
        (@self).isOpen = result
        (@self).append = false
        return result

    // Opens the file for appending
    func openAppend() -> bool:
        if ((@self).isOpen):
            raise "Use FileWriter#open() before FileWriter#openAppend"

        result = BFW_00(1, (@self).file.name, "")
        (@self).isOpen = result
        (@self).append = true
        return result

    // Writes raw text to the file
    func write(text: str) -> bool:
        if ((@self).isOpen == false):
            raise "Use FileWriter#open() before FileWriter#write"

        return BFW_00(2, (@self).file.name, text)

    // Writes text followed by a newline
    func writeLine(text: str) -> bool:
        if ((@self).isOpen == false):
            raise "Use FileWriter#open() before FileWriter#writeLine"

        return BFW_00(2, (@self).file.name, text + "\n")

    // Flushes the file
    func flush() -> bool:
        if ((@self).isOpen == false):
            raise "Use FileWriter#open() before flush"

        return BFW_00(3, (@self).file.name, "")

    // Closes the file
    func close() -> bool:
        if ((@self).isOpen == false):
            raise "Use FileWriter#open() before FileWriter#close"

        result = BFW_00(4, (@self).file.name, "")
        (@self).isOpen = false
        return result
