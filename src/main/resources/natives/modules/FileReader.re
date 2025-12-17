using file

// Native C++ backend for file reading
_NativeCPP("FileReader") str BFR_00(op: int, path: str) // returns file content or empty string

// FileReader struct
struct FileReader:
    file: File
    isOpen: bool = false
    content: str = ""
    position: int = 0

// Implement FileReader functions
impl FileReader:

    // Opens the file for reading
    func open() -> bool:
        if ((@self).isOpen):
            return false

        if ((@self).file.exists() == false):
            raise "Tried to open a FileReader of a file that does not exist"

        success = BFR_00(0, (@self).file.name) == "1"
        if (success):
            (@self).content = BFR_00(1, (@self).file.name) 
            (@self).position = 0
            (@self).isOpen = true

        return success

    // Reads the next N characters from the file
    func read(n: int) -> str:
        if ((@self).isOpen == false):
            raise "Use FileReader#open() before FileReader#read"

        start = (@self).position
        end: mut = start + n
        if (end > len((@self).content)):
            end = len((@self).content)

        result: mut = ""
        i: mut = start
        while (i < end):
            result = result + (@self).content[i]
            i = i + 1

        (@self).position = end
        return result

    // Reads the next line from the file
    func readLine() -> str:
        if ((@self).isOpen == false):
            raise "Use FileReader#open() before FileReader#readLine"

        result: mut = ""
        while ((@self).position < len((@self).content)):
            c = (@self).content[(@self).position]
            (@self).position = (@self).position + 1
            if (c == '\n'):
                break
            result = result + c

        return result

    // Checks if the reader reached the end of the file
    func eof() -> bool:
        return (@self).position >= len((@self).content)

    // Closes the file
    func close() -> bool:
        if ((@self).isOpen == false):
            raise "Use FileReader#open() before FileReader#"

        (@self).content = ""
        (@self).position = 0
        (@self).isOpen = false
        return true
