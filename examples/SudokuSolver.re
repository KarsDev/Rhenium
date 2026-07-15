using list
using number
using option

type Board = List<List<int>>

struct Coord:
    x: int
    y: int

func parseBoard(boardStr: str) -> Board:
    board = init List<List<int>>()

    result = strSplit(boardStr, "\n")
    for (i in range(result.size)):
        r = result.values[i]
        line = strReplace(strStrip(r), " ", "")

        if (len(line) == 0):
            continue
        
        if (len(line) != 9):
            raise "Invalid row: " + line
        
        row = init List<int>()

        for (c in line):
            if (c == '.' or c == '0'):
                row.add(0)
            else if (c >= '0' and c <= '9'):
                cts = charToStrAscii(c)
                row.add(Number::parseInt(cts))
            else:
                raise "Invalid character: " + c
        
        board.add(row)

    return board

func findEmpty(board: Board) -> Option<Coord>:
    for (r in range(9)):
        for (c in range(9)):
            if (board.get(r).get(c) == 0):
                coord = init Coord(r, c)
                return init Option<Coord>(coord)
    return init Option<Coord>()

func valid(board: Board, row: int, col: int, num: int) -> bool:
    for (c in range(9)):
        if (board.get(row).get(c) == num):
            return false
    
    for (r in range(9)):
        if (board.get(r).get(col) == num):
            return false

    box_row = (row / 3) * 3
    box_col = (col / 3) * 3

    for (r in range(box_row, box_row + 3)):
        for (c in range(box_col, box_col + 3)):
            if (board.get(r).get(c) == num):
                return false

    return true

func solve(board: Board) -> bool:
    empty = findEmpty(board)

    if (empty.isEmpty()):
        return true
    
    row = empty.get().x
    col = empty.get().y

    for (num in range(1, 10)):
        if (valid(board, row, col, num)):
            board.get(row).set(col, num)

            if (solve(board)):
                return true

            board.get(row).set(col, 0)
    return false

func writeBoard(board: Board) -> str:
    s: mut = ""

    for (i in range(board.size)):
        b = board.get(i)
        for (j in range(b.size)):
            s += b.get(j)
        if (i < board.size):
            s += "\n"
    
    return s

func main() -> int:
    boardStr = \
        "53..7....\n" + \
        "6..195...\n" + \
        ".98....6.\n" + \
        "8...6...3\n" + \
        "4..8.3..1\n" + \
        "7...2...6\n" + \
        ".6....28.\n" + \
        "...419..5\n" + \
        "....8..79"

    board = parseBoard(boardStr)
    
    println("Board: \n" + writeBoard(board))

    solve(board)

    println("Solved: \n" + writeBoard(board))