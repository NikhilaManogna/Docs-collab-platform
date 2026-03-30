param(
    [string]$BaseUrl = "http://localhost:8080"
)

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$username = "alice$suffix"

$registerBody = @{
    username = $username
    email = "$username@example.com"
    password = "Password123!"
} | ConvertTo-Json

$auth = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registerBody
$headers = @{ Authorization = "Bearer $($auth.accessToken)" }

$docBody = @{
    title = "Realtime Design Doc"
    content = "Hello from the collaborative backend."
} | ConvertTo-Json

$document = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/documents" -Headers $headers -ContentType "application/json" -Body $docBody

Write-Host "Token acquired for user:" $auth.username
Write-Host "Created document:" $document.id
Write-Host "Fetch state endpoint:" "$BaseUrl/api/collaboration/documents/$($document.id)/state"
Write-Host ""
Write-Host "Use the following STOMP destinations in a WebSocket client:"
Write-Host "CONNECT header: Authorization: Bearer $($auth.accessToken)"
Write-Host "Endpoint: ws://localhost:8080/ws/collaboration"
Write-Host "Send join frame to: /app/documents/$($document.id)/join"
Write-Host "Send edit frame to: /app/documents/$($document.id)/operations"
Write-Host "Subscribe to: /topic/documents.$($document.id).operations"
Write-Host "Subscribe to: /topic/documents.$($document.id).presence"
Write-Host "Subscribe to user queue: /user/queue/documents.$($document.id).snapshot"
