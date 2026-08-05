/*
  ResultError is raised when a value is extracted from
  a Result, an Option or similar structs without checking whether it contains
  a successful value first.

  Example:
      let value = result.value()

  when result contains an error variant.
*/
struct ResultError inherits Error:
    message_text: str

impl ResultError:
    init(stname: str):
        this.message_text = "Unchecked get/unwrap from " + stname

    func message() -> str: // override
        return this.message_text