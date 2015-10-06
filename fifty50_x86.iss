; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Fifty50 Racing"
#define MyAppVersion "1.0"
#define MyAppPublisher "P-Seminar Informatik: Samuel Hopstock, Andreas Stra�er, Simon Lehmair"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{51574032-5764-447E-99FA-9D3FF7C3CCDD}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputBaseFilename=setup
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"
Name: "catalan"; MessagesFile: "compiler:Languages\Catalan.isl"
Name: "corsican"; MessagesFile: "compiler:Languages\Corsican.isl"
Name: "czech"; MessagesFile: "compiler:Languages\Czech.isl"
Name: "danish"; MessagesFile: "compiler:Languages\Danish.isl"
Name: "dutch"; MessagesFile: "compiler:Languages\Dutch.isl"
Name: "finnish"; MessagesFile: "compiler:Languages\Finnish.isl"
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "greek"; MessagesFile: "compiler:Languages\Greek.isl"
Name: "hebrew"; MessagesFile: "compiler:Languages\Hebrew.isl"
Name: "hungarian"; MessagesFile: "compiler:Languages\Hungarian.isl"
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"
Name: "japanese"; MessagesFile: "compiler:Languages\Japanese.isl"
Name: "norwegian"; MessagesFile: "compiler:Languages\Norwegian.isl"
Name: "polish"; MessagesFile: "compiler:Languages\Polish.isl"
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"
Name: "scottishgaelic"; MessagesFile: "compiler:Languages\ScottishGaelic.isl"
Name: "serbiancyrillic"; MessagesFile: "compiler:Languages\SerbianCyrillic.isl"
Name: "serbianlatin"; MessagesFile: "compiler:Languages\SerbianLatin.isl"
Name: "slovenian"; MessagesFile: "compiler:Languages\Slovenian.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"
Name: "ukrainian"; MessagesFile: "compiler:Languages\Ukrainian.isl"

[Files]
Source: "Z:\home\samuel\IdeaProjects\Fifty50 Racing\out\artifacts\computer_jar_windows_x86\computer.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\countdown1.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\countdown2.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\countdown3.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\gameover_16-9.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\gameover_4-3.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\hand.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\hintergrund_16-9.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\hintergrund_4-3.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\null.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\ranking.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\start_fokussiert_1.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\start_fokussiert_2.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\start_fokussiert_3.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\start_fokussiert.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\start.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "Z:\home\samuel\fifty50\libs_32\*"; DestDir: "{app}"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{group}\Fifty50 Racing"; Filename: "{app}\start.bat"; Comment: "Fifty50 Racing by S. Hopstock, A. Stra�er, S. Lehmair"
Name: "{commondesktop}\Fifty50 Racing"; Filename: "{app}\start.bat"; Comment: "Fifty50 Racing by S. Hopstock, A. Stra�er, S. Lehmair"

[Dirs]
Name: "{app}\actionImgs"; Permissions: everyone-full
Name: "{app}"; Permissions: everyone-full

[Code]
function CreateBatch(): boolean;
var
fileName : string;
path : string;
begin
fileName := ExpandConstant('{app}\start.bat');
path := ExpandConstant('{app}');
SaveStringToFile(filename,'java -jar ' + '"' + path + 'computer.jar" 192.168.42.1 8888 http://192.168.42.1:8080/?action=stream ' + '"' + path + '"',false);
exit;
end;

procedure
CurStepChanged(CurStep:TSetupStep);
begin
if CurStep=ssPostInstall
then
begin
CreateBatch();
end
end;

procedure
CurUninstallStepChanged(CurUninstallStep:TUninstallStep);
begin
if CurUninstallStep=usPostUninstall
then
begin
DeleteFile(ExpandConstant('{app}\start.bat'));
DeleteFile(ExpandConstant('{commondesktop}\Fifty50 Racing.lnk'));
end
end;

[Run]
Filename: "{app}\start.bat"; Description: "Fifty50 Racing jetzt starten!"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{group}";