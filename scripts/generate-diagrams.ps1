Add-Type -AssemblyName System.Drawing

$outputDir = "C:\Users\nikhi\OneDrive\Documents\New project\docs-collab-backend\docs\images"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function New-Canvas {
    param([int]$Width = 1600, [int]$Height = 900)

    $bitmap = New-Object System.Drawing.Bitmap $Width, $Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $background = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(246, 248, 252))
    $graphics.FillRectangle($background, 0, 0, $Width, $Height)
    return @{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function New-Font {
    param([float]$Size, [bool]$Bold = $false)
    if ($Bold) {
        return New-Object System.Drawing.Font("Segoe UI", $Size, [System.Drawing.FontStyle]::Bold)
    }
    return New-Object System.Drawing.Font("Segoe UI", $Size, [System.Drawing.FontStyle]::Regular)
}

function Draw-Box {
    param(
        $Graphics,
        [string]$Title,
        [string]$Subtitle,
        [int]$X,
        [int]$Y,
        [int]$W,
        [int]$H,
        [string]$Fill = "#EAF4FF",
        [string]$Border = "#2B5C88"
    )

    $fillColor = [System.Drawing.ColorTranslator]::FromHtml($Fill)
    $borderColor = [System.Drawing.ColorTranslator]::FromHtml($Border)
    $brush = New-Object System.Drawing.SolidBrush $fillColor
    $pen = New-Object System.Drawing.Pen $borderColor, 3
    $rect = New-Object System.Drawing.Rectangle $X, $Y, $W, $H
    $Graphics.FillRectangle($brush, $rect)
    $Graphics.DrawRectangle($pen, $rect)

    $titleFont = New-Font -Size 22 -Bold $true
    $subFont = New-Font -Size 14
    $textBrush = [System.Drawing.Brushes]::Black
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center

    $titleRect = New-Object System.Drawing.RectangleF ($X + 14), ($Y + 18), ($W - 28), 38
    $subRect = New-Object System.Drawing.RectangleF ($X + 16), ($Y + 58), ($W - 32), ($H - 70)

    $Graphics.DrawString($Title, $titleFont, $textBrush, $titleRect, $format)
    $Graphics.DrawString($Subtitle, $subFont, $textBrush, $subRect, $format)
}

function Draw-Arrow {
    param(
        $Graphics,
        [int]$X1,
        [int]$Y1,
        [int]$X2,
        [int]$Y2,
        [string]$Label = ""
    )

    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(53, 79, 104)), 4
    $pen.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap 7, 9
    $Graphics.DrawLine($pen, $X1, $Y1, $X2, $Y2)

    if ($Label) {
        $font = New-Font -Size 13 -Bold $true
        $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(53, 79, 104))
        $midX = [int](($X1 + $X2) / 2)
        $midY = [int](($Y1 + $Y2) / 2) - 22
        $Graphics.DrawString($Label, $font, $brush, $midX - 55, $midY)
    }
}

function Save-Canvas {
    param($Canvas, [string]$Path)
    $Canvas.Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Canvas.Graphics.Dispose()
    $Canvas.Bitmap.Dispose()
}

function Draw-Title {
    param($Graphics, [string]$Title, [string]$Subtitle)
    $titleFont = New-Font -Size 30 -Bold $true
    $subFont = New-Font -Size 15
    $Graphics.DrawString($Title, $titleFont, [System.Drawing.Brushes]::Black, 50, 28)
    $Graphics.DrawString($Subtitle, $subFont, [System.Drawing.Brushes]::DimGray, 52, 76)
}

# Architecture diagram
$canvas = New-Canvas
$g = $canvas.Graphics
Draw-Title $g "Collaborative Docs Platform Architecture" "Client, API routing, collaboration service, Redis fanout, and PostgreSQL persistence"
Draw-Box $g "Browser Client" "React + Vite UI`nLogin, sharing, editing, rollback" 70 290 220 130 "#FDECC8" "#B8801F"
Draw-Box $g "Gateway Service" "REST entry point`nRoutes auth and document APIs" 350 290 250 130 "#E7F0FE" "#2B5C88"
Draw-Box $g "Collaboration Service" "STOMP/WebSocket`nPresence + realtime sync" 650 120 280 130 "#E4F7EE" "#2D7D5A"
Draw-Box $g "Auth Service" "JWT auth`nRegistration + password reset" 670 300 240 120 "#FBE7F0" "#9A3E6B"
Draw-Box $g "Document Service" "CRUD, memberships, versions`nRollback + persistent state" 670 490 240 120 "#E7F7FB" "#1F728C"
Draw-Box $g "Redis" "Pub/Sub fanout`nShared collaboration state" 1020 140 210 120 "#F3E8FF" "#6E4AA5"
Draw-Box $g "PostgreSQL" "Users, documents, memberships`nversions, reset tokens" 1035 410 230 140 "#F2F2F2" "#505050"
Draw-Arrow $g 290 355 350 355 "REST"
Draw-Arrow $g 290 330 650 180 "WebSocket"
Draw-Arrow $g 600 335 670 335 ""
Draw-Arrow $g 600 420 670 540 ""
Draw-Arrow $g 930 180 1020 180 "pub/sub"
Draw-Arrow $g 930 540 1035 480 ""
Draw-Arrow $g 910 360 1035 450 ""
Draw-Arrow $g 930 240 930 490 "persist sync"
Save-Canvas $canvas (Join-Path $outputDir "architecture-overview.png")

