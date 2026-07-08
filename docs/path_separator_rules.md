# Path Separator Rules

This project is developed on Windows, but it also edits and verifies ZIP/JAR
archives. Treat these as different path systems.

## Filesystem Paths

Use Windows-native filesystem APIs for real files and directories:

- Use `Join-Path`, `Resolve-Path`, and `-LiteralPath` in PowerShell.
- A real local path may use `\`, for example:
  `F:\minecraft\HMCL\.minecraft\versions\Supersymmetry\shaderpacks`.
- Do not pass archive entry names to `Test-Path`, `Join-Path`, or
  filesystem copy/move commands.

## ZIP And JAR Entry Names

ZIP/JAR paths are archive entry names, not filesystem paths.

- JAR entries should be treated as `/`-separated logical names.
- Shader ZIPs created on Windows may contain either `/` or `\` in
  `ZipArchiveEntry.FullName`.
- Never assume one separator when reading an existing ZIP. Enumerate entries or
  normalize entry names first.

PowerShell verification code must normalize before matching:

```powershell
$wanted = 'shaders/lib/materials/materialHandling/blockEntityIPBR.glsl'
$entry = $zip.Entries | Where-Object {
    ($_.FullName -replace '\\', '/') -eq $wanted
} | Select-Object -First 1
```

When writing new archive tools, prefer `/` for entry names. When using
`[System.IO.Compression.ZipFile]::CreateFromDirectory(...)`, immediately verify
the actual `FullName` values because .NET may preserve Windows separators.

For this repository, use `tools/Pack-Shaderpack.ps1` instead of
`ZipFile.CreateFromDirectory(...)`. The script writes `/`-separated entries and
fails if any `\` entry remains.

## Rule Of Thumb

- Real local file: use filesystem APIs and `\`.
- Archive entry: normalize `\` to `/` before comparison.
- Never mix the two in the same variable without naming it clearly, for example
  `$filePath` vs `$entryName`.
