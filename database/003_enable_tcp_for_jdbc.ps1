# Chạy PowerShell "Run as Administrator"
# Bật TCP/IP cho instance MSSQLSERVER (default) để Java JDBC kết nối được.

$ErrorActionPreference = 'Stop'
$tcpKey = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQLServer\SuperSocketNetLib\Tcp'
$ipAllKey = "$tcpKey\IPAll"

if (-not (Test-Path $tcpKey)) {
    Write-Error "Không tìm thấy registry cho MSSQLSERVER. Kiểm tra tên instance trong SSMS."
}

Set-ItemProperty -Path $tcpKey -Name Enabled -Value 1 -Type DWord

$port = (Get-ItemProperty $ipAllKey -ErrorAction SilentlyContinue).TcpPort
if (-not $port) {
    Set-ItemProperty -Path $ipAllKey -Name TcpPort -Value '1433' -Type String
    Write-Host "Đã đặt TcpPort=1433 tại IPAll."
}

Restart-Service MSSQLSERVER -Force
Start-Sleep -Seconds 3
Write-Host "Kiểm tra cổng 1433:"
netstat -ano | findstr ":1433"
