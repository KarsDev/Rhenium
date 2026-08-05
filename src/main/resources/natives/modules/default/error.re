/*
  Represents an error that can be either raised or returned by functions.

  The Error trait is used by the compiler's exception system.
  Any type implementing this trait can be raised as an exception and can 
  provide a human-readable description through the message() function.

  This allows custom error types to integrate naturally with exception
  handling while providing descriptive messages.

  This allows custom error types to integrate naturally with
  exception handling while providing descriptive error messages.

  Example:
  struct FileError inherits Error:
      path: str

  impl FileError:
      func message() -> str: // override
          return "File not found: " + this.path

  raise FileError("config.toml")
*/
trait Error:
    // Returns a human-readable description of the error.
    //
    // This message is displayed when the error is raised
    // or reported by the runtime.
    func message() -> str
trait Error:
    func message() -> str