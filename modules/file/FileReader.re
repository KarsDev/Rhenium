using number
using file.file
using errors.FileError

// Native C++ backend for file reading
_NativeCPP("FileReader") str BFR_00(op: int, handle: int, path: str)

// FileReader struct
struct FileReader:
    file: File
    isOpen: mut bool
    content: mut str
    position: mut int
    bytePosition: mut int
    handle: mut int

impl FileReader:
    init(file: File):
        this.file = file
        this.isOpen = false
        this.content = ""
        this.position = 0
        this.bytePosition = 0
        this.handle = 0

    // Opens the file for reading
    func open() -> bool:
        if this.isOpen:
            return false

        if not this.file.exists():
            raise init FileError("Cannot open a file that does not exist")

        result = BFR_00(0, 0, this.file.name)

        if result == "0":
            return false

        this.handle = Number::parseInt(result)

        this.content = BFR_00(1, this.handle, "")

        this.position = 0
        this.bytePosition = 0
        this.isOpen = true

        return true

    // Reads n bytes from the file and creates a string
    func read(n: int) -> str:
        this.ensureOpen()

        if n < 0:
            raise init FileError("Read length cannot be negative")

        if n == 0:
            return ""

        remaining = len(this.content) - this.position
        count: mut = n

        if count > remaining:
            count = remaining

        result = strSubRange(this.content, this.position, this.position + count)

        this.position += count

        return result

    // Reads bytes until it finds the newline char
    func readLine() -> str:
        this.ensureOpen()

        start = this.position

        while this.position < len(this.content):
            if this.content[this.position] == '\n':
                result = strSubRange(this.content, start, this.position)

                this.position += 1
                return result

            this.position += 1

        return strSubRange(this.content, start, this.position)

    // Reads the whole file
    func readAll() -> str:
        this.ensureOpen()

        result = strSubRange(this.content, this.position, len(this.content))
        this.position = len(this.content)

        return result

    // Reads a character
    func readChar() -> char:
        return this.read(1)[0]

    // Gets the last char without increasing the reader position
    func peek() -> char:
        this.ensureOpen()

        if this.position >= len(this.content):
            return '?'

        return strSubRange(this.content, this.position, this.position + 1)[0]

    // Skips n chars
    func skip(n: int) -> int:
        this.ensureOpen()

        if n < 0:
            raise init FileError("Skip length cannot be negative")

        count: mut = n
        remaining = len(this.content) - this.position

        if count > remaining:
            count = remaining

        this.position += count

        return count

    // Sets the position to a custom value pos
    func seek(pos: int):
        this.ensureOpen()

        if pos < 0 or pos > len(this.content):
            raise init FileError("Seek position out of range")

        this.position = pos

    // Gets the position of the reader
    func tell() -> int:
        this.ensureOpen()
        return this.position

    // Gets the remaining bytes to read
    func remaining() -> int:
        this.ensureOpen()
        return len(this.content) - this.position

    // Resets the reader position
    func reset():
        this.ensureOpen()
        this.position = 0
        this.bytePosition = 0

    // Checks if the reader position reached the content length
    func eof() -> bool:
        this.ensureOpen()
        return this.position >= len(this.content)

    // Gets the file size in bytes
    func size() -> int:
        this.ensureOpen()
        return Number::parseInt(BFR_00(5, this.handle, ""))

    // Reads n bytes
    func readBytes(n: int) -> str:
        this.ensureOpen()

        if n < 0:
            raise init FileError("Read length cannot be negative")

        if n == 0:
            return ""

        left = this.size() - this.bytePosition
        count: mut = n

        if count > left:
            count = left

        if count <= 0:
            return ""

        arg = "" + this.bytePosition + "," + count
        result = BFR_00(3, this.handle, arg)

        this.bytePosition += count

        return result

    // Reads a single byte
    func readByte() -> int:
        this.ensureOpen()

        if this.bytePosition >= this.size():
            return -1

        result = Number::parseInt(BFR_00(4, this.handle, "" + this.bytePosition))

        this.bytePosition += 1

        return result

    // Reads all the bytes creating a string
    func readAllBytes() -> str:
        this.ensureOpen()
        return this.readBytes(this.size() - this.bytePosition)

    // Sets the byte position to pos
    func seekBytes(pos: int):
        this.ensureOpen()

        if pos < 0 or pos > this.size():
            raise init FileError("Byte seek position out of range")

        this.bytePosition = pos

    // Gets the reader byte position
    func tellBytes() -> int:
        this.ensureOpen()
        return this.bytePosition

    // Checks if the reader byte position reached the size
    func eofBytes() -> bool:
        this.ensureOpen()
        return this.bytePosition >= this.size()

    // Ensures the file reader is open
    func ensureOpen():
        if this.isOpen == false:
            raise init FileError("FileReader is not open")

    // Closes the file reader
    func close() -> bool:
        if this.isOpen == false:
            return false

        result = BFR_00(2, this.handle, "")

        this.content = ""
        this.position = 0
        this.bytePosition = 0
        this.handle = 0
        this.isOpen = false

        return result == "1"
