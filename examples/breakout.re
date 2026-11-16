using graphics
using graphics.keys
using clock.time
using collections.list

func main() -> int:
    width = 800
    height = 600

    screen = createScreen(width, height, "Breakout")

    paddleW = 75
    paddleH = 14
    paddleX: mut = 362
    paddleY = 555
    paddleSpeed = 7

    ballSize = 12
    ballX: mut = 394
    ballY: mut = 400
    ballDX: mut = 5
    ballDY: mut = -5
    ballSpeedLevel: mut = 0
    maxSpeedLevel = 3

    brickRows = 7
    brickCols = 10
    brickW = 70
    brickH = 18
    brickGap = 5
    brickStartX = 30
    brickStartY = 45
    brickCount = brickRows * brickCols

    bricks = init List<bool>(70)
    for i in range(brickCount):
        bricks.add(true)

    score: mut = 0
    alive: mut = true
    won: mut = false
    hit: mut = false
    remaining: mut = 0
    idx: mut = 0
    brickX: mut = 0
    brickY: mut = 0

    while isScreenRunning(screen):
        pollScreenEvents(screen)

        // restart after winning or losing
        if (not alive or won) and isKeyPressed(Keys::KEY_ENTER):
            paddleX = 362
            ballX = 394
            ballY = 400
            ballDX = 5
            ballDY = -5
            ballSpeedLevel = 0
            score = 0
            alive = true
            won = false

            for i in range(brickCount):
                bricks.set(i, true)

        if alive and not won:
            // paddle movement
            if isKeyPressed(Keys::KEY_LEFT) or isKeyPressed(Keys::KEY_A):
                paddleX -= paddleSpeed

            if isKeyPressed(Keys::KEY_RIGHT) or isKeyPressed(Keys::KEY_D):
                paddleX += paddleSpeed

            if paddleX < 0:
                paddleX = 0

            if paddleX + paddleW > width:
                paddleX = width - paddleW

            // mMove ball
            ballX += ballDX
            ballY += ballDY

            // left and right walls
            if ballX <= 0:
                ballX = 0
                ballDX = -ballDX

            if ballX + ballSize >= width:
                ballX = width - ballSize
                ballDX = -ballDX

            // top wall
            if ballY <= 0:
                ballY = 0
                ballDY = -ballDY

            // ball falls below the screen
            if ballY > height:
                alive = false

            // paddle collision
            if alive and ballDY > 0 and \
               ballX + ballSize > paddleX and \
               ballX < paddleX + paddleW and \
               ballY + ballSize >= paddleY and \
               ballY < paddleY + paddleH:

                ballY = paddleY - ballSize
                ballDY = -ballDY

                // stronger angle changes near the paddle edges
                if ballX + ballSize / 2 < paddleX + paddleW / 3:
                    ballDX = -6
                else if ballX + ballSize / 2 > paddleX + paddleW * 2 / 3:
                    ballDX = 6
                else:
                    if ballDX > 0:
                        ballDX = 4
                    else:
                        ballDX = -4

            // brick collision: remove at most one brick per frame
            hit = false

            for row in range(brickRows):
                for col in range(brickCols):
                    idx = row * brickCols + col

                    if bricks.get(idx) and not hit:
                        brickX = brickStartX + col * (brickW + brickGap)
                        brickY = brickStartY + row * (brickH + brickGap)

                        if ballX + ballSize > brickX and \
                           ballX < brickX + brickW and \
                           ballY + ballSize > brickY and \
                           ballY < brickY + brickH:

                            bricks.set(idx, false)
                            ballDY = -ballDY
                            score += 1
                            hit = true

                            // ball gets faster every 10 score
                            if score % 10 == 0 and ballSpeedLevel < maxSpeedLevel:
                                ballSpeedLevel++

                                if ballDX > 0:
                                    ballDX++
                                else:
                                    ballDX--

                                if ballDY > 0:
                                    ballDY++
                                else:
                                    ballDY--

            // check whether any bricks remain
            remaining = 0
            for i in range(brickCount):
                if bricks.get(i):
                    remaining++

            if remaining == 0:
                won = true

        // draw background
        setScreenColor(screen, 10, 10, 22, 255)
        clearScreen(screen)

        // draw bricks
        for row in range(brickRows):
            for col in range(brickCols):
                idx = row * brickCols + col

                if bricks.get(idx):
                    brickX = brickStartX + col * (brickW + brickGap)
                    brickY = brickStartY + row * (brickH + brickGap)

                    if row == 0:
                        setScreenColor(screen, 255, 55, 75, 255)
                    else if row == 1:
                        setScreenColor(screen, 255, 110, 55, 255)
                    else if row == 2:
                        setScreenColor(screen, 255, 175, 50, 255)
                    else if row == 3:
                        setScreenColor(screen, 220, 210, 55, 255)
                    else if row == 4:
                        setScreenColor(screen, 100, 205, 90, 255)
                    else if row == 5:
                        setScreenColor(screen, 65, 170, 230, 255)
                    else:
                        setScreenColor(screen, 155, 100, 230, 255)

                    fillScreenRect(screen, brickX, brickY, brickW, brickH)

        // draw paddle
        setScreenColor(screen, 80, 155, 255, 255)
        fillScreenRect(screen, paddleX, paddleY, paddleW, paddleH)

        // draw ball
        setScreenColor(screen, 255, 255, 255, 255)
        fillScreenRect(screen, ballX, ballY, ballSize, ballSize)

        // score display
        setScreenColor(screen, 255, 255, 255, 255)
        drawScreenText(screen, "SCORE: " + score, 10, 15)

        // speed indicator
        drawScreenText(screen, "SPEED", 680, 15)
        for i in range(ballSpeedLevel + 1):
            setScreenColor(screen, 255, 100 + i * 40, 60, 255)
            fillScreenRect(screen, 680 + i * 22, 35, 15, 8)

        if not alive:
            drawScreenText(screen, "GAME OVER - press Enter to restart", 235, 300)

        if won:
            drawScreenText(screen, "YOU WIN! - press Enter to play again", 235, 300)

        presentScreen(screen)

        if isKeyPressed(Keys::KEY_ESCAPE):
            closeScreen(screen)

        Time::sleep(16)

    return 0
