param(
    [ValidateSet("all", "diagnostic")]
    [string] $Target = "all",
    [string] $InstanceRoot = "F:\minecraft\HMCL\.minecraft\versions\Supersymmetry"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildRoot = Join-Path $ProjectRoot "build"
$LibOut = Join-Path $BuildRoot "libs"
$MainJava = Join-Path $ProjectRoot "src\main\java"
$CompileStubsJava = Join-Path $ProjectRoot "src\compileStubs\java"
$Resources = Join-Path $ProjectRoot "src\main\resources"
$MinecraftRoot = Split-Path -Parent (Split-Path -Parent $InstanceRoot)

New-Item -ItemType Directory -Force -Path $BuildRoot, $LibOut | Out-Null

function Get-Javac {
    $cmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    throw "javac was not found on PATH. Install a JDK or add javac.exe to PATH."
}

function Get-CompileClasspath {
    $paths = New-Object System.Collections.Generic.List[string]

    $mcpMinecraft = "D:\MinecraftModding\Cleanroom\build\rfg\recompiled_minecraft-1.12.2.jar"
    if (Test-Path -LiteralPath $mcpMinecraft) {
        $paths.Add($mcpMinecraft)
    } else {
        $versionJar = Join-Path $InstanceRoot "Supersymmetry.jar"
        if (Test-Path -LiteralPath $versionJar) {
            $paths.Add($versionJar)
        }
    }

    foreach ($library in @(
        "org\apache\logging\log4j\log4j-api\2.8.1\log4j-api-2.8.1.jar",
        "org\apache\logging\log4j\log4j-core\2.8.1\log4j-core-2.8.1.jar",
        "org\spongepowered\mixin\0.8.5\mixin-0.8.5.jar",
        "com\cleanroommc\sponge-mixin\0.20.12+mixin.0.8.7\sponge-mixin-0.20.12+mixin.0.8.7.jar"
    )) {
        $path = Join-Path (Join-Path $MinecraftRoot "libraries") $library
        if (Test-Path -LiteralPath $path) {
            $paths.Add($path)
        }
    }

    $mods = Join-Path $InstanceRoot "mods"
    foreach ($name in @(
        "!mixinbooter-10.7.jar",
        "CodeChickenLib-1.12.2-3.2.3.358-universal.jar",
        "gregtech-1.12.2-2.8.10-beta.jar",
        "preview_OptiFine_1.12.2_HD_U_G6_pre1.jar"
    )) {
        $path = Join-Path $mods $name
        if (Test-Path -LiteralPath $path) {
            $paths.Add($path)
        }
    }

    return (($paths | Select-Object -Unique | ForEach-Object { $_.Replace("\", "/") }) -join [IO.Path]::PathSeparator)
}

function Convert-ToJavacArgPath([string] $Path) {
    return $Path.Replace("\", "/")
}

function Copy-DirectoryContents([string] $Source, [string] $Destination) {
    if (Test-Path -LiteralPath $Source) {
        Get-ChildItem -LiteralPath $Source -Recurse | ForEach-Object {
            $relative = $_.FullName.Substring($Source.Length).TrimStart("\", "/")
            $target = Join-Path $Destination $relative
            if ($_.PSIsContainer) {
                New-Item -ItemType Directory -Force -Path $target | Out-Null
            } else {
                New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
                Copy-Item -LiteralPath $_.FullName -Destination $target -Force
            }
        }
    }
}

function New-ZipFromDirectory([string] $Source, [string] $ZipPath) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Remove-Item -LiteralPath $ZipPath -Force -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem -LiteralPath $Source -File -Recurse | ForEach-Object {
            $relative = $_.FullName.Substring($Source.Length).TrimStart("\", "/").Replace("\", "/")
            $entry = $zip.CreateEntry($relative)
            $in = [IO.File]::OpenRead($_.FullName)
            try {
                $out = $entry.Open()
                try {
                    $in.CopyTo($out)
                } finally {
                    $out.Dispose()
                }
            } finally {
                $in.Dispose()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

function Invoke-Build {
    $javac = Get-Javac
    $classpath = Get-CompileClasspath
    $classes = Join-Path $BuildRoot "classes\diagnostic"
    $staging = Join-Path $BuildRoot "staging\diagnostic"
    $sourcesFile = Join-Path $BuildRoot "sources-diagnostic.txt"
    $argsFile = Join-Path $BuildRoot "javac-diagnostic.args"

    Remove-Item -LiteralPath $classes, $staging -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $classes, $staging | Out-Null

    $sources = @(Get-ChildItem -LiteralPath $MainJava -Filter "*.java" -Recurse)
    if (Test-Path -LiteralPath $CompileStubsJava) {
        $sources += Get-ChildItem -LiteralPath $CompileStubsJava -Filter "*.java" -Recurse
    }
    $sourceArgs = $sources | ForEach-Object { '"' + (Convert-ToJavacArgPath $_.FullName) + '"' }
    $javacArgs = @(
        "-encoding", "UTF-8",
        "-source", "8",
        "-target", "8",
        "-Xlint:none",
        "-proc:none",
        "-cp", ('"' + $classpath + '"'),
        "-d", ('"' + (Convert-ToJavacArgPath $classes) + '"')
    ) + $sourceArgs
    $javacArgs | Set-Content -Encoding ASCII -LiteralPath $argsFile

    & $javac "@$argsFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for $Name"
    }

    Remove-Item -LiteralPath (Join-Path $classes "net\minecraftforge") -Recurse -Force -ErrorAction SilentlyContinue

    Copy-DirectoryContents -Source $Resources -Destination $staging
    Copy-DirectoryContents -Source $classes -Destination $staging

    $jarPath = Join-Path $LibOut "gtceu-optifine-shader-bridge-semantic-debug.jar"
    New-ZipFromDirectory -Source $staging -ZipPath $jarPath
    Write-Host "Built $jarPath"
}

if ($Target -eq "all" -or $Target -eq "diagnostic") {
    Invoke-Build
}
