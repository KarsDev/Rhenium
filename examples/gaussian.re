using graphics
using clock.time
using math.math
using random.random

/*
 Gaussian viewer: 
 Draws samples from a normal distribution using random moudle into a live histogram, with the theoretical bell curve overlaid
*/
func main() -> int:
    width = 800
    height = 600

    screen = createScreen(width, height, "Gaussian Viewer")

    //  histogram layout
    bins = 60
    rangeLo = -6.0
    rangeHi = 6.0
    binValueWidth = (rangeHi - rangeLo) / 60.0
    binPx = 12
    plotX = 40
    plotW = 720
    baseY = 540
    plotH = 440

    samplesPerFrame = 25
    twoPi = 6.283185307179586
    sqrtTwoPi = 2.5066282746310002

    rng = init Random()

    counts = init List<int>(bins)
    for i in range(bins):
        counts.insert(i, 0)

    mu: mut = 0.0
    sigma: mut = 1.0
    total: mut = 0

    while isScreenRunning(screen):
        pollScreenEvents(screen)

        /*
        INPUT
        */
        changed: mut = false

        if isKeyPressed(Keys::KEY_LEFT):
            mu = mu - 0.03
            changed = true
        if isKeyPressed(Keys::KEY_RIGHT):
            mu = mu + 0.03
            changed = true
        if isKeyPressed(Keys::KEY_UP):
            sigma = sigma + 0.02
            changed = true
        if isKeyPressed(Keys::KEY_DOWN):
            sigma = sigma - 0.02
            changed = true
        if isKeyPressed(Keys::KEY_R):
            changed = true

        if sigma < 0.2:
            sigma = 0.2
        if sigma > 3.0:
            sigma = 3.0
        if mu < -4.0:
            mu = -4.0
        if mu > 4.0:
            mu = 4.0

        if changed:
            for i in range(bins):
                counts.insert(i, 0)
            total = 0

        /*
        SAMPLING
        */
        for s in range(samplesPerFrame):
            u1: mut = rng.nextDouble()
            if u1 < 0.000000000001:
                u1 = 0.000000000001
            u2 = rng.nextDouble()

            z = Math::sqrt(-2.0 * Math::ln(u1)) * Math::cos(twoPi * u2)
            value = mu + sigma * z

            if value >= rangeLo and value < rangeHi:
                b = cast<int>((value - rangeLo) / binValueWidth)
                if b >= 0 and b < bins:
                    counts.insert(b, counts.get(b) + 1)
                    total = total + 1

        /*
        SCALING
        */

        maxCount: mut = 1
        for i in range(bins):
            c = counts.get(i)
            if c > maxCount:
                maxCount = c

        // expected count in the peak bin for the theoretical curve
        expectedPeak = cast<double>(total) * binValueWidth / (sigma * sqrtTwoPi)
        top: mut = cast<double>(maxCount)
        if expectedPeak > top:
            top = expectedPeak
        top = top * 1.1
        if top < 10.0:
            top = 10.0
        scale = cast<double>(plotH) / top

        /*
        DRAWING
        */

        setScreenColor(screen, 15, 15, 25, 255)
        clearScreen(screen)

        // axis
        setScreenColor(screen, 90, 90, 110, 255)
        drawScreenLine(screen, plotX, baseY, plotX + plotW, baseY)

        // mean marker line
        meanPx = plotX + cast<int>((mu - rangeLo) / (rangeHi - rangeLo) * cast<double>(plotW))
        drawScreenLine(screen, meanPx, baseY, meanPx, baseY - plotH)

        // tick marks every 1.0
        for t in range(13):
            tx = plotX + t * 60
            drawScreenLine(screen, tx, baseY, tx, baseY + 6)

        // histogram bars
        setScreenColor(screen, 80, 160, 255, 255)
        for i in range(bins):
            h = cast<int>(cast<double>(counts.get(i)) * scale)
            if h > 0:
                fillScreenRect(screen, plotX + i * binPx, baseY - h, binPx - 1, h)

        // theoretical curve
        setScreenColor(screen, 255, 120, 80, 255)
        prevX: mut = plotX
        prevY: mut = baseY
        for k in range(181):
            px = k * 4
            x = rangeLo + cast<double>(px) / cast<double>(plotW) * (rangeHi - rangeLo)
            d = (x - mu) / sigma
            pdf = Math::exp(-0.5 * d * d) / (sigma * sqrtTwoPi)
            expected = cast<double>(total) * binValueWidth * pdf
            cy = baseY - cast<int>(expected * scale)
            cx = plotX + px
            if k > 0:
                drawScreenLine(screen, prevX, prevY, cx, cy)
            prevX = cx
            prevY = cy

        // text
        setScreenColor(screen, 230, 230, 240, 255)
        drawScreenText(screen, "samples: " + total, 20, 15)
        drawScreenText(screen, "mean: " + mu + "   sigma: " + sigma, 20, 35)
        drawScreenText(screen, "LEFT/RIGHT mean  |  UP/DOWN sigma  |  R reset  |  ESC quit", 20, 570)

        presentScreen(screen)

        if isKeyPressed(Keys::KEY_ESCAPE):
            closeScreen(screen)

    return 0
