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
        d: mut int = this.day
        m: mut int = this.month
        y: mut int = this.year

        if m < 3:
            m += 12
            y -= 1
        
        K = y % 100
        J = y / 100

        h = (d + (13 * (m + 1)) / 5 + K + K / 4 + J / 4 + 5 * J ) % 7
        return h
    
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

        year = cast<int>(packed & cast<long>(65535))
        month = cast<int>((packed >> 16) & cast<long>(255))
        day = cast<int>((packed >> 24) & cast<long>(255))

        return init Date(day, month, year)