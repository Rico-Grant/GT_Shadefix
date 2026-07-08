param(
    [Parameter(Mandatory = $true)]
    [string] $SourceDir,

    [Parameter(Mandatory = $true)]
    [string] $OutputZip
)

$source = Resolve-Path -LiteralPath $SourceDir
$outputParent = Split-Path -Parent $OutputZip
if ($outputParent) {
    New-Item -ItemType Directory -Force -Path $outputParent | Out-Null
}

if (Test-Path -LiteralPath $OutputZip) {
    Remove-Item -LiteralPath $OutputZip -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$zip = [System.IO.Compression.ZipFile]::Open($OutputZip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $source.Path -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($source.Path.Length).TrimStart('\', '/')
        $entryName = $relative -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip,
            $_.FullName,
            $entryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
    }
} finally {
    $zip.Dispose()
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($OutputZip)
try {
    $backslashEntries = @($zip.Entries | Where-Object { $_.FullName.Contains('\') })
    if ($backslashEntries.Count -gt 0) {
        throw "Shaderpack contains ZIP entries with Windows backslashes: $($backslashEntries[0].FullName)"
    }

    if (-not ($zip.Entries | Where-Object { $_.FullName -eq 'shaders/gbuffers_block.fsh' } | Select-Object -First 1)) {
        throw "Shaderpack is missing shaders/gbuffers_block.fsh"
    }
} finally {
    $zip.Dispose()
}

Write-Host "Packed shaderpack with forward-slash entries: $OutputZip"
