using file.file
using errors.FileError
using number.number

// Native C++ backend for file writing
_NativeCPP("FileWriter") bool BFW_00(op: int, path: str, data: str) and str BFW_GFSB(op: int, path: str)

// FileWriter struct
struct FileWriter:
    file: File
    isOpen: mut bool
    append: mut bool

// Implement FileWriter functions
impl FileWriter:
    init(file: File):
        this.file = file
        this.isOpen = false
        this.append = false

    // Opens the file for writing (truncate)
    func open() -> bool:
        if this.isOpen:
            return false

        if not this.file.exists():
            raise init FileError("Tried to open a FileWriter of a file that does not exist")

        result = BFW_00(0, this.file.name, "")
        this.isOpen = result
        this.append = false
        return result

    // Opens the file for appending
    func openAppend() -> bool:
        if this.isOpen:
            raise init FileError("FileWriter is already open, close it before calling FileWriter#openAppend")

        result = BFW_00(1, this.file.name, "")
        this.isOpen = result
        this.append = true
        return result

    // Writes raw text to the file
    func write(text: str) -> bool:
        this.ensureOpen("FileWriter#write")

        return BFW_00(2, this.file.name, text)

    // Writes text followed by a newline
    func writeLine(text: str) -> bool:
        this.ensureOpen("FileWriter#writeLine")

        return BFW_00(2, this.file.name, text + "\n")

    // Writes an empty line
    func newLine() -> bool:
        return this.writeLine("")

    // Writes several pieces of text back to back, stops on the first failure
    func writeAll(parts: str) -> bool:
        return this.write(parts)

    // Writes a hex string as raw bytes. Length must be even and only
    // contain 0-9, a-f, A-F.
    func writeBytes(hex: str) -> bool:
        this.ensureOpen("FileWriter#writeBytes")

        if len(hex) % 2 != 0:
            raise init FileError("Hex string must have an even number of characters")

        if len(hex) == 0:
            return true

        return BFW_00(5, this.file.name, hex)

    // Writes a single byte (0..255)
    func writeByte(value: int) -> bool:
        this.ensureOpen("FileWriter#writeByte")

        if value < 0 or value > 255:
            raise init FileError("Byte value must be between 0 and 255")

        return BFW_00(6, this.file.name, "" + value)

    // Clears the file contents (keeps it open)
    func clear() -> bool:
        this.ensureOpen("FileWriter#clear")

        return BFW_00(7, this.file.name, "")

    // Current size of the file in bytes
    func size() -> int:
        this.ensureOpen("FileWriter#size")

        return Number::parseInt(BFW_GFSB(0, this.file.name))

    func isAppending() -> bool:
        return this.append

    func ensureOpen(caller: str):
        if not this.isOpen:
            raise init FileError("Use FileWriter#open() before " + caller)

    // Flushes the file
    func flush() -> bool:
        this.ensureOpen("FileWriter#flush")

        return BFW_00(3, this.file.name, "")

    // Closes the file
    func close() -> bool:
        this.ensureOpen("FileWriter#close")

        result = BFW_00(4, this.file.name, "")
        this.isOpen = false
        return result
