param(
    [string]$BaseUrl = "http://127.0.0.1:8000"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# All values are synthetic. No patient identity, source report, or API key is read here.
$payload = @{
    taskId = "SYNTHETIC_DEEPSEEK_TEST"
    patientId = "synthetic"
    patientContext = @{
        gender = "FEMALE"
        age = 38
    }
    modelCodes = @(
        "GLUCOSE_METABOLISM"
        "LIPID_CARDIOVASCULAR"
    )
    indicators = @(
        @{ code = "fasting_glucose"; name = "Fasting glucose"; value = 6.4; unit = "mmol/L"; referenceLow = 3.9; referenceHigh = 6.1 }
        @{ code = "hba1c"; name = "HbA1c"; value = 5.8; unit = "%"; referenceLow = 4.0; referenceHigh = 6.0 }
        @{ code = "triglyceride"; name = "Triglyceride"; value = 1.9; unit = "mmol/L"; referenceLow = 0; referenceHigh = 1.7 }
        @{ code = "hdl"; name = "HDL"; value = 1.2; unit = "mmol/L"; referenceLow = 1.0; referenceHigh = $null }
        @{ code = "total_cholesterol"; name = "Total cholesterol"; value = 5.4; unit = "mmol/L"; referenceLow = 0; referenceHigh = 5.2 }
        @{ code = "ldl"; name = "LDL"; value = 3.6; unit = "mmol/L"; referenceLow = 0; referenceHigh = 3.4 }
    )
} | ConvertTo-Json -Depth 8

$webResponse = Invoke-WebRequest -UseBasicParsing `
    -Method Post `
    -Uri "$($BaseUrl.TrimEnd('/'))/api/v1/assessments/evaluate" `
    -ContentType "application/json; charset=utf-8" `
    -Body $payload `
    -TimeoutSec 45

$webResponse.RawContentStream.Position = 0
$reader = New-Object System.IO.StreamReader(
    $webResponse.RawContentStream,
    [System.Text.Encoding]::UTF8
)
$response = $reader.ReadToEnd() | ConvertFrom-Json
$reader.Dispose()

if ($response.code -ne 0) {
    throw "Assessment API failed: $($response.message)"
}

$interpretation = $response.data.interpretation

Write-Host "`nRule results (two of twelve models requested):" -ForegroundColor Cyan
$response.data.results | Select-Object modelCode, status, score, riskLevel, dataCompleteness, confidence | Format-Table -AutoSize

Write-Host "DeepSeek result:" -ForegroundColor Cyan
[pscustomobject]@{
    status = $interpretation.status
    source = $interpretation.source
    model = $interpretation.model
    summary = $interpretation.summary
    uncertainty = $interpretation.uncertainty
} | Format-List

if ($interpretation.status -ne "SUCCESS" -or $interpretation.source -ne "DEEPSEEK") {
    throw "DeepSeek is not active. Current source: $($interpretation.source). Check container environment and logs."
}

Write-Host "DeepSeek V4 Flash live connectivity test passed." -ForegroundColor Green
