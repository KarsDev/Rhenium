using graphics
using clock
using collections.list
using math
using random.random

namespace Config:
    global COLS = 10
    global ROWS = 20
    global CELL = 30
    global BOARD_X = 30
    global BOARD_Y = 30
    global SIDE_X = 370
    global WIN_W = 520
    global WIN_H = 660

struct Tetris:
    m_screen: anyptr
    m_board: List<int>
    m_shapes: List<int>
    m_rng: Random

    m_kind: mut int
    m_rot: mut int
    m_x: mut int
    m_y: mut int
    m_next: mut int

    m_score: mut int
    m_lines: mut int
    m_level: mut int
    m_gameOver: mut bool
    m_fallTicks: mut int

    m_leftHeld: mut int
    m_rightHeld: mut int
    m_prevRotate: mut bool
    m_prevDrop: mut bool

impl Tetris:
    init(screen: anyptr):
        this.m_screen = screen
        this.m_board = init List<int>(Config::COLS * Config::ROWS)
        this.m_shapes = init List<int>(56)
        this.m_rng = init Random()

        this.m_kind = 0
        this.m_rot = 0
        this.m_x = 4
        this.m_y = 1
        this.m_next = 0
        this.m_score = 0
        this.m_lines = 0
        this.m_level = 0
        this.m_gameOver = false
        this.m_fallTicks = 0
        this.m_leftHeld = 0
        this.m_rightHeld = 0
        this.m_prevRotate = false
        this.m_prevDrop = false

        this.defineShapes()
        this.reset()

    func setShape(kind: int, x0: int, y0: int, x1: int, y1: int, x2: int, y2: int, x3: int, y3: int):
        b = kind * 8
        this.m_shapes.insert(b + 0, x0)
        this.m_shapes.insert(b + 1, y0)
        this.m_shapes.insert(b + 2, x1)
        this.m_shapes.insert(b + 3, y1)
        this.m_shapes.insert(b + 4, x2)
        this.m_shapes.insert(b + 5, y2)
        this.m_shapes.insert(b + 6, x3)
        this.m_shapes.insert(b + 7, y3)

    func defineShapes():
        this.setShape(0, -1, 0,  0, 0,  1, 0,  2, 0)    // I
        this.setShape(1,  0, 0,  1, 0,  0, 1,  1, 1)    // O
        this.setShape(2, -1, 0,  0, 0,  1, 0,  0, -1)   // T
        this.setShape(3, -1, 0,  0, 0,  0, -1, 1, -1)   // S
        this.setShape(4, -1, -1, 0, -1, 0, 0,  1, 0)    // Z
        this.setShape(5, -1, -1, -1, 0, 0, 0,  1, 0)    // J
        this.setShape(6,  1, -1, -1, 0, 0, 0,  1, 0)    // L

    func reset():
        for i in range(Config::COLS * Config::ROWS):
            this.m_board.insert(i, 0)

        this.m_score = 0
        this.m_lines = 0
        this.m_level = 0
        this.m_gameOver = false
        this.m_fallTicks = 0
        this.m_next = this.m_rng.nextInt(6)
        this.spawn()

    func cell(x: int, y: int) -> int:
        return this.m_board.get(y * Config::COLS + x)

    func setCell(x: int, y: int, value: int):
        this.m_board.insert(y * Config::COLS + x, value)

    func offset(kind: int, rot: int, i: int, axis: int) -> int:
        x: mut int = this.m_shapes.get(kind * 8 + i * 2)
        y: mut int = this.m_shapes.get(kind * 8 + i * 2 + 1)

        r: mut int = rot
        if kind == 1:
            r = 0

        while r > 0:
            t = x
            x = 0 - y
            y = t
            r -= 1

        if axis == 0:
            return x
        return y

    func collides(kind: int, rot: int, px: int, py: int) -> bool:
        for i in range(4):
            x = px + this.offset(kind, rot, i, 0)
            y = py + this.offset(kind, rot, i, 1)

            if x < 0 or x >= Config::COLS or y >= Config::ROWS:
                return true
            if y >= 0 and this.cell(x, y) != 0:
                return true
        return false

    func rowFull(row: int) -> bool:
        for x in range(Config::COLS):
            if this.cell(x, row) == 0:
                return false
        return true

    func removeRow(row: int):
        y: mut int = row
        while y > 0:
            for x in range(Config::COLS):
                this.setCell(x, y, this.cell(x, y - 1))
            y -= 1
        for x in range(Config::COLS):
            this.setCell(x, 0, 0)

    func clearLines() -> int:
        cleared: mut int = 0
        row: mut int = Config::ROWS - 1

        while row >= 0:
            if this.rowFull(row):
                this.removeRow(row)
                cleared += 1
            else:
                row -= 1
        return cleared

    func randomPiece() -> int:
        p = this.m_rng.nextInt(6)
        if p == this.m_kind:
            return this.m_rng.nextInt(6)
        return p

    func spawn():
        this.m_kind = this.m_next
        this.m_next = this.randomPiece()
        this.m_rot = 0
        this.m_x = 4
        this.m_y = 1
        this.m_fallTicks = 0

        if this.collides(this.m_kind, this.m_rot, this.m_x, this.m_y):
            this.m_gameOver = true

    func tryMove(dx: int, dy: int) -> bool:
        if this.collides(this.m_kind, this.m_rot, this.m_x + dx, this.m_y + dy):
            return false
        this.m_x += dx
        this.m_y += dy
        return true

    func tryRotate():
        newRot = (this.m_rot + 1) % 4

        for d in range(3):
            if not this.collides(this.m_kind, newRot, this.m_x + d, this.m_y):
                this.m_rot = newRot
                this.m_x += d
                return
            if not this.collides(this.m_kind, newRot, this.m_x - d, this.m_y):
                this.m_rot = newRot
                this.m_x -= d
                return

    func hardDrop():
        while this.tryMove(0, 1):
            this.m_score += 2
        this.lockPiece()

    func lineScore(n: int) -> int:
        base: mut int = 0
        if n == 1:
            base = 100
        if n == 2:
            base = 300
        if n == 3:
            base = 500
        if n == 4:
            base = 800
        return base * (this.m_level + 1)

    func lockPiece():
        for i in range(4):
            x = this.m_x + this.offset(this.m_kind, this.m_rot, i, 0)
            y = this.m_y + this.offset(this.m_kind, this.m_rot, i, 1)
            if y >= 0:
                this.setCell(x, y, this.m_kind + 1)

        cleared = this.clearLines()
        this.m_score += this.lineScore(cleared)
        this.m_lines += cleared

        if this.m_lines >= (this.m_level + 1) * 10:
            this.m_level += 1

        this.spawn()

    func horizontal(pressed: bool, held: int, dx: int) -> int:
        if not pressed:
            return 0
        if held == 0 or (held >= 10 and (held - 10) % 3 == 0):
            this.tryMove(dx, 0)
        return held + 1

    func handleInput():
        this.m_leftHeld = this.horizontal(isKeyPressed(Keys::KEY_LEFT), this.m_leftHeld, -1)
        this.m_rightHeld = this.horizontal(isKeyPressed(Keys::KEY_RIGHT), this.m_rightHeld, 1)

        rotate = isKeyPressed(Keys::KEY_UP) or isKeyPressed(Keys::KEY_X)
        if rotate and not this.m_prevRotate:
            this.tryRotate()
        this.m_prevRotate = rotate

        drop = isKeyPressed(Keys::KEY_SPACE)
        if drop and not this.m_prevDrop:
            this.hardDrop()
        this.m_prevDrop = drop

    func applyGravity():
        interval: mut int = Math::clamp(48 - this.m_level * 5, 4, 48)
        if isKeyPressed(Keys::KEY_DOWN):
            interval = 2

        this.m_fallTicks += 1
        if this.m_fallTicks >= interval:
            this.m_fallTicks = 0
            if not this.tryMove(0, 1):
                this.lockPiece()

    func update():
        if this.m_gameOver:
            if isKeyPressed(Keys::KEY_ENTER):
                this.reset()
            return

        this.handleInput()

        if this.m_gameOver:
            return
        this.applyGravity()

    func setPieceColor(kind: int):
        s = this.m_screen
        if kind == 0:
            setScreenColor(s, 0, 220, 230, 255)     // I cyan
        if kind == 1:
            setScreenColor(s, 240, 220, 0, 255)     // O yellow
        if kind == 2:
            setScreenColor(s, 160, 40, 220, 255)    // T purple
        if kind == 3:
            setScreenColor(s, 40, 200, 60, 255)     // S green
        if kind == 4:
            setScreenColor(s, 230, 40, 40, 255)     // Z red
        if kind == 5:
            setScreenColor(s, 50, 70, 230, 255)     // J blue
        if kind == 6:
            setScreenColor(s, 240, 150, 20, 255)    // L orange

    func drawBlock(px: int, py: int, kind: int):
        this.setPieceColor(kind)
        fillScreenRect(this.m_screen, px + 1, py + 1, Config::CELL - 2, Config::CELL - 2)

    func drawBoard():
        s = this.m_screen
        w = Config::COLS * Config::CELL
        h = Config::ROWS * Config::CELL

        setScreenColor(s, 30, 30, 40, 255)
        fillScreenRect(s, Config::BOARD_X, Config::BOARD_Y, w, h)

        for y in range(Config::ROWS):
            for x in range(Config::COLS):
                v = this.cell(x, y)
                if v != 0:
                    this.drawBlock(Config::BOARD_X + x * Config::CELL, Config::BOARD_Y + y * Config::CELL, v - 1)

        setScreenColor(s, 200, 200, 210, 255)
        drawScreenRect(s, Config::BOARD_X - 1, Config::BOARD_Y - 1, w + 2, h + 2)

    func drawPiece():
        s = this.m_screen

        gy: mut int = this.m_y
        while not this.collides(this.m_kind, this.m_rot, this.m_x, gy + 1):
            gy += 1

        this.setPieceColor(this.m_kind)
        for i in range(4):
            x = this.m_x + this.offset(this.m_kind, this.m_rot, i, 0)
            y = gy + this.offset(this.m_kind, this.m_rot, i, 1)
            if y >= 0:
                drawScreenRect(s, Config::BOARD_X + x * Config::CELL + 2, Config::BOARD_Y + y * Config::CELL + 2, Config::CELL - 4, Config::CELL - 4)

        for i in range(4):
            x = this.m_x + this.offset(this.m_kind, this.m_rot, i, 0)
            y = this.m_y + this.offset(this.m_kind, this.m_rot, i, 1)
            if y >= 0:
                this.drawBlock(Config::BOARD_X + x * Config::CELL, Config::BOARD_Y + y * Config::CELL, this.m_kind)

    func drawSidebar():
        s = this.m_screen
        sx = Config::SIDE_X

        setScreenColor(s, 230, 230, 240, 255)
        drawScreenText(s, "NEXT", sx, Config::BOARD_Y)
        drawScreenText(s, "Score: " + this.m_score, sx, 260)
        drawScreenText(s, "Lines: " + this.m_lines, sx, 290)
        drawScreenText(s, "Level: " + this.m_level, sx, 320)

        drawScreenText(s, "Left/Right  move", sx, 400)
        drawScreenText(s, "Up / X      rotate", sx, 425)
        drawScreenText(s, "Down        soft drop", sx, 450)
        drawScreenText(s, "Space       hard drop", sx, 475)

        ox = sx + 40
        oy = Config::BOARD_Y + 70
        for i in range(4):
            x = this.offset(this.m_next, 0, i, 0)
            y = this.offset(this.m_next, 0, i, 1)
            this.drawBlock(ox + x * Config::CELL, oy + y * Config::CELL, this.m_next)

    func draw():
        s = this.m_screen

        setScreenColor(s, 12, 12, 18, 255)
        clearScreen(s)

        this.drawBoard()
        if not this.m_gameOver:
            this.drawPiece()
        this.drawSidebar()

        if this.m_gameOver:
            setScreenColor(s, 255, 255, 255, 255)
            drawScreenText(s, "GAME OVER", Config::BOARD_X + 110, 300)
            drawScreenText(s, "Press ENTER to restart", Config::BOARD_X + 70, 330)


func main() -> int:
    screen = createScreen(Config::WIN_W, Config::WIN_H, "Tetris")
    game = init Tetris(screen)

    ticks_per_second = 60
    time_per_tick = 1000.0 / ticks_per_second

    last_tick_time: mut = Time::millis()

    while isScreenRunning(screen):
        pollScreenEvents(screen)

        game.update()
        game.draw()
        presentScreen(screen)

        last_tick_time = last_tick_time + time_per_tick

        current_time = Time::millis()
        sleep_time = last_tick_time - current_time

        if sleep_time > 0:
            Time::sleep(sleep_time)

    closeScreen(screen)
    return 0
