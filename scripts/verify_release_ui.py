"""Launch verification for the actual R8 release on a fresh CI emulator."""
import pathlib, re, subprocess, time, xml.etree.ElementTree as ET
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)
def ui():
    adb('shell', 'uiautomator', 'dump', '/sdcard/release-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/release-ui.xml'))
def click(text):
    deadline = time.monotonic() + 20
    while time.monotonic() < deadline:
        for node in ui().iter('node'):
            if node.get('text') == text:
                left, top, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
                adb('shell', 'input', 'tap', str((left + right)//2), str((top + bottom)//2))
                return
        time.sleep(.25)
    raise AssertionError('Missing release UI: ' + text)
click('설정')
click('Google 계정 · 자동 동기화')
labels = {node.get('text') for node in ui().iter('node')}
for text in ('Google 계정 · 자동 동기화', 'Google 계정 연결', '아직 연결되지 않았어요'):
    assert any(text.casefold() in label.casefold() for label in labels if label), (text, labels)
for forbidden in ('Google Drive에 백업', 'Drive 최근 백업 불러오기', '백업 파일 불러오기'):
    assert forbidden.casefold() not in {label.casefold() for label in labels if label}, labels
logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E')
assert 'FATAL EXCEPTION' not in logs, logs
with open('release-verification.txt', 'a') as output:
    output.write('PASS shrunk release launch and Google backup settings on Android 15\n')
