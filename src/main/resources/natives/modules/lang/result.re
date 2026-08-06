using errors.ResultError

/*
  Represents the result of an operation that can either succesd or fail.

  A Result<T> can either:
  - Contain a success value (Ok)
  - Contain an error value (Err)
  
  This is useful for operations that may fail without using null values or unchecked exceptions.

  Example:
  // Using the namespace "Result"
  result = Result::Ok(40)

  if (result.isOk()):
    println(result.unwrap())
  else:
    println(result.unwrapErr())
*/
generic struct Result<T, E inherits Error>:
    value: T
    error: E
    ok: bool

impl Result<T, E inherits Error>:
    init(value: T):
        this.value = copy(value)
        this.ok = true

    // Creates a failed Result
    init(error: E, isError: bool):
        this.error = copy(error)
        this.ok = false

    // Returns true if the result is successful
    func isOk() -> bool:
        return this.ok

    // Returns true if the result is an error
    func isErr() -> bool:
        return not this.ok

    // Returns the success value, raising if this is an error
    func unwrap() -> T:
        if (this.isErr()):
            raise init ResultError("Result")
        return this.value

    // Returns the error value, raising if this is successful
    func unwrapErr() -> E:
        if (this.isOk()):
            raise init ResultError("Result")
        return this.error

    // Returns the success value or 'other' if this is an error
    func orElse(other: T) -> T:
        return this.value if this.isOk() else other

    // Returns the success value or raises with the given message
    func expect(message: str) -> T:
        if (this.isErr()):
            raise message
        return this.unwrap()

    // Returns the error value or raises with the given message
    func expectErr(message: str) -> E:
        if (this.isOk()):
            raise message
        return this.unwrapErr()

    // Return true if the Result contains the specified success value
    func contains(value: T) -> bool:
        return this.isOk() and this.unwrap() == value

    // Return true if the Result contains the specified error value
    func containsErr(error: E) -> bool:
        return this.isErr() and this.unwrapErr() == error
    
namespace Result:
    // Successed result
    generic func Ok<T, E>(obj: T):
        return init Result<T, E>(obj)

    // Failed result
    generic func Err<T, E>(err: E):
        return init Result<T, E>(err, true)