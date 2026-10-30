param([switch]$SkipBuild,[string]$LibrariesPath='',[string]$JdkBin='')
$ErrorActionPreference='Stop'
if(!$JdkBin){$JdkBin=Split-Path (Get-Command javac.exe -ErrorAction Stop).Source}
if(!$SkipBuild){& "$PSScriptRoot/build.ps1" -LibrariesPath $LibrariesPath -JdkBin $JdkBin}
$taskArgs=Get-Content "$PSScriptRoot/compile.args"
$taskCp=$taskArgs[[Array]::IndexOf($taskArgs,'-cp')+1].Trim('"')
# Paperclip extracts additional Minecraft runtime libraries on its first boot.
# Refresh the test runtime classpath even when reusing an earlier compile file.
$taskApiPath=$taskCp.Split(';')[0]
$taskApiMarker='/io/papermc/paper/paper-api/'
$taskMarkerIndex=$taskApiPath.IndexOf($taskApiMarker)
if($taskMarkerIndex -ge 0){
 $taskRuntimeLibs=Get-ChildItem -LiteralPath $taskApiPath.Substring(0,$taskMarkerIndex) -Recurse -Filter '*.jar' | Where-Object {$_.FullName -notmatch '\\paper-api\\'} | Select-Object -ExpandProperty FullName
 $taskCp=(@($taskApiPath)+@($taskRuntimeLibs | ForEach-Object {$_.Replace('\','/')})) -join ';'
}
New-Item -ItemType Directory -Force "$PSScriptRoot/test-classes" | Out-Null
$taskTestArgs=@($taskArgs[0..8]);$taskTestArgs[8]='"'+"$PSScriptRoot/test-classes".Replace('\','/')+'"'
$taskTestArgs+=@('src/RtpRules.java','TestRtpRules.java','TestRtpConfig.java') | ForEach-Object {'"'+"$PSScriptRoot/$_".Replace('\','/')+'"'}
[IO.File]::WriteAllLines("$PSScriptRoot/test-compile.args",$taskTestArgs,[Text.UTF8Encoding]::new($false))
& "$JdkBin/javac.exe" -J-Xmx128m "@$PSScriptRoot/test-compile.args"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
foreach($taskTest in @('TestRtpRules','TestRtpConfig')){
 $taskRunArgs=@('--class-path',('"'+"$PSScriptRoot/test-classes;".Replace('\','/')+$taskCp+'"'),$taskTest)
 if($taskTest -eq 'TestRtpConfig'){$taskRunArgs+=('"'+"$PSScriptRoot/classes/config.yml".Replace('\','/')+'"')}
 [IO.File]::WriteAllLines("$PSScriptRoot/test-run.args",$taskRunArgs,[Text.UTF8Encoding]::new($false))
 & "$JdkBin/java.exe" "@$PSScriptRoot/test-run.args"
 if($LASTEXITCODE -ne 0){throw "$taskTest failed"}
}
