using graphics
using graphics.keys
using clock.time
using collections.list
using random.random


func main() -> int:
    width = 800
    height = 600

    cell = 20
    cols = 40
    rows = 30

    screen = createScreen(width, height, "Snake")

    bodyX = init List<int>(1200)
    bodyY = init List<int>(1200)
    for i in range(1200):
        bodyX.add(0)
        bodyY.add(0)

    snakeLen: mut = 3

    dirX: mut = 1
    dirY: mut = 0
    nextDX: mut = 1
    nextDY: mut = 0

    foodX: mut = 0
    foodY: mut = 0

    alive: mut = 1
    needReset: mut = 1
    needFood: mut = 1

    tick: mut = 0
    moveDelay: mut = 10

    rng = init Random()

    newX: mut = 0
    newY: mut = 0
    ate: mut = 0
    checkCount: mut = 0
    idx: mut = 0
    fx: mut = 0
    fy: mut = 0
    free: mut = 0
    found: mut = 0

    while isScreenRunning(screen):
        pollScreenEvents(screen)

        // start
        if needReset == 1:
            snakeLen = 3
            bodyX.set(0, 20)
            bodyY.set(0, 15)
            bodyX.set(1, 19)
            bodyY.set(1, 15)
            bodyX.set(2, 18)
            bodyY.set(2, 15)
            dirX = 1
            dirY = 0
            nextDX = 1
            nextDY = 0
            alive = 1
            tick = 0
            moveDelay = 10
            needFood = 1
            needReset = 0

        // place food on a random free cell
        if needFood == 1:
            found = 0
            for t in range(100):
                fx = rng.nextInt(0, cols - 1)
                fy = rng.nextInt(0, rows - 1)

                free = 1
                for i in range(snakeLen):
                    if bodyX.get(i) == fx and bodyY.get(i) == fy:
                        free = 0

                if free == 1 and found == 0:
                    foodX = fx
                    foodY = fy
                    found = 1
            needFood = 0

        // input
        if isKeyPressed(Keys::KEY_UP) or isKeyPressed(Keys::KEY_W):
            if dirY == 0:
                nextDX = 0
                nextDY = -1
        if isKeyPressed(Keys::KEY_DOWN) or isKeyPressed(Keys::KEY_S):
            if dirY == 0:
                nextDX = 0
                nextDY = 1
        if isKeyPressed(Keys::KEY_LEFT) or isKeyPressed(Keys::KEY_A):
            if dirX == 0:
                nextDX = -1
                nextDY = 0
        if isKeyPressed(Keys::KEY_RIGHT) or isKeyPressed(Keys::KEY_D):
            if dirX == 0:
                nextDX = 1
                nextDY = 0

        if alive == 1:
            tick++

            if tick >= moveDelay:
                tick = 0
                dirX = nextDX
                dirY = nextDY

                newX = bodyX.get(0) + dirX
                newY = bodyY.get(0) + dirY

                // walls
                if newX < 0 or newX >= cols or newY < 0 or newY >= rows:
                    alive = 0

                if alive == 1:
                    ate = 0
                    if newX == foodX and newY == foodY:
                        ate = 1

                    // self collision
                    checkCount = snakeLen - 1
                    if ate == 1:
                        checkCount = snakeLen

                    for i in range(checkCount):
                        if bodyX.get(i) == newX and bodyY.get(i) == newY:
                            alive = 0

                if alive == 1:
                    if ate == 1:
                        snakeLen++
                        needFood = 1
                        if moveDelay > 4:
                            moveDelay--

                    // shift every segment back one slot starting from the tail
                    for k in range(snakeLen - 1):
                        idx = snakeLen - 1 - k
                        bodyX.set(idx, bodyX.get(idx - 1))
                        bodyY.set(idx, bodyY.get(idx - 1))

                    bodyX.set(0, newX)
                    bodyY.set(0, newY)

        if alive == 0:
            if isKeyPressed(Keys::KEY_ENTER):
                needReset = 1


        setScreenColor(screen, 15, 15, 25, 255)
        clearScreen(screen)

        // food
        setScreenColor(screen, 230, 60, 60, 255)
        fillScreenRect(screen, foodX * cell + 3, foodY * cell + 3, cell - 6, cell - 6)

        // body
        setScreenColor(screen, 40, 170, 80, 255)
        for i in range(1, snakeLen):
            fillScreenRect(screen, bodyX.get(i) * cell + 1, bodyY.get(i) * cell + 1, cell - 2, cell - 2)

        // head
        setScreenColor(screen, 120, 255, 150, 255)
        if alive == 0:
            setScreenColor(screen, 255, 120, 60, 255)
        fillScreenRect(screen, bodyX.get(0) * cell + 1, bodyY.get(0) * cell + 1, cell - 2, cell - 2)

        // score
        setScreenColor(screen, 255, 255, 255, 255)
        for i in range(snakeLen - 3):
            fillScreenRect(screen, 8 + i * 10, 6, 6, 6)

        if alive == 0:
            drawScreenText(screen, "GAME OVER - press Enter to restart", 270, 290)

        presentScreen(screen)

        if isKeyPressed(Keys::KEY_ESCAPE):
            closeScreen(screen)

        Time::sleep(16)

    return 0