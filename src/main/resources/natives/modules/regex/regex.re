using lang.option
using number.number

_NativeCPP("regex") \
    anyptr __RegexCompile0(pattern: str) and \
    bool __RegexBools0(regex: anyptr, option: int, input: str) and \
    str __RegexFind0(regex: anyptr, input: str) and \
    ptr -> str __RegexFindAll0(input: str, regex: anyptr, count: ptr -> int) and \
    int __RegexCount0(input: str, regex: anyptr)

struct Regex:
    handle: anyptr

impl Regex:
    func matches(input: str) -> bool:
        return __RegexBools0(this.handle, 0, input)
    
    func search(input: str) -> bool:
        return __RegexBools0(this.handle, 1, input)

    func find(input: str) -> Option<str>:
        result = __RegexFind0(this.handle, input)
        if (result == null):
            return init Option<str>()
        
        return init Option<str>(cast<str>(result))

    func findAll(input: str) -> StrDynArr:
        count: mut = 0
        countPtr = ptr(count)

        results = __RegexFindAll0(input, this.handle, countPtr)

        return init StrDynArr(results, count)
    
    func count(input: str) -> int:
        return __RegexCount0(input, this.handle)

    delete:
        __RegexBools0(this.handle, 2, null)

namespace Regex:
    func compile(pattern: str) -> Regex:
        return init Regex(__RegexCompile0(pattern))

/*
    replace(input: str, replacement: str) -> str
    replaceAll(input: str, replacement: str) -> str

    split(input: str) -> str[]

    groups(input: str) -> str[]?
*/