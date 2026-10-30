param([string]$LibrariesPath='', [string]$JdkBin='')
$ErrorActionPreference='Stop'
if(!$LibrariesPath){$LibrariesPath=Join-Path (Split-Path (Split-Path $PSScriptRoot)) 'test-servers/RandomTeleportCompatibility/1.21/libraries'}
if(!(Test-Path -LiteralPath $LibrariesPath)){throw 'Supply -LibrariesPath with the Paper 1.21 dependency libraries folder.'}
if(!$JdkBin){$JdkBin=Split-Path (Get-Command javac.exe -ErrorAction Stop).Source}
$libs=Get-ChildItem -LiteralPath $LibrariesPath -Recurse -Filter '*.jar' | Select-Object -ExpandProperty FullName
$api=$libs | Where-Object {$_ -match '\\paper-api\\1\.21-R0\.1-SNAPSHOT\\'} | Select-Object -First 1
if(!$api){throw 'Paper 1.21 API was not found. Build against the minimum supported API, not a newer Paper API.'}
$cp=(@($api)+@($libs | Where-Object {$_ -notmatch '\\paper-api\\'})) -join ';'
New-Item -ItemType Directory -Force "$PSScriptRoot/classes","$PSScriptRoot/dist" | Out-Null
$compileArgs=@('-proc:none','-encoding','UTF-8','--release','21','-cp',('"'+$cp.Replace('\','/')+'"'),'-d',('"'+"$PSScriptRoot/classes".Replace('\','/')+'"'))
$compileArgs+=Get-ChildItem "$PSScriptRoot/src/*.java" | ForEach-Object {'"'+$_.FullName.Replace('\','/')+'"'}
[IO.File]::WriteAllLines("$PSScriptRoot/compile.args",$compileArgs,[Text.UTF8Encoding]::new($false))
& "$JdkBin/javac.exe" -J-Xmx128m "@$PSScriptRoot/compile.args"
if($LASTEXITCODE -ne 0){throw 'Compile failed'}
& "$JdkBin/jar.exe" -J-Xmx32m --create --file "$PSScriptRoot/dist/RandomTeleportPlugin-1.1.0.jar" -C "$PSScriptRoot/classes" .
if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
Write-Output "Built: $PSScriptRoot/dist/RandomTeleportPlugin-1.1.0.jar"

