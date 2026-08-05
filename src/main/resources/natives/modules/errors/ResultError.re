/*
  ResultUnwrapError is raised when a value is extracted from
  a Result, an Option or similar structs without checking whether it contains
  a successful value first.

  Example:
      let value = result.value()

  when result contains an error variant.
*/
struct ResultUnwrapError inherits Error:
    message_text: str

impl ResultUnwrapError:
    init(stname: str):
        this.message_text = "Unchecked get from " + stname

    func message() -> str: // override
        return this.message_text