# Request flow diagram
$canvas = New-Canvas
$g = $canvas.Graphics
Draw-Title $g "Realtime Collaboration Request Flow" "From authentication to live edits and durable persistence"
$actors = @(
    @{ Name = "User"; X = 110 },
    @{ Name = "Frontend"; X = 320 },
    @{ Name = "Gateway"; X = 530 },
    @{ Name = "Auth"; X = 740 },
    @{ Name = "Document"; X = 950 },
    @{ Name = "Collaboration"; X = 1160 },
    @{ Name = "Redis"; X = 1390 }
)
$headerFont = New-Font -Size 18 -Bold $true
foreach ($actor in $actors) {
    Draw-Box $g $actor.Name "" ($actor.X - 70) 110 140 56 "#EAF0F7" "#4A607A"
    $g.DrawLine((New-Object System.Drawing.Pen ([System.Drawing.Color]::LightGray), 2), $actor.X, 166, $actor.X, 820)
}

$steps = @(
    @{ From = 110; To = 320; Y = 220; Text = "Sign in / Register" },
    @{ From = 320; To = 530; Y = 270; Text = "POST /api/auth/*" },
    @{ From = 530; To = 740; Y = 320; Text = "Forward auth request" },
    @{ From = 740; To = 320; Y = 370; Text = "JWT token response" },
    @{ From = 320; To = 950; Y = 450; Text = "Load document metadata" },
    @{ From = 320; To = 1160; Y = 520; Text = "Join WebSocket session" },
    @{ From = 1160; To = 950; Y = 580; Text = "Resolve role + latest state" },
    @{ From = 1160; To = 1390; Y = 650; Text = "Publish collaboration event" },
    @{ From = 1160; To = 950; Y = 710; Text = "Persist synced snapshot" },
    @{ From = 1160; To = 320; Y = 770; Text = "Broadcast updated content" }
)
foreach ($step in $steps) {
    Draw-Arrow $g $step.From $step.Y $step.To $step.Y $step.Text
}
Save-Canvas $canvas (Join-Path $outputDir "request-flow.png")

# Service boundaries diagram
$canvas = New-Canvas
$g = $canvas.Graphics
Draw-Title $g "Service Boundaries And Responsibilities" "Separation of concerns across platform modules"
Draw-Box $g "Gateway Service" "Single REST entry point`nRoutes downstream APIs" 110 180 260 120 "#E7F0FE" "#2B5C88"
Draw-Box $g "Auth Service" "User accounts`nJWT tokens`nPassword reset" 460 120 230 150 "#FBE7F0" "#9A3E6B"
Draw-Box $g "Document Service" "CRUD`nSharing`nVersion history`nRollback" 460 330 230 170 "#E7F7FB" "#1F728C"
Draw-Box $g "Collaboration Service" "Realtime sync`nPresence`nLive updates" 460 570 230 150 "#E4F7EE" "#2D7D5A"
Draw-Box $g "PostgreSQL" "Auth schema`nDocument schema" 840 230 210 130 "#F2F2F2" "#505050"
Draw-Box $g "Redis" "Pub/Sub`nCross-instance sync" 840 560 210 120 "#F3E8FF" "#6E4AA5"
Draw-Box $g "Frontend App" "Login`nEditor`nSharing UI`nVersion view" 110 560 260 150 "#FDECC8" "#B8801F"
Draw-Arrow $g 370 240 460 190 "route"
Draw-Arrow $g 370 310 460 400 "route"
Draw-Arrow $g 370 620 460 640 "socket"
Draw-Arrow $g 690 200 840 270 "store"
Draw-Arrow $g 690 410 840 310 "store"
Draw-Arrow $g 690 645 840 620 "fanout"
Draw-Arrow $g 690 620 840 320 "sync"
Save-Canvas $canvas (Join-Path $outputDir "service-boundaries.png")
