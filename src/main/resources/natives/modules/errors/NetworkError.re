/*
  NetworkError is raised when an operation involving network
  communication fails.

  Example:
      client.connect("example.com")

  when the connection cannot be established due to a network failure.
*/
struct NetworkError inherits Error:
    // Description of the network failure
    message_text: str

impl NetworkError:
    init(message: str):
        this.message_text = message

    func message() -> str: // override
        return this.message_text