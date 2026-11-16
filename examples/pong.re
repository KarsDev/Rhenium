using graphics
using graphics.keys
using clock.time

func main() -> int:
    width = 800
    height = 600

    screen = createScreen(width, height, "Pong")

    paddleW = 12
    paddleH = 90
    paddleHalf = 45
    ballSize = 12
    ballHalf = 6

    leftX = 30
    rightX = 758

    aiSpeed = 4

    leftY: mut = 255
    rightY: mut = 255

    ballX: mut = 394
    ballY: mut = 294
    ballVX: mut = 5
    ballVY: mut = 3

    leftScore: mut = 0
    rightScore: mut = 0

    hit: mut = 0

    while isScreenRunning(screen):
        pollScreenEvents(screen)

        if isKeyPressed(Keys::KEY_W):
            leftY = leftY - 6
        if isKeyPressed(Keys::KEY_S):
            leftY = leftY + 6

        if leftY < 0:
            leftY = 0
        if leftY > height - paddleH:
            leftY = height - paddleH

        aiCenter = rightY + paddleHalf
        ballCenter = ballY + ballHalf

        if ballVX > 0:
            if ballX > 350:
                if ballCenter < aiCenter - 10:
                    rightY = rightY - aiSpeed
                if ballCenter > aiCenter + 10:
                    rightY = rightY + aiSpeed

        if rightY < 0:
            rightY = 0
        if rightY > height - paddleH:
            rightY = height - paddleH

        ballX = ballX + ballVX
        ballY = ballY + ballVY

        if ballY < 0:
            ballY = 0
            ballVY = -ballVY
        if ballY > height - ballSize:
            ballY = height - ballSize
            ballVY = -ballVY

        if ballVX < 0 and \
          ballX < leftX + paddleW and \
          ballX > leftX - ballSize and \
          ballY + ballSize > leftY and \
          ballY < leftY + paddleH:

            ballVX = -ballVX
            ballX = leftX + paddleW

            hit = ballY + ballHalf - leftY
            if hit < 30:
                ballVY = ballVY - 2
            if hit > 60:
                ballVY = ballVY + 2

            if ballVX < 9:
                ballVX = ballVX + 1

        if ballVX > 0 and \
          ballX + ballSize > rightX and \
          ballX < rightX + paddleW and \
          ballY + ballSize > rightY and \
          ballY < rightY + paddleH:
            ballVX = -ballVX
            ballX = rightX - ballSize

            hit = ballY + ballHalf - rightY
            if hit < 30:
                ballVY = ballVY - 2
            if hit > 60:
                ballVY = ballVY + 2

            if ballVX > -9:
                ballVX = ballVX - 1

        if ballVY > 8:
            ballVY = 8
        if ballVY < -8:
            ballVY = -8

        if ballX < 0:
            rightScore = rightScore + 1
            ballX = 394
            ballY = 294
            ballVX = 5
            ballVY = 3
        if ballX > width:
            leftScore = leftScore + 1
            ballX = 394
            ballY = 294
            ballVX = -5
            ballVY = 3

        if leftScore >= 7:
            leftScore = 0
            rightScore = 0
        if rightScore >= 7:
            leftScore = 0
            rightScore = 0

        setScreenColor(screen, 15, 15, 25, 255)
        clearScreen(screen)

        setScreenColor(screen, 70, 70, 90, 255)
        for i in range(15):
            fillScreenRect(screen, 397, i * 40 + 10, 6, 20)

        setScreenColor(screen, 255, 255, 255, 255)
        fillScreenRect(screen, leftX, leftY, paddleW, paddleH)
        fillScreenRect(screen, rightX, rightY, paddleW, paddleH)
        fillScreenRect(screen, ballX, ballY, ballSize, ballSize)

        for i in range(leftScore):
            fillScreenRect(screen, 340 - i * 20, 20, 12, 12)
        for i in range(rightScore):
            fillScreenRect(screen, 448 + i * 20, 20, 12, 12)

        presentScreen(screen)

        if isKeyPressed(Keys::KEY_ESCAPE):
            closeScreen(screen)

        Time::sleep(5)

    return 0
