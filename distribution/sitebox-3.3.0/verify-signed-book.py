"""Drive the unmodified signed APK through physical UI; no instrumentation/debug hooks."""
import pathlib,re,subprocess,time,xml.etree.ElementTree as E
TITLE='ReleaseMeshCheck'
def adb(*args):return subprocess.check_output(['adb',*args],text=True)
def ui():
 adb('shell','rm','-f','/sdcard/solid-release-ui.xml');s=adb('shell','uiautomator','dump','/sdcard/solid-release-ui.xml')
 return E.fromstring(adb('shell','cat','/sdcard/solid-release-ui.xml')) if 'dumped to:' in s else E.Element('hierarchy')
def wait(predicate,message):
 end=time.monotonic()+60
 while time.monotonic()<end:
  root=ui();result=predicate(root)
  if result is not None and result is not False:return result
  time.sleep(.2)
 raise AssertionError(message)
def node(label):return wait(lambda root:next((n for n in root.iter('node') if n.get('text')==label or n.get('content-desc')==label),None),'Missing UI '+label)
def tap(n):
 a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',str((a+c)//2),str((b+d)//2))
def click(label):tap(node(label))
def field(index):
 fields=wait(lambda root: (nodes if len(nodes)==2 else None) if (nodes:=[n for n in root.iter('node') if n.get('class')=='android.widget.EditText']) else None,'Missing book dialog fields')
 return fields[index]
def book_visible():
 wait(lambda root:next((n for n in root.iter('node') if TITLE in n.get('content-desc','') and n.get('class')!='android.widget.EditText'),None),'Book not visible after native renderer/update')
def screenshot(name):
 p=pathlib.Path('release-device-qa')/name;p.parent.mkdir(exist_ok=True);p.write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
click('작아진 책장 열기');click('북마크 추가');tap(field(0));adb('shell','input','text',TITLE);tap(field(1));adb('shell','input','text','https://example.com/release-check');click('저장');book_visible();screenshot('release-solid-book.png')
with open('release-verification.txt','a') as out:out.write('PASS signed R8 release adds and renders an actual 3D book through physical UI\n')
adb('logcat','-c');adb('shell','am','force-stop','com.sitebox.app');adb('shell','am','start','-W','-n','com.sitebox.app/com.sitebox.app.MainActivity')
end=time.monotonic()+60
while time.monotonic()<end:
 logs=adb('logcat','-d','-s','SiteBox3D:I')
 assert 'Native room initialization failed' not in logs,logs
 if 'Loaded supplied room GLB:' in logs and 'room GPU frame presented' in logs:break
 time.sleep(.25)
else:raise AssertionError('Native signed renderer did not restart')
click('작아진 책장 열기');book_visible();screenshot('release-solid-book-restored.png')
assert 'FATAL EXCEPTION' not in adb('logcat','-d','-s','AndroidRuntime:E')
with open('release-verification.txt','a') as out:out.write('PASS signed R8 release restores its saved 3D book after process restart\n')
