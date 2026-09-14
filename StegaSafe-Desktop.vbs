Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strPath = fso.GetParentFolderName(WScript.ScriptFullName)

strJava = "C:\Program Files\Java\jdk-26.0.1\bin\javaw.exe"
strJar = strPath & "\target\stegasafe-1.0.0.jar"

If Not fso.FileExists(strJar) Then
    MsgBox "stegasafe-1.0.0.jar not found! Please run build.bat first.", vbCritical, "StegaSafe Desktop"
    WScript.Quit 1
End If

strCmd = """" & strJava & """ --enable-native-access=ALL-UNNAMED -Dsun.java2d.uiScale=1.0 -Dloader.main=com.stegasafe.desktop.StegaSafeDesktopApp -cp """ & strJar & """ org.springframework.boot.loader.launch.PropertiesLauncher"

WshShell.Run strCmd, 0, False
