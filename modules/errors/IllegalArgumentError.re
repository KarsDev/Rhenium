/*
  IllegalArgumentError is raised when a function or method receives
  an argument that is not valid for the requested operation.

  Example:
      init List<int>(-1)

  when a negative value is provided where only non-negative values are allowed.
*/
struct IllegalArgumentError inherits Error:
    // Description of the invalid argument and why it was rejected
    message_text: str

impl IllegalArgumentError:
    init(message: str):
        this.message_text = message

    func message() -> str: // override
        return this.message_text