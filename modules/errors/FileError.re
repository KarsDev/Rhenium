// FileError is used with File Writer/Reader
struct FileError inherits Error:
    msg: str

impl FileError:
    init(msg: str):
        this.msg = msg

    func message() -> str: // override
        return this.msg