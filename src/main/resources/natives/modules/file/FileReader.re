using number
using file.file
using errors.FileError

_NativeCPP("FileReader") str BFR_00(op: int, handle: int, path: str)

struct FileReader:
    file: File
    isOpen: mut bool
    content: mut str
    position: mut int
    handle: mut int

impl FileReader:

    init(file: File):
        this.file = file
        this.isOpen = false
        this.content = ""
        this.position = 0
        this.handle = 0

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
        this.isOpen = true

        return true

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

    func eof() -> bool:
        this.ensureOpen()
        return this.position >= len(this.content)

    func ensureOpen():
        if this.isOpen == false:
            raise init FileError("FileReader is not open")

    func close() -> bool:
        if this.isOpen == false:
            return false

        result = BFR_00(2, this.handle, "")

        this.content = ""
        this.position = 0
        this.handle = 0
        this.isOpen = false

        return result == "1"
