using file

// Native C++ backend for file reading
_NativeCPP("FileReader") str BFR_00(op: int, path: str) // returns file content or empty string

// FileReader struct
struct FileReader:
    file: File
    isOpen: bool
    content: str
    position: int

// Implement FileReader functions
impl FileReader:
    init(file: File):
        this.file = file
        this.isOpen = false 
        this.content = ""
        this.position = 0

    // Opens the file for reading
    func open() -> bool:
        if (this.isOpen):
            return false

        if (this.file.exists() == false):
            raise "Tried to open a FileReader of a file that does not exist"

        success = BFR_00(0, this.file.name) == "1"
        if (success):
            this.content = BFR_00(1, this.file.name) 
            this.position = 0
            this.isOpen = true

        return success

    // Reads the next N characters from the file
    func read(n: int) -> str:
        if (this.isOpen == false):
            raise "Use FileReader#open() before FileReader#read"

        start = this.position
        end: mut = start + n
        if (end > len(this.content)):
            end = len(this.content)

        result: mut = ""
        i: mut = start
        while (i < end):
            result = result + this.content[i]
            i = i + 1

        this.position = end
        return result

    // Reads the next line from the file
    func readLine() -> str:
        if (this.isOpen == false):
            raise "Use FileReader#open() before FileReader#readLine"

        result: mut = ""
        while (this.position < len(this.content)):
            c = this.content[this.position]
            this.position = this.position + 1
            if (c == '\n'):
                break
            result = result + c

        return result

    // Checks if the reader reached the end of the file
    func eof() -> bool:
        return this.position >= len(this.content)

    // Closes the file
    func close() -> bool:
        if (this.isOpen == false):
            raise "Use FileReader#open() before FileReader#"

        this.content = ""
        this.position = 0
        this.isOpen = false
        return true
