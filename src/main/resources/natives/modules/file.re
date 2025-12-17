// Include the native C++ file with its functions
_NativeCPP("file") bool BFN_00(op: int, file: str)

// File struct
struct File:
    // File name, also with extension
    name: str

// Implement File functions
impl File:
    // Returns if the file exists
    func exists() -> bool:
        return BFN_00(0, (@self).name) // 0 -> file exists

    // Creates the new file, returns true if the file already existed
    func createNew() -> bool:
        return BFN_00(1, (@self).name) // 1 -> create new file

    // Checks if the file is a directory
    func isDir() -> bool:
        return BFN_00(2, (@self).name) // 2 -> file is directory

    // Checks if the file is a normal file
    func isFile() -> bool:
        return BFN_00(3, (@self).name) // 3 -> file is file

    // Renames the file and returns true if it has been renamed successfully
    func rename(newName: str) -> bool:
        result = BFN_00(4, (@self).name + "//" + newName) // 4 -> rename file

        if (result):
            (@self).name = newName

        return result

    // Deletes the file and returns true if it has been deleted successfully
    func delete() -> bool:
        return BFN_00(5, (@self).name) // 5 -> delete file
