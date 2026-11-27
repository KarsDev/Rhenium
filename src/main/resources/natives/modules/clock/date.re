_NativeCPP("clock") long __getLocalDate()

enum DayOfWeek:
    SUNDAY
    MONDAY
    TUESDAY
    WEDNESDAY
    THURSDAY
    FRIDAY
    SATURDAY

enum Month:
    JANUARY
    FEBRUARY
    MARCH
    APRIL
    MAY
    JUNE
    JULY
    AUGUST
    SEPTEMBER
    OCTOBER
    NOVEMBER
    DECEMBER

struct Date inherits Writeable:
    day: int
    month: Month
    year: int

impl Date:
    func toString() -> str: // override
        return "Date{day=" + this.day + ", month=" + this.month + ", year=" + this.year + "}"
    
    func getDayOfWeek() -> DayOfWeek inline:
        y: mut int = this.year
        m: int = this.month + 1
        d: int = this.day

        offsets = [0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4]

        if m < 3:
            y -= 1

        dow = (y + y / 4 - y / 100 + y / 400 + offsets[m - 1] + d) % 7

        return dow
    
    func isLeapYear() -> bool inline:
        return this.year % 400 == 0 or (this.year % 4 == 0 and this.year % 100 != 0)

    func daysInMonth() -> int inline:
        match this.month:
            Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY, Month.AUGUST, Month.OCTOBER, Month.DECEMBER:
                return 31
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER:
                return 30
            Month.FEBRUARY:
                return 29 if this.isLeapYear() else 28
        return -1
    func isValid() -> bool inline:
        if this.year < 1:
            return false

        if this.day < 1:
            return false

        return this.day <= this.daysInMonth()

    func isWeekend() -> bool inline:
        dow = this.getDayOfWeek()
        return dow == DayOfWeek.SATURDAY or dow == DayOfWeek.SUNDAY

    func isWeekday() -> bool inline:
        return not this.isWeekend()

namespace Date:
    func getLocalDate() -> Date:
        packed: long = __getLocalDate()

        year = cast<int>(packed & 65535L)
        month = cast<int>((packed >> 16) & 255L)
        day = cast<int>((packed >> 24) & 255L)

        return init Date(day, month, year)