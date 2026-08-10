using lang.option
using number.number

_NativeCPP("regex") \
    anyptr __RegexCompile0(pattern: str) and \
    bool __RegexBools0(regex: anyptr, option: int, input: str) and \
    str __RegexFind0(regex: anyptr, input: str) and \
    ptr -> str __RegexFindAll0(input: str, regex: anyptr, count: ptr -> int) and \
    int __RegexCount0(input: str, regex: anyptr) and \
    none __RegexFreeAll0(strings: ptr -> str, count: int) and \
    str __RegexReplace0(regex: anyptr, input: str, replacement: str) and \
    ptr -> str __RegexSplit0(input: str, pattern: anyptr, count: ptr -> int) and \
    ptr -> str __RegexGroups0(input: str, pattern: anyptr, count: ptr -> int)

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

    func replace(input: str, replacement: str) -> str:
        return __RegexReplace0(this.handle, input, replacement)

    func split(input: str) -> StrDynArr:
        count: mut = 0
        countPtr = ptr(count)

        results = __RegexSplit0(input, this.handle, countPtr)

        return init StrDynArr(results, count)

    func groups(input: str) -> StrDynArr:
        count: mut = 0
        countPtr = ptr(count)

        results = __RegexGroups0(input, this.handle, countPtr)

        return init StrDynArr(results, count)

    func freeAll(strings: ptr -> str, count: int):
        __RegexFreeAll0(strings, count)
    
    delete:
        __RegexBools0(this.handle, 2, null)

namespace Regex:
    func compile(pattern: str) -> Regex:
        return init Regex(__RegexCompile0(pattern))